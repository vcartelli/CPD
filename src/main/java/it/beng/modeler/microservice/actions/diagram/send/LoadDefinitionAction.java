package it.beng.modeler.microservice.actions.diagram.send;

import java.util.HashMap;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.db.MongoDB;
import it.beng.modeler.microservice.actions.SendAction;
import it.beng.modeler.microservice.actions.diagram.DiagramAction;
import it.beng.modeler.microservice.actions.diagram.reply.DefinitionLoadedAction;

public class LoadDefinitionAction extends SendAction implements DiagramAction {
    public static final String TYPE = "[Diagram Action] Load Definition";

    public LoadDefinitionAction(JsonObject action) {
        super(action);
    }

    @Override
    protected String innerType() {
        return TYPE;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && diagramId() != null;
    }

    public String diagramId() {
        return json.getString("diagramId");
    }

    @Override
    public void handle(JsonObject account, Handler<AsyncResult<JsonObject>> handler) {
        MongoDB.Command command = mongodb.command(COMMAND_PATH + "getDiagramDefinition",
            new HashMap<String, String>() {
                private static final long serialVersionUID = 1L;
                {
                    put("diagramId", diagramId());
                }
            });
        mongodb.runCommand("aggregate", command, getDiagramDefinition -> {
            if (getDiagramDefinition.succeeded()) {
                JsonArray result = getDiagramDefinition.result().getJsonArray("result");
                JsonObject definition = result.size() == 0
                        ? null
                        : getDiagramDefinition.result().getJsonArray("result").getJsonObject(0);
                reply(new DefinitionLoadedAction(definition), handler);
            } else {
                handler.handle(Future.failedFuture(getDiagramDefinition.cause()));
            }
        });
    }

}
