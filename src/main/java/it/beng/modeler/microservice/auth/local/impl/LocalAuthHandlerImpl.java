package it.beng.modeler.microservice.auth.local.impl;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import it.beng.modeler.config;
import it.beng.modeler.microservice.auth.local.LocalAuthHandler;
import it.beng.modeler.microservice.subroute.AuthSubRoute;

import java.util.logging.Logger;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class LocalAuthHandlerImpl extends AuthHandlerImpl implements LocalAuthHandler {

    private static Logger logger = Logger.getLogger(LocalAuthHandlerImpl.class.getName());

    public LocalAuthHandlerImpl(AuthProvider authProvider) {
        super(authProvider);
    }

    @Override
    public void handle(RoutingContext rc) {

        User user = rc.user();
        if (user != null) {
            this.authorise(user, rc);
        } else {
            JsonObject state = AuthSubRoute.getState(rc);
            authProvider.authenticate(state.getJsonObject("authInfo"), rh -> {
                if (rh.succeeded()) {
                    LocalUser authenticated = (LocalUser) rh.result();
                    logger.finest(Json.encodePrettily(authenticated.principal()));
                    rc.setUser(authenticated);
                    this.authorise(authenticated, rc);
                } else {
                    state.put("errorMsg", rh.cause().getMessage());
                    String encodedState = AuthSubRoute.getBase64EncodedState(rc);
                    rc.response()
                      .setStatusCode(302)
                      .putHeader("Location", config.server.baseHref + config.app.path +
                          config.locale(rc) + "/login/" + encodedState)
                      .end();
                }
            });
        }
    }

}
