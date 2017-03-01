package it.beng.modeler.microservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import it.beng.modeler.config;
import it.beng.modeler.microservice.auth.local.LocalAuthProvider;
import it.beng.modeler.microservice.auth.local.impl.LocalUser;
import it.beng.modeler.model.basic.Entity;
import it.beng.modeler.model.basic.Typed;
import it.beng.modeler.model.diagram.Diagram;
import it.beng.modeler.model.diagram.DiagramElement;
import it.beng.modeler.model.semantic.AcceptMatrix;
import it.beng.modeler.model.semantic.SemanticElement;
import it.beng.modeler.model.semantic.organization.roles.AuthenticationRole;
import it.beng.modeler.model.semantic.organization.roles.AuthorizationRole;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class RestAPIVerticle extends AbstractVerticle {

    private boolean develop;

    static class Error extends RuntimeException {

        public static JsonObject fromRoutingContext(RoutingContext rc, Throwable t) {
            if (t == null) return fromRoutingContext(rc, new IllegalStateException("Unknown Error"));
            int statusCode = rc.statusCode() >= 0 ? rc.statusCode() : 500;
            return new JsonObject()
                .put("timestamp", System.nanoTime())
                .put("remote", rc.request().remoteAddress().host() + ":" + rc.request().remoteAddress().port())
                .put("request", "[" + rc.request().method() + "] " + rc.request().uri())
                .put("statusCode", statusCode)
                .put("error", HttpResponseStatus.valueOf(statusCode).reasonPhrase())
                .put("message", t.getMessage());
        }

        public Error(RoutingContext rc, Throwable t) {
            rc.put("error", fromRoutingContext(rc, t));
        }

        public Error(RoutingContext rc, String message) {
            rc.put("error", fromRoutingContext(rc, new IllegalStateException(message)));
        }

    }

    static {
        Typed.init();
        Diagram.init();
        SemanticElement.init();
        Json.mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        Json.mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        Json.prettyMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        Json.prettyMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    }

    // TODO: this is just for simulating a remote call lagtime. Delete it when done.

    private void simLagTime(Long simLagTime) {
        if (!config.developMode) return;
        if (simLagTime == null)
            simLagTime = config.server.simLagTime;
        if (simLagTime > 0) try {
            long ms = (long) (Math.max(0, simLagTime * (1 + new Random().nextGaussian() / 3)));
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void simLagTime() {
        simLagTime(config.server.simLagTime);
    }

    private void redirect(HttpServerResponse response, String location) {
        response
            .setStatusCode(301)
            .putHeader("Location", location)
            .end();
    }

    private boolean isAuthenticated(RoutingContext rc) {
        return rc.user() != null;
    }

    private static String getQueryParameter(String query, String paramName) {
        if (query != null && paramName != null)
            for (String s : query.split("&")) {
                String[] entry = s.split("=");
                if (paramName.equals(entry[0])) {
                    return entry[1];
                }
            }
        return null;
    }

    @Override
    public void start() {

        // create the secure http client
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions()
            .setSsl(true)
            .setDefaultHost("localhost")
            .setDefaultPort(config.server.port)
            .setKeyStoreOptions(new JksOptions()
                .setPath(config.keystore.filename)
                .setPassword(config.keystore.password)
            )
        );

        // Create a router object.
        Router router = Router.router(vertx);

        // configure CORS origins and allowed methods
        System.out.println("CORS pattern is: " + config.server.allowedOriginPattern);
        router.route().handler(CorsHandler.create(config.server.allowedOriginPattern)
            .allowedMethod(HttpMethod.GET)      // select
            .allowedMethod(HttpMethod.POST)     // insert
            .allowedMethod(HttpMethod.PUT)      // update
            .allowedMethod(HttpMethod.DELETE)   // delete
            .allowedHeader("X-PINGARUNER")
            .allowedHeader("Content-Type"));

        // create cookie and session handler
        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(/*ClusteredSessionStore*/LocalSessionStore.create(vertx))
            .setCookieHttpOnlyFlag(true)
            .setCookieSecureFlag(true)
            .setCookieSecureFlag(true)
            .setSessionTimeout(TimeUnit.HOURS.toMillis(12))
        );

        // set secure headers in each response
        router.route().handler(rc -> {
            rc.response()
/*
                **X-Content-Type-Options**

                The 'X-Content-Type-Options' HTTP header if set to 'nosniff' stops the browser from guessing the MIME
                type of a file via content sniffing. Without this option set there is a potential increased risk of
                cross-site scripting.

                Secure configuration: Server returns the 'X-Content-Type-Options' HTTP header set to 'nosniff'.
*/
                .putHeader("X-Content-Type-Options", "nosniff")
/*
                **X-XSS-Protection**

                The 'X-XSS-Protection' HTTP header is used by Internet Explorer version 8 and higher. Setting this HTTP
                header will instruct Internet Explorer to enable its inbuilt anti-cross-site scripting filter. If
                enabled, but without 'mode=block' then there is an increased risk that otherwise non exploitable
                cross-site scripting vulnerabilities may potentially become exploitable.

                Secure configuration: Server returns the 'X-XSS-Protection' HTTP header set to '1; mode=block'.
*/
                .putHeader("X-XSS-Protection", "1; mode=block")
/*
                **X-Frame-Options**

                The 'X-Frame-Options' HTTP header can be used to indicate whether or not a browser should be allowed to
                render a page within a <frame> or <iframe>. The valid options are DENY, to deny allowing the page to
                exist in a frame or SAMEORIGIN to allow framing but only from the originating host. Without this option
                set the site is at a higher risk of click-jacking unless application level mitigations exist.

                Secure configuration: Server returns the 'X-Frame-Options' HTTP header set to 'DENY' or 'SAMEORIGIN'.
*/
                .putHeader("X-FRAME-OPTIONS", "DENY")
/*
                **Cache-Control**

                The 'Cache-Control' response header controls how pages can be cached either by proxies or the user's
                browser. Using this response header can provide enhanced privacy by not caching sensitive pages in the
                users local cache at the potential cost of performance. To stop pages from being cached the server sets
                a cache control by returning the 'Cache-Control' HTTP header set to 'no-store'.

                Secure configuration: Either the server sets a cache control by returning the 'Cache-Control' HTTP
                header set to 'no-store, no-cache' or each page sets their own via the 'meta' tag for secure
                connections.

                Updated: The above was updated after our friend Mark got in-touch. Originally we had said no-store was
                sufficient. But as with all things web related it appears Internet Explorer and Firefox work slightly
                differently (so everyone ensure you thank Mark!).
*/
                .putHeader("Cache-Control", "no-store, no-cache")
/*
                **Strict-Transport-Security**

                The 'HTTP Strict Transport Security' (Strict-Transport-Security) HTTP header is used to control if the
                browser is allowed to only access a site over a secure connection and how long to remember the server
                response for thus forcing continued usage.

                Note: This is a draft standard which only Firefox and Chrome support. But it is supported by sites such
                as PayPal. This header can only be set and honoured by web browsers over a trusted secure connection.

                Secure configuration: Return the 'Strict-Transport-Security' header with an appropriate timeout over an
                secure connection.
*/
                .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
/*
                **Access-Control-Allow-Origin**

                The 'Access Control Allow Origin' HTTP header is used to control which sites are allowed to bypass same
                origin policies and send cross-origin requests. This allows cross origin access without web application
                developers having to write mini proxies into their apps.

                Note: This is a draft standard which only Firefox and Chrome support, it is also advocarted by sites
                such as http://enable-cors.org/.

                Secure configuration: Either do not set or return the 'Access-Control-Allow-Origin' header restricting
                it to only a trusted set of sites.
*/
//                .putHeader("Access-Control-Allow-Origin", "a b c")
/*
                 IE8+ do not allow opening of attachments in the context of this resource
*/
                .putHeader("X-Download-Options", "noopen");
            // TODO: logger.debug only
            if (config.developMode) System.out.println("[" + rc.request().method() + "] " + rc.request().uri());
            rc.next();
        });

        // simple callback handler
        router.get("/callback").handler(rc -> {
            String error = getQueryParameter(rc.request().query(), "error");
            if (error != null) {
                System.err.println("ERROR: " + error);
                redirect(rc.response(), "/login");
            } else rc.next();
        });

        // configure local auth provider
        LocalAuthProvider localAuthProvider = LocalAuthProvider.create(vertx);
        // create local auth user session handler
//        router.route().handler(UserSessionHandler.create(localAuthProvider));

        // configure oauth2 auth provider
        OAuth2ClientOptions credentials = new OAuth2ClientOptions()
            .setClientID(config.oauth2.clientId)
            .setClientSecret(config.oauth2.clientSecret)
            .setSite(config.oauth2.site)
            .setTokenPath(config.oauth2.tokenPath)
            .setAuthorizationPath(config.oauth2.authPath);

        OAuth2Auth oauth2Provider = OAuth2Auth.create(vertx, OAuth2FlowType.AUTH_CODE, credentials);
        OAuth2AuthHandler oauth2Handler = OAuth2AuthHandler.create(oauth2Provider, config.rootOrigin());

        oauth2Handler.addAuthority(config.oauth2.scope);
        oauth2Handler.setupCallback(router.get("/callback"));
        // create oauth2 user session handler
        router.route().handler(UserSessionHandler.create(oauth2Provider));

        router.route(HttpMethod.PUT, "/api/*").handler(oauth2Handler);
        router.route(HttpMethod.DELETE, "/api/*").handler(oauth2Handler);

        // NOTE: "router.route("/auth/google/*").handler(oauth2Handler)" is not used
        //       since I didn't find a way to handle the state object with the default handler
        //       defined in that way!
//         router.route("/auth/google/*").handler(oauth2Handler);

        // Instead, this is the handler for all login providers:
        router.get("/auth/:provider/login").handler(rc -> {
//            System.out.println("query: " + rc.request().query());
            String state = getQueryParameter(rc.request().query(), "state");
            if (state != null) {
                String provider = rc.request().getParam("provider");
                switch (provider) {
                    case "google":
                        redirect(rc.response(), oauth2Handler.authURI("/auth/google/login/handler", state));
//                        redirect(rc.response(), oauth2Handler.authURI("/auth/google/login/handler", state));
                        break;
                    case "local":
                        rc.reroute("/auth/local/login/handler");
                        break;
                    default:
                        rc.put("message", "Unknown authentication provider: " + provider);
                        throw new IllegalStateException(rc.get("message").toString());
                }
            } else {
                redirect(rc.response(), "/login");
            }
        });

        router.get("/auth/google/login/handler").handler(rc -> {

            System.out.println("method: " + rc.request().method().toString());

            User user = rc.user();

            if (user != null) {

                String userId = null;
                {
                    Base64.Decoder decoder = Base64.getDecoder();
                    for (String s : rc.user().principal().getString("id_token").split("\\.")) {
                        try {
                            String decoded = new String(decoder.decode(s), StandardCharsets.UTF_8);
                            JsonObject o = new JsonObject(decoded);
                            userId = o.getString("sub");
                            if (userId != null) {
                                System.out.println("userId found: " + userId);
                                break;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                }
                if (userId == null) throw new IllegalStateException("user-id not found in user principal!");

                httpClient.request(HttpMethod.GET, 443, "www.googleapis.com", "/plus/v1/people/" + userId)
                    .putHeader("Authorization", "Bearer " + user.principal().getString("access_token"))
                    .handler(response -> {
                        response.bodyHandler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer buffer) {
                                System.out.println("Response " + buffer.length());
//                                System.out.println(buffer.getString(0, buffer.length()));
                                JsonObject profile = new JsonObject()
                                    .put("authenticationRole", AuthenticationRole.CITIZEN.toString())
                                    .put("authorizationRoles",
                                        new JsonObject()
                                            .put("*", new JsonArray(Arrays.asList(AuthorizationRole.OBSERVER.toString())))
                                    )
                                    .mergeIn(new JsonObject(buffer.getString(0, buffer.length())));
                                rc.user().principal()
                                    .put("provider", "google")
                                    .put("profile", profile);
                                System.out.println(Json.encodePrettily(rc.user().principal()));
                            }
                        });
                    })
                    .endHandler(v -> {
                        JsonObject state;
                        try {
                            state = new JsonObject(new String(Base64.getDecoder().decode(
                                getQueryParameter(rc.request().query(), "state"))));
                        } catch (Exception e) {
                            e.printStackTrace();
                            state = new JsonObject().put("redirect", "/");
                        }
//                        rc.response()
//                            .putHeader("content-type", "application/json; charset=utf-8")
//                            .end(state.encodePrettily());
                        redirect(rc.response(), state.getString("redirect"));
                    })
                    .end();
            } else {
                redirect(rc.response(), "/login");
            }

        });

        router.route("/auth/local/login/handler").handler(rc -> {
            System.out.println("query: " + rc.request().query());
            JsonObject state = new JsonObject(new String(Base64.getDecoder().decode(
                getQueryParameter(rc.request().query(), "state"))));
            localAuthProvider.authenticate(state.getJsonObject("authInfo"), rh -> {
                if (rh.failed()) {
                    redirect(rc.response(), "/login/" + Base64.getEncoder().encodeToString(
                        state.getString("redirect").getBytes()
                    ));
                } else {
                    LocalUser user = (LocalUser) rh.result();
                    System.out.println(Json.encodePrettily(user.principal()));
                    rc.setUser(user);
                    redirect(rc.response(), state.getString("redirect"));
                }
            });
        });

        router.route("/auth/logout").handler(rc -> {
            rc.setUser(null);
            rc.session().destroy();
            redirect(rc.response(), "/login");
        });

        router.route("/auth/isAuthenticated").handler(rc -> {
            rc.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encode(isAuthenticated(rc)));
        });

        router.get("/auth/profile").handler(rc -> {
            if (isAuthenticated(rc)) {
                rc.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(rc.user().principal().getJsonObject("profile").encode());
            } else redirect(rc.response(), "/login");
        });

        router.get("/auth/authenticationRole").handler(rc -> {
            if (isAuthenticated(rc)) {
                rc.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(rc.user().principal().getJsonObject("profile").getString("authenticationRole"));
            } else redirect(rc.response(), "/login");
        });

        router.get("/auth/authorizationRoles").handler(rc -> {
            if (isAuthenticated(rc)) {
                rc.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(rc.user().principal().getJsonObject("profile").getJsonObject("authorizationRoles").encode());
            } else redirect(rc.response(), "/login");
        });

        String api = config.server.api.base;

        // /api/*
        router.route(api + "/*").handler(BodyHandler.create());

        router.get(api + "/stats/diagram/:diagramId/eServiceCount").handler(this::getDiagramEServiceCount);
//        router.get(api + "/stats/diagram/element/:diagramElementId/eServiceCount").handler(this::getDiagramElementEServiceCount);

        router.get(api + "/type/list").handler(this::getTypeList);
        router.get(api + "/schema/:type").handler(this::getSchemaByType);

        router.get(api + "/diagram/summary/list").handler(this::getDiagramSummaryList);
        router.get(api + "/diagram/summary/notation/:notation/list").handler(this::getDiagramSummaryListByNotation);
//        router.get(api + "/diagram/summary/category/:category/list").handler(this::getDiagramSummaryListByCategory);
        router.get(api + "/diagram/:diagramId").handler(this::getDiagram);
        router.get(api + "/diagram/:diagramId/summary").handler(this::getDiagramSummary);
//        router.get(api + "/diagram/:diagramId/elements").handler(this::getDiagramElements);
//        router.get(api + "/diagram/:diagramId/:elementId").handler(this::getDiagramElement);
        router.get(api + "/diagram/:diagramId/:elementId/displayName").handler(this::getDiagramElementDisplayName);

        router.get(api + "/diagram/eService/:eServiceId/summary").handler(this::getDiagramEServiceSummary);
        router.get(api + "/diagram/eService/:eServiceId/element").handler(this::getDiagramEServiceElement);

        router.get(api + "/semantic/list").handler(this::getSemanticList);
        router.get(api + "/semantic/type/:type/list").handler(this::getSemanticListByType);
        router.get(api + "/semantic/:semanticId").handler(this::getSemanticElement);
        router.put(api + "/semantic/:semanticId").handler(this::putSemanticElement);
        router.get(api + "/semantic/:semanticId/accepts").handler(this::getSemanticElementAccepts);

        /*** STATIC RESOURCES ***/

        // redirect /api to /api/ !important;
        // it MUST be done with regex (must end exactly with "/api") to avoid infinite redirections
        router.getWithRegex("\\" + api + "$").handler(rc -> {
            redirect(rc.response(), api + "/");
        });
        router.get(api + "/*").handler(StaticHandler.create("web/swagger-ui"));

        // /assets/*
        router.get(config.server.assets.base + "/*").handler(StaticHandler.create("web/assets")
//            .setWebRoot("/")
            .setDirectoryListing(config.server.assets.allowListing));

        // let the ROOT application handle /diagram/* calls
        router.get("/diagram/*").handler(rc -> {
            rc.reroute("/");
        });
        // let the ROOT application handle /login calls
        router.get("/login*").handler(rc -> {
            rc.reroute("/");
        });

        // /*
        router.get("/*").handler(StaticHandler.create("web/ROOT")
            .setDirectoryListing(false)
            .setAllowRootFileSystemAccess(false)
            .setAlwaysAsyncFS(true)
            .setCachingEnabled(true)
            .setFilesReadOnly(true)
        );

        // handle failures
        router.route().failureHandler(rc -> {
            JsonObject error = rc.get("error") != null ? rc.get("error") : Error.fromRoutingContext(rc, null);
            System.err.println(Json.encodePrettily(rc.response()));
            System.err.println("ERROR (" + error.getInteger("statusCode") + "): " + error.encodePrettily());
            switch (rc.statusCode()) {
                case 404: {
                    // let root application find the resource or show the 404 not found page
                    rc.reroute("/");
                    break;
                }
                default: {
                    rc.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .setStatusCode(error.getInteger("statusCode"))
                        .end(error.encode());
                }
            }
        });

        vertx.createHttpServer(new HttpServerOptions()
                .setSsl(true)
                .setKeyStoreOptions(new JksOptions()
                    .setPath(config.keystore.filename)
                    .setPassword(config.keystore.password)
                )
                .setTrustStoreOptions(
                    new JksOptions()
                        .setPath(config.keystore.filename)
                        .setPassword(config.keystore.password)
                )
//            .setClientAuth(ClientAuth.REQUIRED)
        )
            .requestHandler(router::accept)
            .listen(config.server.port, ar -> {
                    if (ar.succeeded()) {
                        System.out.println("HTTP Server started: " + config.rootOrigin());
                    } else {
                        System.out.println("Cannot start HTTP Server: " + ar.cause());
                    }
                }
            );
    }

    private void getDiagramEServiceCount(RoutingContext rc) {
        simLagTime();
        String diagramId = rc.request().getParam("diagramId");
        rc.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(
                DiagramElement.listByDiagram(diagramId).stream()
                    .filter(d -> d.eServiceId != null)
                    .collect(Collectors.toList())
                    .size()));
    }

//    private void getDiagramElementEServiceCount(RoutingContext rc) {
//    }

    private void getTypeList(RoutingContext rc) {
        simLagTime();
        rc.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(Typed.knownTypes()));
    }

    private void getSchemaByType(RoutingContext rc) {
        simLagTime();
        String type = rc.request().getParam("type");
        HttpServerResponse response = rc.response();
        JsonNode schema = Typed.schema(type);
        if (schema == null) {
            throw new Error(rc, "schema '" + type + "' not found");
        } else response
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(schema));
    }

    private void getDiagramSummaryList(RoutingContext rc) {
        simLagTime();
        rc.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(Diagram.summaryList()));
    }

    private void getDiagramSummaryListByNotation(RoutingContext rc) {
        simLagTime();
        String notation = rc.request().getParam("notation");
        rc.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(Diagram.summaryList(notation)));
    }

    private void getDiagram(RoutingContext rc) {
        simLagTime();
        String diagramId = rc.request().getParam("diagramId");
        HttpServerResponse response = rc.response();
        Diagram diagram = Entity.get(diagramId, Diagram.class);
        if (diagram == null) {
            throw new Error(rc, "diagram " + diagramId + " not found");
        } else response
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(diagram));
    }

    private void getDiagramSummary(RoutingContext rc) {
        simLagTime();
        String diagramId = rc.request().getParam("diagramId");
        HttpServerResponse response = rc.response();
        Diagram diagram = Entity.get(diagramId, Diagram.class);
        if (diagram == null) {
            throw new Error(rc, "diagram " + diagramId + " not found");
        } else response
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(diagram.summary()));
    }

    private void getDiagramElements(RoutingContext rc) {
        simLagTime();
        String diagramId = rc.request().getParam("diagramId");
        rc.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(DiagramElement.listByDiagram(diagramId)));
    }

    private void getDiagramElement(RoutingContext rc) {
        simLagTime();
        String diagramId = rc.request().getParam("diagramId");
        String elementId = rc.request().getParam("elementId");
        HttpServerResponse response = rc.response();
        DiagramElement element = Entity.get(elementId, DiagramElement.class);
        if (element == null) {
            throw new Error(rc, "diagram element " + elementId + " not found");
        } else response
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(element));
    }

    private void getDiagramElementDisplayName(RoutingContext rc) {
        simLagTime();
        HttpServerResponse response = rc.response();
        String diagramId = rc.request().getParam("diagramId");
        Diagram diagram = Entity.get(diagramId, Diagram.class);
        if (diagram == null) {
            throw new Error(rc, "diagram " + diagramId + " not found");
        } else {
            String elementId = rc.request().getParam("elementId");
            DiagramElement element = diagram.element(elementId);
            if (element == null) {
                throw new Error(rc, "diagram element " + elementId + " not found");
            } else {
                SemanticElement diagramSemantic = Entity.get(diagram.semanticId, SemanticElement.class);
                if (diagramSemantic == null) {
                    throw new Error(rc, "semantic element " + diagram.semanticId + " not found");
                }
                SemanticElement elementSemantic = Entity.get(element.semanticId, SemanticElement.class);
                if (elementSemantic == null) {
                    throw new Error(rc, "semantic element " + element.semanticId + " not found");
                }
                response
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encode("[" + diagramSemantic.name + "]@[" + elementSemantic.name + "]"));
            }
        }
    }

    private void getDiagramEServiceSummary(RoutingContext rc) {
        simLagTime();
        String eServiceId = rc.request().getParam("eServiceId");
        HttpServerResponse response = rc.response();
        DiagramElement element = DiagramElement.getByEService(eServiceId);
        if (element == null) {
            throw new Error(rc, "diagram element associated to e-service " + eServiceId + " not found");
        } else response
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(element.diagramSummary()));
    }

    private void getDiagramEServiceElement(RoutingContext rc) {
        simLagTime();
        String eServiceId = rc.request().getParam("eServiceId");
        HttpServerResponse response = rc.response();
        DiagramElement element = DiagramElement.getByEService(eServiceId);
        if (element == null) {
            throw new Error(rc, "diagram element associated to e-service " + eServiceId + " not found");
        } else response
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(element));
    }

    private void getSemanticList(RoutingContext rc) {
        simLagTime();
        rc.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(SemanticElement.list()));
    }

    private void getSemanticListByType(RoutingContext rc) {
        simLagTime();
        String type = rc.request().getParam("type");
        rc.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(SemanticElement.listByType(type)));
    }

    private void getSemanticElement(RoutingContext rc) {
        simLagTime();
        String semanticId = rc.request().getParam("semanticId");
        HttpServerResponse response = rc.response();
        SemanticElement element = Entity.get(semanticId, SemanticElement.class);
        if (element == null) {
            throw new Error(rc, "semantic element " + semanticId + " not found");
        } else response
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(element));
    }

    private void putSemanticElement(RoutingContext rc) {
        simLagTime();
        System.out.println("received " + rc.getBodyAsString());
        JsonObject json = new JsonObject(rc.getBodyAsString());
        Entity entity = Entity.get(json.getString("id"));
        if (entity == null)
            throw new Error(rc, "semantic element " + json.getString("id") + " not found");
        if (!entity.getType().equals(json.getString("type")))
            throw new Error(rc, "expected type " + entity.getType() + " but received " + json.getString("type"));
        try {
            SemanticElement semantic = SemanticElement.createOrUpdate(json);
            if (semantic == null)
                throw new Error(rc, "could not update semantic " + json.getString("id"));
            // check, validate, transform
            Entity.put(semantic);
            rc.response()
                .setStatusCode(HttpResponseStatus.ACCEPTED.code())
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(semantic));
        } catch (DecodeException e) {
            throw new Error(rc, e);
        }
    }

    private void getSemanticElementAccepts(RoutingContext rc) {
        simLagTime();
        String semanticId = rc.request().getParam("semanticId");
        HttpServerResponse response = rc.response();
        SemanticElement element = Entity.get(semanticId, SemanticElement.class);
        if (element == null) {
            throw new Error(rc, "semantic element " + semanticId + " not found");
        } else response
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(AcceptMatrix.accepts(element)));
    }

}
