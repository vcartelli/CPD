package it.beng.modeler.microservice.actions.diagram.publish;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.modeler.microservice.utils.ProcessEngineUtils;
import it.beng.modeler.model.Domain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class UpdateThingsAction extends AuthorizedAction {
    private static final Logger logger = LogManager.getLogger(UpdateThingsAction.class);

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
    protected void forEach(JsonObject update, Handler<AsyncResult<Void>> handler) {
        JsonObject changes = update.getJsonObject("changes");
        JsonObject replace = update.getJsonObject("original").mergeIn(changes, true);
        Domain domain = Domain.get(replace.getString("$domain"));
        mongodb.findOneAndReplace(
            domain.getCollection(),
            new JsonObject().put("id", replace.getString("id")),
            replace,
            findOneAndReplace -> {
                if (findOneAndReplace.succeeded()) {
                    if (domain == Domain.ofDefinition(Domain.Definition.DIAGRAM)) {
                        ProcessEngineUtils.update(update, updated -> {
                            if (updated.succeeded())
                                handler.handle(Future.succeededFuture());
                            else
                                handler.handle(Future.failedFuture(updated.cause()));
                        });
                    }
                } else handler.handle(Future.failedFuture(findOneAndReplace.cause()));
            });
    }

    public JsonArray updates() {
        return json.getJsonArray("updates");
    }

}
