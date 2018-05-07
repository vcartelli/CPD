package it.beng.modeler.microservice.actions.diagram.send;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.db.MongoDB;
import it.beng.modeler.microservice.actions.SendAction;
import it.beng.modeler.microservice.actions.diagram.DiagramAction;
import it.beng.modeler.microservice.actions.diagram.reply.DefinitionLoadedAction;
import it.beng.modeler.microservice.utils.JsonUtils;

import java.util.HashMap;

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
                reply(new DefinitionLoadedAction(
                    JsonUtils.firstOrNull(getDiagramDefinition.result().getJsonArray("result"))
                ), handler);
            } else {
                handler.handle(Future.failedFuture(getDiagramDefinition.cause()));
            }
        });
    }

}
