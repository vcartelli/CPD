package it.beng.modeler.microservice.auth.local.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import it.beng.modeler.microservice.auth.local.LocalAuthProvider;
import it.beng.modeler.model.semantic.organization.UserProfile;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class LocalAuthProviderImpl implements LocalAuthProvider {

    private final Vertx vertx;

    public LocalAuthProviderImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {

        System.out.println("authenticating " + authInfo.encodePrettily());

        String username = authInfo.getString("username");
        if (username == null) {
            resultHandler.handle(Future.failedFuture("authInfo must contain username in 'username' field"));
            return;
        }

        String password = authInfo.getString("password");
        if (password == null) {
            resultHandler.handle(Future.failedFuture("authInfo must contain password (md5 encoded) in 'password' field"));
            return;
        }

        UserProfile profile = UserProfile.get(username);
        if (profile != null && password.equals(profile.password)) {
            System.out.println(Json.encodePrettily(profile));
            resultHandler.handle(Future.succeededFuture(new LocalUser(username, this)));
        } else {
            resultHandler.handle(Future.failedFuture("Invalid username/password"));
        }

    }

}
