package it.beng.modeler.microservice.auth.local.impl;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import it.beng.modeler.config;
import it.beng.modeler.microservice.auth.local.LocalAuthHandler;

import java.util.Base64;

import static it.beng.modeler.microservice.subroute.SubRoute.getQueryParameter;
import static it.beng.modeler.microservice.subroute.SubRoute.redirect;

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

        JsonObject state = new JsonObject(new String(Base64.getDecoder().decode(
            getQueryParameter(rc.request().query(), "state"))));
        User user = rc.user();
        if (user != null) {
            this.authorise(user, rc);
            redirect(rc, state.getString("redirect"));
        } else {
            authProvider.authenticate(state.getJsonObject("authInfo"), rh -> {
                if (rh.succeeded()) {
                    LocalUser authenticated = (LocalUser) rh.result();
                    if (config.develop) System.out.println(Json.encodePrettily(authenticated.principal()));
                    rc.setUser(authenticated);
                    this.authorise(authenticated, rc);
                    redirect(rc, state.getString("redirect"));
                } else {
                    String redirect = Base64.getEncoder().encodeToString(state.getString("redirect").getBytes());
                    redirect(rc, config.server.baseHref + "login/" + redirect);
                }
            });
        }
    }

}
