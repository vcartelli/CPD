package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.db.MongoDB;
import it.beng.microservice.schema.SchemaTools;
import it.beng.modeler.config;
import it.beng.modeler.microservice.ResponseError;
import it.beng.modeler.microservice.subroute.auth.LocalAuthSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2AuthCodeSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2ClientSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2ImplicitSubRoute;
import it.beng.modeler.model.ModelTools;

import java.util.UUID;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class AuthSubRoute extends VoidSubRoute {

    public AuthSubRoute(Vertx vertx, Router router, MongoDB mongodb, SchemaTools schemaTools, ModelTools modelTools) {
        super(config.server.auth.path, vertx, router, mongodb, schemaTools, modelTools);
    }

    public static JsonObject getState(RoutingContext rc) {
        return rc.session().get("state");
    }

    public static String getBase64EncodedState(RoutingContext rc) {
        return base64.encode(getState(rc).encode());
    }

    public static void setState(RoutingContext rc, JsonObject state) {
        rc.session().put("state", state);
    }

    public static void loginRedirect(RoutingContext rc) {
        JsonObject state = AuthSubRoute.getState(rc);
        String appHref = config.server.appHref(rc);
        if (rc.user() != null)
            redirect(rc, appHref + state.getString("redirect"));
        else {
            appHref += "login";
            if (!"".equals(state.getString("redirect"))) {
                appHref += "/" + base64.encode(state.encode());
            }
            redirect(rc, appHref);
        }
    }

    public static void checkState(RoutingContext rc, String encodedState) {
        if (encodedState == null || !encodedState.equals(getBase64EncodedState(rc))) {
            throw new ResponseError(rc, "invalid login transaction");
        }
    }

    @Override
    protected void init() {

        router.route(HttpMethod.GET, path + "login/:provider").handler(this::login);

        // local/login
        new LocalAuthSubRoute(vertx, router, mongodb, schemaTools, modelTools);

        // oauth2provider/login
        for (config.OAuth2Config oAuth2Config : config.oauth2.configs) {
            for (String flowType : oAuth2Config.flows.keySet()) {
                switch (flowType) {
                    case OAuth2AuthCodeSubRoute.FLOW_TYPE:
                        new OAuth2AuthCodeSubRoute(vertx, router, mongodb, schemaTools, modelTools, oAuth2Config);
                        break;
                    case OAuth2ClientSubRoute.FLOW_TYPE:
                        new OAuth2ClientSubRoute(vertx, router, mongodb, schemaTools, modelTools, oAuth2Config);
                        break;
                    case "PASSWORD":
                        break;
                    case "AUTH_JWT":
                        break;
                    case OAuth2ImplicitSubRoute.FLOW_TYPE:
                        new OAuth2ImplicitSubRoute(vertx, router, mongodb, schemaTools, modelTools, oAuth2Config);
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
            loginRedirect(rc);
        });

        // logout
        router.route(HttpMethod.GET, path + "logout").handler(this::logout);

        /* API */

        // getOAuth2Providers
        router.route(HttpMethod.GET, path + "oauth2/providers").handler(this::getOAuth2Providers);
        // getUserProfile
        router.route(HttpMethod.GET, path + "user").handler(this::getUser);
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
                state = new JsonObject(base64.decode(encodedState));
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
        // TODO: save user state
        rc.clearUser();
        rc.session().destroy();
        redirect(rc, config.server.appHref(rc) + "login");
    }

    private void getOAuth2Providers(RoutingContext rc) {
        JsonArray providers = new JsonArray();
        for (config.OAuth2Config config : config.oauth2.configs) {
            providers.add(new JsonObject()
                .put("provider", config.provider)
                .put("logoUrl", config.logoUrl));
        }
        JSON_ARRAY_RESPONSE_END(rc, providers);
    }

    private void getUserIsAuthenticated(RoutingContext rc) {
        JSON_HEADER_RESPONSE(rc).end(Boolean.toString(rc.user() != null));
    }

    private void getUser(RoutingContext rc) {
        if (rc.user() == null) JSON_NULL_RESPONSE(rc);
        else {
            final JsonObject user = new JsonObject().put("profile", rc.user().principal().getJsonObject("profile"));
            isAuthorized(rc, config.role.cpd.access.admin, isAdmin -> {
                if (isAdmin.succeeded()) {
                    if (isAdmin.result()) {
                        JSON_OBJECT_RESPONSE_END(rc, user.put("isAdmin", true));
                    } else isAuthorized(rc, config.role.cpd.access.civilServant, isCivilServant -> {
                        if (isCivilServant.succeeded()) {
                            if (isCivilServant.result()) {
                                JSON_OBJECT_RESPONSE_END(rc, user.put("isCivilServant", true));
                            } else {
                                JSON_OBJECT_RESPONSE_END(rc, user.put("isCitizen", true));
                            }
                        } else throw new ResponseError(rc, isCivilServant.cause());
                    });
                } else throw new ResponseError(rc, isAdmin.cause());
            });
        }
    }

    private void getUserHasAccess(RoutingContext rc) {
        final User user = rc.user();
        final String accessRole = rc.request().getParam("accessRole");
        isAuthorized(rc, accessRole, ar -> {
            if (ar.succeeded()) {
                JSON_HEADER_RESPONSE(rc).end(ar.result().toString());
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void getUserIsAuthorized(RoutingContext rc) {
        User user = rc.user();
        if (user == null) {
            JSON_HEADER_RESPONSE(rc).end(Boolean.FALSE.toString());
        } else {
            String contextName = rc.request().getParam("contextName");
            String contextId = rc.request().getParam("contextId");
            String contextRole = rc.request().getParam("contextRole");
            isAuthorized(rc, contextName + "|" + contextId + "|" + contextRole, ar -> {
                if (ar.succeeded()) {
                    JSON_HEADER_RESPONSE(rc).end(ar.result().toString());
                } else {
                    throw new ResponseError(rc, ar.cause());
                }
            });
        }
    }

}
