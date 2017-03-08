package it.beng.modeler.microservice.auth.local.impl;

import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import it.beng.modeler.microservice.auth.local.LocalAuthHandler;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class LocalAuthHandlerImpl extends AuthHandlerImpl implements LocalAuthHandler {

    public LocalAuthHandlerImpl(AuthProvider authProvider) {
        super(authProvider);
    }

    @Override
    public void handle(RoutingContext rc) {
        User user = rc.user();
        if (user != null)
            rc.next();
//            this.authorise(user, rc);
        else
            rc.response().putHeader("Location", "/login").setStatusCode(302).end();
    }

}
