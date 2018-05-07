package it.beng.modeler.microservice.subroute.auth;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.UserSessionHandler;
import it.beng.microservice.common.ServerError;
import it.beng.modeler.config;
import it.beng.modeler.microservice.auth.local.LocalAuthProvider;
import it.beng.modeler.microservice.http.JsonResponse;
import it.beng.modeler.microservice.subroute.VoidSubRoute;

import java.util.logging.Logger;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class LocalAuthSubRoute extends VoidSubRoute {

    private static Logger logger = Logger.getLogger(LocalAuthSubRoute.class.getName());

    public static final String PROVIDER = "local";

    public LocalAuthSubRoute(Vertx vertx, Router router) {
        super(config.server.auth.path + PROVIDER + "/", vertx, router, false);
    }

    @Override
    protected void init() {

        /** LOCAL AUTHENTICATION **/

        // configure local auth provider
        final LocalAuthProvider localAuthProvider = LocalAuthProvider.create(vertx);

        // create local auth user session handler
        router.route().handler(UserSessionHandler.create(localAuthProvider));

        // create local user login handler
        router.route(HttpMethod.POST, path + "login/handler").handler(context -> {
            final JsonObject authInfo = context.getBodyAsJson();
            if (authInfo != null) {
                localAuthProvider.authenticate(authInfo, result -> {
                    if (result.succeeded()) {
                        User user = result.result();
                        JsonObject loginState = context.get("loginState");
                        user.principal().put("loginState", loginState);
                        context.setUser(user);
                        Session session = context.session();
                        if (session != null) {
                            // the user has upgraded from unauthenticated to authenticated
                            // session should be upgraded as recommended by owasp
                            session.regenerateId();
                        }
                        logger.finest("local user principal: " + context.user().principal().encodePrettily());
                        // return the user
                        new JsonResponse(context).end(context.user().principal());
                    } else {
                        context.fail(result.cause());
                    }
                });
            } else
                context.fail(ServerError.message("no authInfo body in login post"));
        });
    }

}
