package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import it.beng.modeler.config;
import it.beng.modeler.microservice.ResponseError;
import it.beng.modeler.microservice.subroute.auth.LocalAuthSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2AuthCodeSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2ClientSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2ImplicitSubRoute;

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
        return Base64.getEncoder().encodeToString(getState(rc).encode().getBytes());
    }

    public static void setState(RoutingContext rc, JsonObject state) {
        rc.session().put("state", state);
    }

    public static void loginRedirect(RoutingContext rc, String baseHref) {
        JsonObject state = AuthSubRoute.getState(rc);
        String path = config.server.appPath(rc);
        if (rc.user() != null)
            redirect(rc, path + state.getString("redirect"));
        else {
            path += "/login";
            if (!"/".equals(state.getString("redirect"))) {
                path += "/" + Base64.getEncoder().encodeToString(state.encode().getBytes());
            }
            redirect(rc, path);
        }
    }

    public static void checkState(RoutingContext rc, String encodedState) {
        if (encodedState == null || !encodedState.equals(getBase64EncodedState(rc))) {
            throw new ResponseError(rc, "invalid login transaction");
        }
    }

    @Override
    protected void init(Object userData) {

        router.route(HttpMethod.GET, path + "login/:provider").handler(this::login);

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
            loginRedirect(rc, baseHref);
        });

        // logout
        router.route(HttpMethod.GET, path + "logout").handler(this::logout);

        /* API */

        // getOAuth2Providers
        router.route(HttpMethod.GET, path + "oauth2/providers").handler(this::getOAuth2Providers);
        // getUserProfile
        router.route(HttpMethod.GET, path + "user/profile").handler(this::getUserProfile);
        // getUserIsAuthenticated
        router.route(HttpMethod.GET, path + "user/isAuthenticated").handler(this::getUserIsAuthenticated);
        // getUserHasAccess
        router.route(HttpMethod.GET, path + "user/hasAccess/:accessRole").handler(this::getUserHasAccess);
        // getUserIsAuthorized
        router.route(HttpMethod.GET, path + "user/isAuthorized/:contextName/:contextId/:contextRole")
              .handler(this::getUserIsAuthorized);

    }

    private void login(RoutingContext rc) {
        String provider = rc.request().getParam("provider");
        rc.clearUser();
        if (config.develop) System.out.println("user cleared");
        String encodedState = rc.request().getParam("state");
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
        redirect(rc, config.server.appPath(rc) + "/login");
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

    private void getUserHasAccess(RoutingContext rc) {
        if (rc.user() == null) JSON_RESPONSE(rc).end("false");
        String accessRole = rc.request().getParam("accessRole");
        rc.user().isAuthorised(accessRole, ar -> {
            if (ar.succeeded()) {
                JSON_RESPONSE(rc).end(Boolean.toString(ar.result()));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void getUserIsAuthorized(RoutingContext rc) {
        if (rc.user() == null) JSON_RESPONSE(rc).end("false");
        if (rc.user() instanceof AccessToken) {
            AccessToken token = (AccessToken) rc.user();
            if (token.expired()) {
                System.out.println("before refresh: " + rc.user().principal().encodePrettily());
                token.refresh(ar -> {
                    if (ar.succeeded()) {
                        System.out.println("after refresh: " + rc.user().principal().encodePrettily());
                    } else {
                        logout(rc);
                    }
                });
            }
        }
        String contextName = rc.request().getParam("contextName");
        String contextId = rc.request().getParam("contextId");
        String contextRole = rc.request().getParam("contextRole");
        rc.user().isAuthorised(contextName + "|" + contextId + "|" + contextRole, ar -> {
            if (ar.succeeded()) {
                JSON_RESPONSE(rc).end(Boolean.toString(ar.result()));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

}
