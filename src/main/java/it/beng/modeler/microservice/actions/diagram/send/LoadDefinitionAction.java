package it.beng.modeler.microservice.actions.diagram.send;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.db.MongoDB;
import it.beng.modeler.config;
import it.beng.modeler.microservice.actions.SendAction;
import it.beng.modeler.microservice.actions.diagram.DiagramAction;
import it.beng.modeler.microservice.actions.diagram.reply.DefinitionLoadedAction;
import it.beng.modeler.microservice.utils.JsonUtils;
import it.beng.modeler.microservice.utils.QueryUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class LoadDefinitionAction extends SendAction implements DiagramAction {
    public static final String TYPE = "[Diagram Action Send] Load Definition";

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
            new HashMap<String, String>() {{
                put("diagramId", diagramId());
            }}
        );
        mongodb.runCommand("aggregate", command, getDiagramDefinition -> {
            if (getDiagramDefinition.succeeded()) {
                JsonObject definition = JsonUtils.firstOrNull(getDiagramDefinition.result().getJsonArray("result"));
                if (definition != null) {
                    List<JsonObject> tasks = account == null
                        ? Collections.emptyList()
                        : config.processEngine().getTaskService().createTaskQuery()
                                .processInstanceBusinessKey(diagramId())
                                .active()
                                // take all active tasks, the filtering is done client side
//                                .taskAssignee(account.getString("id"))
                                .list().stream()
                                .map(task -> {
                                    // TODO: create task POJO
                                    JsonObject model = new JsonObject(task.getFormKey());
                                    return new JsonObject()
                                        .put("id", task.getId())
                                        .put("processId", task.getProcessInstanceId())
                                        .put("name", task.getName())
                                        .put("documentation", task.getDescription())
                                        .put("assignee", task.getAssignee())
                                        .put("createTime", QueryUtils.mongoDateTime(
                                            QueryUtils.parseDateTime(task.getCreateTime().toInstant().toString())
                                        ))
                                        .put("model", model)
                                        // TODO: create Collaboration.Process and Collaboration.Process.Task schemas
                                        .put("$domain", "Model.Thing");
                                })
                                .collect(Collectors.toList());
                    reply(new DefinitionLoadedAction(definition.put("tasks", new JsonArray(tasks))), handler);
                } else
                    handler.handle(Future.failedFuture("definition not found"));
            } else {
                handler.handle(Future.failedFuture(getDiagramDefinition.cause()));
            }
        });
    }

}
