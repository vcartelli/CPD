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
import it.beng.microservice.common.ServerError;
import it.beng.modeler.config;
import it.beng.modeler.microservice.http.JsonResponse;
import it.beng.modeler.microservice.subroute.auth.LocalAuthSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2AuthCodeSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2ClientSubRoute;
import it.beng.modeler.microservice.subroute.auth.OAuth2ImplicitSubRoute;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class AuthSubRoute extends VoidSubRoute {

    private static Logger logger = Logger.getLogger(AuthSubRoute.class.getName());

    static List<String> knownProviders = new LinkedList<>();

    public AuthSubRoute(Vertx vertx, Router router) {
        super(config.server.auth.path, vertx, router, false);
    }

    public static void checkEncodedStateStateCookie(RoutingContext context, String encodedState) {
        if (encodedState == null || !encodedState.equals(context.session().get("encodedState"))) {
            logger.severe(HttpResponseStatus.NOT_FOUND.toString());
            throw ServerError.message("invalid login transaction");
        }
        logger.fine("state check succesfull");
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
            for (String flowType : oAuth2Config.flows.keySet()) {
                switch (flowType) {
                    case OAuth2AuthCodeSubRoute.FLOW_TYPE:
                        new OAuth2AuthCodeSubRoute(vertx, router, oAuth2Config);
                        break;
                    case OAuth2ClientSubRoute.FLOW_TYPE:
                        new OAuth2ClientSubRoute(vertx, router, oAuth2Config);
                        break;
                    case "PASSWORD":
                        break;
                    case "AUTH_JWT":
                        break;
                    case OAuth2ImplicitSubRoute.FLOW_TYPE:
                        new OAuth2ImplicitSubRoute(vertx, router, oAuth2Config);
                        break;
                    default: {
                        logger.warning("Provider '" + oAuth2Config.provider + "' will not be available.");
                        continue;
                    }
                }
                logger.info("Provider '" + oAuth2Config.provider + "' will follow the '" + flowType + "' flow.");
            }
        }

        logger.fine("known providers are " + knownProviders);

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
        logger.finest("login state: " + loginState.encodePrettily());

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
                        logger.severe("user could not logout: " + context.user().principal().encodePrettily());
                        context.next();
                    }
                });
            } catch (Exception e) {
                logger.fine("cannot logout user: " + context.user().principal().encodePrettily());
            }
        }
        context.clearUser();
        Session session = context.session();
        if (session != null) {
            session.regenerateId();
        }
        new JsonResponse(context).end(true);
    }

    private void getOAuth2Providers(RoutingContext context) {
        JsonArray providers = new JsonArray();
        for (config.OAuth2Config providerConfig : config.oauth2.configs) {
            providers.add(new JsonObject().put("provider", providerConfig.provider).put("logoUrl", providerConfig.logoUrl));
        }
        new JsonResponse(context).status(HttpResponseStatus.OK).end(providers);
    }

    private void getUserIsAuthenticated(RoutingContext context) {
        new JsonResponse(context).end(context.user() != null);
    }

    private void getUser(RoutingContext context) {
        User user = context.user();
        if (user != null) {
            if (user.principal().getJsonObject("account") == null) {
                context.clearUser();
                user = null;
            } else {
                new JsonResponse(context).end(context.user().principal());
            }
        }
        if (user == null) {
            new JsonResponse(context).end(null);
        }
        // {isAuthorized(context, config.role.cpd.access.admin, isAdmin -> {
        //     if (isAdmin.succeeded()) {
        //         if (isAdmin.result()) {
        //             JSON_OBJECT_RESPONSE_END(context, user.put("isAdmin", true), HttpResponseStatus.OK);
        //         } else
        //             isAuthorized(context, config.role.cpd.access.civilServant, isCivilServant -> {
        //                 if (isCivilServant.succeeded()) {
        //                     if (isCivilServant.result()) {
        //                         JSON_OBJECT_RESPONSE_END(context, user.put("isCivilServant", true), HttpResponseStatus.OK);
        //                     } else {
        //                         JSON_OBJECT_RESPONSE_END(context, user.put("isCitizen", true), HttpResponseStatus.OK);
        //                     }
        //                 } else
        //                     throw new ResponseError(context, isCivilServant.cause());
        //             });
        //     } else
        //         throw new ResponseError(context, isAdmin.cause());
        // })};
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
