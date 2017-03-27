package it.beng.modeler.microservice.subroute;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import it.beng.modeler.config;
import it.beng.modeler.microservice.ResponseError;
import it.beng.modeler.microservice.subroute.auth.LocalAuthSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2AuthCodeSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2ClientSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2ImplicitSubRoute;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class AuthSubRoute extends SubRoute {

    public AuthSubRoute(Vertx vertx, Router router, MongoClient mongodb) {
        super(config.server.auth.path, vertx, router, mongodb);
    }

    public static JsonObject getState(RoutingContext rc) {
        return rc.session().get("state");
    }

    public static String getBase64EncodedState(RoutingContext rc) {
        return Base64.getEncoder().withoutPadding().encodeToString(getState(rc).encode().getBytes());
    }

    public static void setState(RoutingContext rc, JsonObject state) {
        rc.session().put("state", state);
    }

    public static void loginRedirect(RoutingContext rc, String baseHref) {
        JsonObject state = AuthSubRoute.getState(rc);
        System.out.println("state: " + state.encodePrettily());
        if (rc.user() != null)
            redirect(rc, state.getString("redirect"));
        else {
            String redirect = baseHref + "login";
            if (!"/".equals(state.getString("redirect"))) {
                redirect += "/" + Base64.getEncoder().withoutPadding().encodeToString(state.encode().getBytes());
            }
            redirect(rc, redirect);
        }
    }

    public static void checkState(RoutingContext rc, String encodedState) {
        if (encodedState == null || !encodedState.equals(getBase64EncodedState(rc))) {
            throw new ResponseError(rc, "invalid login transaction");
        }
    }

    @Override
    protected void init(Object userData) {

        router.route(HttpMethod.GET, path + ":provider/login").handler(this::login);

        // local/login
        new LocalAuthSubRoute(vertx, router, mongodb);

        // oauth2provider/login
        for (config.OAuth2Config oAuth2Config : config.oauth2.configs) {
            for (String flowType : oAuth2Config.flows.keySet()) {
                switch (flowType) {
                    case OAuth2AuthCodeSubRoute.FLOW_TYPE:
                        new OAuth2AuthCodeSubRoute(vertx, router, mongodb, oAuth2Config);
                        break;
                    case OAuth2ClientSubRoute.FLOW_TYPE:
                        new OAuth2ClientSubRoute(vertx, router, mongodb, oAuth2Config);
                        break;
                    case "PASSWORD":
                        break;
                    case "AUTH_JWT":
                        break;
                    case OAuth2ImplicitSubRoute.FLOW_TYPE:
                        new OAuth2ImplicitSubRoute(vertx, router, mongodb, oAuth2Config);
                        break;
                    default: {
                        System.err.println("Provider '" + oAuth2Config.provider + "' will not be available.");
                        continue;
                    }
                }
                System.out
                    .println("Provider '" + oAuth2Config.provider + "' will follow the '" + flowType + "' flow.");
            }
        }
        router.route(HttpMethod.GET, path + ":provider/login/handler").handler(rc -> {
            if (config.develop) System.out.println("provider: " + rc.request().getParam("provider"));
            if (config.develop) System.out.println("redirect: " + rc.session().get("redirect"));
            loginRedirect(rc, baseHref);
        });

        // logout
        router.route(HttpMethod.GET, path + "logout").handler(this::logout);

        /* API */

        // getOAuth2Providers
        router.route(HttpMethod.GET, path + "oauth2/providers").handler(this::getOAuth2Providers);
        // getUserIsAuthenticated
        router.route(HttpMethod.GET, path + "user/isAuthenticated").handler(this::getUserIsAuthenticated);
        // getUserProfile
        router.route(HttpMethod.GET, path + "user/profile").handler(this::getUserProfile);
        // getUserProfileImage
        router.route(HttpMethod.GET, path + "user/profile/image").handler(this::getUserProfileImage);

    }

    private void login(RoutingContext rc) {
        String provider = rc.request().getParam("provider");
        rc.clearUser();
        if (config.develop) System.out.println("user cleared");
        String encodedState = getQueryParameter(rc.request().query(), "state");
        if (config.develop) System.out.println("encoded state: " + encodedState);
        JsonObject state = null;
        if (encodedState != null)
            try {
                state = new JsonObject(new String(Base64.getDecoder().decode(encodedState)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        if (state == null) state = new JsonObject().put("redirect", "/");
        state.put("provider", provider).put("login_id", UUID.randomUUID().toString());
        if (config.develop) System.out.println("state: " + state.encodePrettily());
        setState(rc, state);
        rc.reroute(path + provider + "/login/handler");
    }

    private void logout(RoutingContext rc) {
        rc.clearUser();
        redirect(rc, baseHref + "login");
    }

    private void getOAuth2Providers(RoutingContext rc) {
        List<JsonObject> providers = new LinkedList<>();
        for (config.OAuth2Config config : config.oauth2.configs) {
            providers.add(new JsonObject()
                .put("provider", config.provider)
                .put("logoUrl", config.logoUrl));
        }
        JSON_RESPONSE(rc).end(toClient(providers));
    }

    private void getUserIsAuthenticated(RoutingContext rc) {
        JSON_RESPONSE(rc).end(Boolean.toString(rc.user() != null));
    }

    private void getUserProfile(RoutingContext rc) {
        if (rc.user() == null) NULL_RESPONSE(rc);
        else {
            JsonObject profile = rc.user().principal().getJsonObject("profile");
            JSON_RESPONSE(rc).end(toClient(profile));
        }
    }

    private static String imageUrl(JsonObject image) {
        if (image != null) {
            try {
                URL url = new URL(image.getString("url"));
                return url.getProtocol() + "://" + url.getHost() +
                    (url.getPort() != -1 ? ":" + url.getPort() : "") + url.getPath();
            } catch (MalformedURLException e) {}
        }
        return null;
    }

    private void getUserProfileImage(RoutingContext rc) {
        checkAuthenticated(rc);
        final String userId = rc.user().principal().getJsonObject("profile").getString("id");
        mongodb.findOne("userBinData",
            new JsonObject().put("_id", userId),
            new JsonObject().put("image", 1),
            mongoFindResult -> {
                if (mongoFindResult.succeeded()) {
                    if (mongoFindResult.result() != null) {
                        final JsonObject image = mongoFindResult.result().getJsonObject("image");
                        final String contentType = image.getString("contentType");
                        final Buffer buffer = Buffer.buffer(image.getBinary("bytes"));
                        rc.response()
                          .putHeader("content-type", contentType)
                          .end(buffer);
                    } else {
                        final String imageUrl = imageUrl(
                            rc.user().principal().getJsonObject("profile").getJsonObject("image"));
                        if (imageUrl != null) {
                            final WebClient client = WebClient.create(
                                vertx,
                                new WebClientOptions()
                                    .setUserAgent("CPD-WebClient/1.0")
                                    .setFollowRedirects(false));
                            client.requestAbs(HttpMethod.GET, imageUrl)
                                  .send(cr -> {
                                      if (cr.succeeded()) {
                                          HttpResponse<Buffer> response = cr.result();
                                          if (response.statusCode() == HttpResponseStatus.OK.code()) {
                                              final String contentType = response.getHeader("content-type");
                                              System.out.println("content-type: " + contentType);
                                              final Buffer image = response.body();
                                              mongodb.insert("userBinData",
                                                  new JsonObject()
                                                      .put("_id", userId)
                                                      .put("image", new JsonObject()
                                                          .put("contentType", contentType)
                                                          .put("bytes", image.getBytes())),
                                                  ih -> {
                                                      if (ih.succeeded()) {
                                                          rc.response()
                                                            .putHeader("content-type", contentType)
                                                            .end(image);
                                                      } else {
                                                          JSON_RESPONSE(rc).end("null");
                                                      }
                                                  });
                                          } else {
                                              JSON_RESPONSE(rc).end("null");
                                          }
                                      } else {
                                          JSON_RESPONSE(rc).end("null");
                                      }
                                  });
                        } else {
                            JSON_RESPONSE(rc).end("null");
                        }
                    }
                } else {
                    mongoFindResult.cause().printStackTrace();
                }
            });
    }

}
