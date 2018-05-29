package it.beng.modeler.microservice.actions.diagram.publish;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.modeler.config;
import it.beng.modeler.microservice.actions.diagram.DiagramAction;
import it.beng.modeler.microservice.actions.diagram.DiagramPublishAction;

import java.util.Collections;

public class UpdateThingsAction extends DiagramPublishAction {
    public static final String TYPE = "[Diagram Action Publish] Update Things";

    public UpdateThingsAction(JsonObject action) {
        super(action);
    }

    @Override
    protected String innerType() {
        return TYPE;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && updates() != null;
    }

    public JsonArray updates() {
        return json.getJsonArray("updates");
    }

    @Override
    public void handle(JsonObject account, Handler<AsyncResult<JsonObject>> handler) {
        DiagramAction.isAuthorized(account, diagramId(), Collections.singletonList("editor"), isAuthorized -> {
            if (isAuthorized.succeeded()) {
                if (isAuthorized.result()) {
                    this.updates().stream()
                        .filter(item -> item instanceof JsonObject)
                        .map(item -> (JsonObject) item)
                        .forEach(item -> {
                            JsonObject original = item.getJsonObject("original");
                            JsonObject changes = item.getJsonObject("changes");
                            mongodb.findOneAndUpdate(
                                config.DOMAIN_COLLECTIONS.get(original.getString("$domain")),
                                new JsonObject().put("id", original.getString("id")),
                                new JsonObject().put("$set", changes),
                                update -> {
                                    if (update.succeeded()) {
                                        handler.handle(Future.succeededFuture(json));
                                    } else {
                                        handler.handle(Future.failedFuture(update.cause()));
                                    }
                                });
                        });
                } else {
                    handler.handle(Future.failedFuture("unauthorized"));
                }
            } else {
                handler.handle(Future.failedFuture(isAuthorized.cause()));
            }
        });
    }
}
