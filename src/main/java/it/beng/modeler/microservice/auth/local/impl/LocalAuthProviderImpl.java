package it.beng.modeler.microservice.auth.local.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import it.beng.microservice.db.MongoDB;
import it.beng.modeler.config;
import it.beng.modeler.microservice.auth.local.LocalAuthProvider;
import it.beng.modeler.model.ModelTools;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class LocalAuthProviderImpl implements LocalAuthProvider {

    private final Vertx vertx;
    private final MongoDB mongodb;

    public LocalAuthProviderImpl(Vertx vertx) {
        this.vertx = vertx;
        this.mongodb = vertx.getOrCreateContext().get("mongodb");
        if (mongodb == null)
            throw new IllegalStateException("could not find mongodb in current context");
    }

    @Override
    public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {

        if (authInfo == null) {
            resultHandler.handle(Future.failedFuture("no authInfo provided"));
            return;
        }

        if (config.develop) System.out.println("authenticating " + authInfo.encodePrettily());

        String username = authInfo.getString("username");
        if (username == null) {
            resultHandler.handle(Future.failedFuture("authInfo must contain username in 'username' field"));
            return;
        }

        String password = authInfo.getString("password");
        if (password == null) {
            resultHandler.handle(Future.failedFuture("authInfo must contain password in 'password' field"));
            return;
        }

        mongodb.findOne("user", new JsonObject()
            .put("id", username), new JsonObject(), ModelTools.JSON_ENTITY_TO_MONGO_DB, ar -> {
            if (ar.succeeded()) {
                JsonObject user = ar.result();
                if (user != null && password.equals(user.getString("password"))) {
                    user.getJsonObject("profile").put("provider", "local");
                    resultHandler.handle(Future.succeededFuture(new LocalUser(user, this)));
                } else {
                    resultHandler.handle(Future.failedFuture("Invalid username/password"));
                }
            } else {
                resultHandler.handle(Future.failedFuture("Invalid username/password"));
            }
        });

    }

}
