package it.beng.modeler.microservice.actions;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public abstract class PublishAction extends IncomingAction {
    public PublishAction(JsonObject action) {
        super(action);
    }

    @Override
    public void handle(JsonObject account, Handler<AsyncResult<JsonObject>> handler) {
        handler.handle(Future.succeededFuture(json));
    }
}
