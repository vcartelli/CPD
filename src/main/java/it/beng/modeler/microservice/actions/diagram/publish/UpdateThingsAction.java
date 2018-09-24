package it.beng.modeler.microservice.actions.diagram.publish;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.modeler.model.Domain;

import java.util.List;
import java.util.stream.Collectors;

public class UpdateThingsAction extends EditorAction {
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

    @Override
    protected List<JsonObject> items() {
        return this.updates().stream()
                   .filter(item -> item instanceof JsonObject)
                   .map(item -> (JsonObject) item)
                   .collect(Collectors.toList());
    }

    @Override
    protected void forEach(JsonObject item, Handler<AsyncResult<Void>> handler) {
        JsonObject original = item.getJsonObject("original");
        JsonObject changes = item.getJsonObject("changes");
        mongodb.findOneAndUpdate(
            Domain.get(original.getString("$domain")).getCollection(),
            new JsonObject().put("id", original.getString("id")),
            new JsonObject().put("$set", changes), update -> {
                if (update.failed()) {
                    handler.handle(Future.failedFuture(update.cause()));
                } else {
                    handler.handle(Future.succeededFuture());
                }
            });
    }

    public JsonArray updates() {
        return json.getJsonArray("updates");
    }
}
