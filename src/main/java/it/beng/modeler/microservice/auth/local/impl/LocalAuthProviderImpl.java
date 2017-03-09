package it.beng.modeler.microservice.auth.local.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import it.beng.modeler.microservice.auth.local.LocalAuthProvider;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class LocalAuthProviderImpl implements LocalAuthProvider {

    private final Vertx vertx;
    private final MongoClient mongodb;

    public LocalAuthProviderImpl(Vertx vertx) {
        this.vertx = vertx;
        this.mongodb = vertx.getOrCreateContext().get("mongodb");
        if (mongodb == null)
            throw new IllegalStateException("could not find mongodb in current context");
    }

    @Override
    public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {

        System.out.println("authenticating " + authInfo.encodePrettily());

        String username = authInfo.getString("username");
        if (username == null) {
            resultHandler.handle(Future.failedFuture("authInfo must contain username in 'username' field"));
            return;
        }

        String password = authInfo.getString("keyStorePassword");
        if (password == null) {
            resultHandler
                .handle(Future.failedFuture("authInfo must contain keyStorePassword (md5 encoded) in 'keyStorePassword' field"));
            return;
        }

        mongodb.findOne("users", new JsonObject().put("_id", username), new JsonObject(), ar -> {
            if (ar.succeeded()) {
                JsonObject profile = ar.result();
                if (profile != null && password.equals(profile.getString("keyStorePassword"))) {
                    JsonObject principal = new JsonObject()
                        .put("provider", "local")
                        .put("username", username)
                        .put("profile", profile);
                    resultHandler.handle(Future.succeededFuture(new LocalUser(principal, this)));
                } else {
                    resultHandler.handle(Future.failedFuture("Invalid username/keyStorePassword"));
                }
            } else {
                resultHandler.handle(Future.failedFuture("Invalid username/keyStorePassword"));
            }
        });

    }

}
