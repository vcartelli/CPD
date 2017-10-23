package it.beng.modeler.microservice.subroute.auth;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.UserSessionHandler;
import it.beng.microservice.db.MongoDB;
import it.beng.microservice.schema.SchemaTools;
import it.beng.modeler.config;
import it.beng.modeler.microservice.ResponseError;
import it.beng.modeler.microservice.auth.local.LocalAuthProvider;
import it.beng.modeler.microservice.subroute.AuthSubRoute;
import it.beng.modeler.microservice.subroute.VoidSubRoute;
import it.beng.modeler.model.ModelTools;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class LocalAuthSubRoute extends VoidSubRoute {

    public LocalAuthSubRoute(Vertx vertx, Router router, MongoDB mongodb,
                             SchemaTools schemaTools, ModelTools modelTools) {
        super(config.server.auth.path + "local/", vertx, router, mongodb, schemaTools, modelTools);
    }

    @Override
    protected void init() {

        /** LOCAL AUTHENTICATION **/

        // configure local auth provider
        final LocalAuthProvider localAuthProvider = LocalAuthProvider.create(vertx);

        // create local auth user session handler
        router.route().handler(UserSessionHandler.create(localAuthProvider));

        // create local user login handler
        router.route(HttpMethod.GET, path + "login/handler").handler(rc -> {
            JsonObject state = AuthSubRoute.getState(rc);
            if (state != null) {
                JsonObject authInfo = state.getJsonObject("authInfo");
                if (authInfo == null) {
                    rc.fail(new ResponseError(rc, "no authInfo supplied to login state"));
                } else { // rc.next();
                    localAuthProvider.authenticate(authInfo, result -> {
                        if (result.succeeded()) {
                            rc.setUser(result.result());
                        } else {
                            rc.fail(result.cause());
                        }
                        rc.next();
                    });
                }
            } else rc.next();
        });
//        router.route(HttpMethod.GET, path + "login/handler").handler(LocalAuthHandler.create(localAuthProvider));
    }

}
