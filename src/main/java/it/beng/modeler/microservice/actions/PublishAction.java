package it.beng.modeler.microservice.actions;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public abstract class PublishAction extends IncomingAction {
    public PublishAction(JsonObject action) {
        super(action);
    }

    @Override
    public void handle(RoutingContext context, Handler<AsyncResult<JsonObject>> handler) {
        handler.handle(Future.succeededFuture(json));
    }
}
