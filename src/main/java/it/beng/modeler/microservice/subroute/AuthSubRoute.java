package it.beng.modeler.microservice.subroute;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import it.beng.modeler.config;
import it.beng.modeler.microservice.http.JsonResponse;
import it.beng.modeler.microservice.subroute.auth.LocalAuthSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2AuthCodeSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2ClientSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2ImplicitSubRoute;
import it.beng.modeler.microservice.utils.AuthUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class AuthSubRoute extends VoidSubRoute {

    private static final Log logger = LogFactory.getLog(AuthSubRoute.class);

    static List<String> knownProviders = new LinkedList<>();

    public AuthSubRoute(Vertx vertx, Router router) {
        super(config.server.auth.path, vertx, router, false);
    }

    public static void checkEncodedStateStateCookie(RoutingContext context, String encodedState) {
        if (encodedState == null || !encodedState.equals(context.session().get("encodedState"))) {
            logger.error(HttpResponseStatus.NOT_FOUND.toString());
            throw new IllegalStateException("invalid login transaction");
        }
        logger.debug("state check succesfull");
    }

    @Override
    protected void init() {

        // local/login
        if (config.app.useLocalAuth) {
            knownProviders.add(LocalAuthSubRoute.PROVIDER);
            new LocalAuthSubRoute(vertx, router);
        }

        // oauth2provider/login
        for (config.OAuth2Config oAuth2Config : config.oauth2.configs) {
            knownProviders.add(oAuth2Config.provider);
            for (final String flowType : oAuth2Config.flows.keySet()) {
                switch (flowType) {
                    case OAuth2AuthCodeSubRoute.FLOW_TYPE:
                        new OAuth2AuthCodeSubRoute(vertx, router, oAuth2Config);
                        break;
                    case OAuth2ClientSubRoute.FLOW_TYPE:
                        new OAuth2ClientSubRoute(vertx, router, oAuth2Config);
                        break;
                    case OAuth2ImplicitSubRoute.FLOW_TYPE:
                        new OAuth2ImplicitSubRoute(vertx, router, oAuth2Config);
                        break;
                    case "PASSWORD":
                        logger.warn("PASSWORD oauth2 flow type not yet implemented");
                        continue;
                    case "AUTH_JWT":
                        logger.warn("AUTH_JWT oauth2 flow type not yet implemented");
                        continue;
                    default:
                        logger.warn("Provider '" + oAuth2Config.provider + "' is unknown and will not be available.");
                        continue;
                }
                logger.info("Provider '" + oAuth2Config.provider + "' will follow the '" + flowType + "' flow.");
            }
        }

        logger.debug("known providers are " + knownProviders);

        router.route(path + "login/:provider").handler(this::login);

        router.route(HttpMethod.GET, path + "logout").handler(this::logout);

        /* API */

        // getOAuth2Providers
        router.route(HttpMethod.GET, path + "oauth2/providers").handler(this::getOAuth2Providers);
        // getUser
        router.route(HttpMethod.GET, path + "user").handler(this::getUser);
        // getUserIsAuthenticated
        router.route(HttpMethod.GET, path + "user/isAuthenticated").handler(this::getUserIsAuthenticated);
        // getUserHasAccess
        // router.route(HttpMethod.GET, path + "user/hasAccess/:accessRole").handler(this::getUserHasAccess);
        // getUserIsAuthorized
        // router.route(HttpMethod.GET, path + "user/isAuthorized/:contextName/:contextId/:contextRole").handler(this::getUserIsAuthorized);

        // getAccounts
        router.route(HttpMethod.GET, path + "accounts").handler(this::getAccounts);
        router.route(HttpMethod.PUT, path + "accounts").handler(this::putAccounts);

    }

    private void login(RoutingContext context) {
        String provider = context.pathParam("provider");
        if (!knownProviders.contains(provider)) {
            context.fail(HttpResponseStatus.UNAUTHORIZED.code());
            return;
        }
        JsonObject loginState = new JsonObject().put("loginId", UUID.randomUUID().toString()).put("provider", provider);
        List<String> redirect = context.queryParam("redirect");
        if (redirect != null && redirect.size() > 0)
            try {
                loginState.put("redirect", base64.decode(redirect.get(0)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        if (loginState.getValue("redirect") == null)
            loginState.put("redirect", "/");
        logger.debug("login state: " + loginState.encodePrettily());

        if ("local".equals(provider)) {
            context.put("loginState", loginState);
        } else {
            JsonObject state = new JsonObject().put("loginState", loginState);
            context.session().put("encodedState", base64.encode(state.encode()));
        }
        context.reroute(path + provider + "/login/handler");
    }

    private void logout(RoutingContext context) {
        if (context.user() instanceof AccessToken) {
            try {
                ((AccessToken) context.user()).logout(logout -> {
                    if (logout.failed()) {
                        logger.error("user could not logout: " + context.user().principal().encodePrettily());
                        context.next();
                    }
                });
            } catch (Exception e) {
                logger.debug("cannot logout user: " + context.user().principal().encodePrettily());
            }
        }
        context.clearUser();
        Session session = context.session();
        if (session != null) {
            session.destroy();
        }
        new JsonResponse(context).end(true);
    }

    private void getOAuth2Providers(RoutingContext context) {
        JsonArray providers = new JsonArray();
        for (config.OAuth2Config providerConfig : config.oauth2.configs) {
            providers
                .add(new JsonObject().put("provider", providerConfig.provider).put("logoUrl", providerConfig.logoUrl));
        }
        new JsonResponse(context).end(providers);
    }

    private void getUserIsAuthenticated(RoutingContext context) {
        new JsonResponse(context).end(context.user() != null);
    }

    private void getUser(RoutingContext context) {
        User user = context.user();
        if (user != null) {
            if (AuthUtils.getAccount(user) == null) {
                context.clearUser();
                user = null;
            } else {
                new JsonResponse(context).end(context.user().principal());
            }
        }
        if (user == null) {
            new JsonResponse(context).end(null);
        }
    }

    private void getAccounts(RoutingContext context) {
        if (isAdminFailOtherwise(context)) {
            mongodb.find("users", new JsonObject(), users -> {
                if (users.succeeded()) {
                    new JsonResponse(context).end(users.result());
                } else {
                    context.fail(users.cause());
                }
            });
        }
    }

    private void putAccounts(RoutingContext context) {
        if (isAdminFailOtherwise(context)) {
            JsonObject account = context.getBodyAsJson();
            String id = (String) account.remove("id");
            if (id == null) {
                context.fail(new NullPointerException());
                return;
            }
            JsonObject query = new JsonObject().put("id", id);
            mongodb.findOneAndReplace("users", query, account, update -> {
                if (update.succeeded()) {
                    new JsonResponse(context).end(update.result());
                    logger.debug("Account UPDATED: " + account.encodePrettily());
                } else {
                    context.fail(update.cause());
                }
            });
        }
    }

    // private void getUserHasAccess(RoutingContext context) {
    //     final User user = context.user();
    //     final String accessRole = context.pathParam("accessRole");
    //     // isAuthorized(context, accessRole, ar -> {
    //     //     if (ar.succeeded()) {
    //     //         JSON_HEADER_RESPONSE(context).end(ar.result().toString());
    //     //     } else {
    //     //         throw new ResponseError(context, ar.cause());
    //     //     }
    //     // });
    // }

    // private void getUserIsAuthorized(RoutingContext context) {
    //     User user = context.user();
    //     if (user == null) {
    //         new JsonResponse(context).end(false);
    //     } else {
    //         // TODO: use LocalUser.isAuthorized(authority, userRoles, resultHandler);
    //         new JsonResponse(context).end(true);
    //     }
    // }

}
