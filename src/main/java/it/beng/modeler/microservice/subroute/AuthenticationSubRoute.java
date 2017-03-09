package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.impl.CookieImpl;
import it.beng.modeler.config;
import it.beng.modeler.microservice.auth.local.LocalAuthHandler;
import it.beng.modeler.microservice.auth.local.LocalAuthProvider;
import it.beng.modeler.microservice.auth.local.impl.LocalUser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class AuthenticationSubRoute extends SubRoute {

    private static String AUTHENTICATION_ROLE_CITIZEN = "semantic:organization:roles:AuthenticationRole:citizen";
    private static JsonObject AUTHORIZATION_ROLES_OBSERVER = new JsonObject("{ \"*\": [ \"semantic:organization:roles:AuthorizationRole:observer\" ] }");

    public AuthenticationSubRoute(Vertx vertx, Router router, MongoClient mongodb) {
        super(vertx, router, mongodb);
    }

    @Override
    protected void init() {

        String auth = config.server.auth.base;

        // default auth callback handler
        router.route(HttpMethod.GET, auth + "/callback").handler(rc -> {
            String error = getQueryParameter(rc.request().query(), "error");
            if (error != null) {
                System.err.println("ERROR: " + error);
                redirect(rc.response(), "/login");
            } else rc.next();
        });

        // configure local auth provider
        LocalAuthProvider localAuthProvider = LocalAuthProvider.create(vertx);
        LocalAuthHandler localAuthHandler = LocalAuthHandler.create(localAuthProvider);

        // TODO: check these out
//        router.route(HttpMethod.GET, "/api/*").handler(localAuthHandler);
//        router.route(HttpMethod.POST, "/api/*").handler(localAuthHandler);
//        router.route(HttpMethod.PUT, "/api/*").handler(localAuthHandler);
//        router.route(HttpMethod.DELETE, "/api/*").handler(localAuthHandler);

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
        oauth2Handler.setupCallback(router.route(HttpMethod.GET, auth + "/callback"));
        // create oauth2 user session handler
        router.route().handler(UserSessionHandler.create(oauth2Provider));

        // NOTE: "router.route("/auth/google/*").handler(oauth2Handler)" is not used
        //       since I didn't find a way to handle the state object with the default handler
        //       defined in that way!
//         router.route("/auth/google/*").handler(oauth2Handler);

        // Instead, this is the handler for all login providers:
        router.route(HttpMethod.GET, auth + "/:provider/login").handler(rc -> {
//            System.out.println("query: " + rc.request().query());
            String state = getQueryParameter(rc.request().query(), "state");
            if (state != null) {
                String provider = rc.request().getParam("provider");
                switch (provider) {
                    case "google":
                        redirect(rc.response(), oauth2Handler.authURI(auth + "/google/login/handler", state));
//                        redirect(rc.response(), oauth2Handler.authURI("/auth/google/login/handler", state));
                        break;
                    case "local":
                        rc.reroute(auth + "/local/login/handler");
                        break;
                    default:
                        rc.put("message", "Unknown authentication provider: " + provider);
                        throw new IllegalStateException(rc.get("message").toString());
                }
            } else {
                redirect(rc.response(), "/login");
            }
        });

        router.route(HttpMethod.GET, auth + "/google/login/handler").handler(rc -> {

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
                String token = user.principal().getString("access_token");
                rc.addCookie(
                    new CookieImpl("token", token)
//                    Cookie.cookie("token", token)
                );

                vertx.createHttpClient(
                    new HttpClientOptions().setSsl(true)
/*
                        .setDefaultHost("localhost")
                        .setDefaultPort(config.server.port)
                        .setKeyStoreOptions(new JksOptions()
                            .setPath(config.keystore.keyStoreFilename)
                            .setPassword(config.keystore.keyStorePassword))
*/
                )
                     .request(HttpMethod.GET, 443, "www.googleapis.com", "/plus/v1/people/" + userId)
                     .putHeader("Authorization", "Bearer " + token)
                     .handler(response -> {
                         response.bodyHandler(buffer -> {
//                            System.out.println("Response " + buffer.length());
                             String json = buffer.getString(0, buffer.length());
//                                System.out.println(json);
                             JsonObject profile = new JsonObject()
                                 .put("token", token)
                                 .put("authenticationRole", AUTHENTICATION_ROLE_CITIZEN)
                                 .put("authorizationRoles", AUTHORIZATION_ROLES_OBSERVER)
                                 .mergeIn(new JsonObject(json));
                             rc.user().principal()
                               .put("provider", "google")
                               .put("profile", profile);
                             System.out.println(Json.encodePrettily(rc.user().principal()));
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

        router.route(HttpMethod.GET, auth + "/local/login/handler").handler(rc -> {
            System.out.println("query: " + rc.request().query());
            JsonObject state = new JsonObject(new String(Base64.getDecoder().decode(
                getQueryParameter(rc.request().query(), "state"))));
            localAuthProvider.authenticate(state.getJsonObject("authInfo"), rh -> {
                if (rh.failed()) {
                    redirect(rc.response(), "/login/" + Base64.getEncoder().encodeToString(
                        state.getString("redirect").getBytes()));
                } else {
                    LocalUser user = (LocalUser) rh.result();
                    System.out.println(Json.encodePrettily(user.principal()));
                    rc.setUser(user);
                    redirect(rc.response(), state.getString("redirect"));
                }
            });
        });

        router.route(HttpMethod.GET, auth + "/logout").handler(rc -> {
            rc.setUser(null);
            rc.session().destroy();
            redirect(rc.response(), "/login");
        });

        router.route(HttpMethod.GET, auth + "/isAuthenticated").handler(rc -> {
            rc.response()
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(Json.encode(isAuthenticated(rc)));
        });

        router.route(HttpMethod.GET, auth + "/profile").handler(rc -> {
//            if (isAuthenticated(rc)) {
            rc.response()
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(isAuthenticated(rc) ?
                  rc.user().principal().getJsonObject("profile").encode() :
                  Json.encode(null));
//            } else redirect(rc.response(), "/login");
        });

        router.route(HttpMethod.GET, auth + "/authenticationRole").handler(rc -> {
            if (isAuthenticated(rc)) {
                rc.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(rc.user().principal().getJsonObject("profile").getString("authenticationRole"));
            } else redirect(rc.response(), "/login");
        });

        router.route(HttpMethod.GET, auth + "/authorizationRoles").handler(rc -> {
            if (isAuthenticated(rc)) {
                rc.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(rc.user().principal().getJsonObject("profile").getJsonObject("authorizationRoles").encode());
            } else redirect(rc.response(), "/login");
        });

    }

}
