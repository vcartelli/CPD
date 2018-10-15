package it.beng.modeler.microservice.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.modeler.config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class ProcessEngineUtils {
    private static final Log logger = LogFactory.getLog(ProcessEngineUtils.class);

    public static void startCollaboration(String diagramId, JsonObject team) {
        config.processEngine().getRuntimeService()
              .startProcessInstanceByKey(
                  config.PROCESS_DEFINITION_KEY,
                  diagramId,
                  new HashMap<String, Object>() {{
                      put("owner", team.getJsonArray("owner").getString(0));
                      put("reviewer", team.getJsonArray("reviewer").getString(0));
                      put("editor", team.getJsonArray("editor").getString(0));
                      if (team.getJsonArray("observer").size() > 0) {
                          put("observer", team.getJsonArray("observer").getString(0));
                      }
                  }}
              );
    }

    public static void update(JsonObject update) {
        final ProcessEngine processEngine = config.processEngine();
        final JsonObject original = update.getJsonObject("original");
        final JsonObject changes = update.getJsonObject("changes");
        switch (original.getString("$domain")) {
            case "Model.FPMN.Diagram":
                String businessKey = original.getString("id");
                JsonObject team = changes.getJsonObject("team");
                if (team != null) {
                    logger.debug("TEAM CHANGED: " + team.encodePrettily());
                    for (Map.Entry<String, Object> entry : team) {
                        final String role = entry.getKey();
                        final String userId = ((JsonArray) entry.getValue()).getString(0);

                        // (1) set active task assignee if changed (NOTE: assignee role is stored in task.formKey)
                        final TaskService taskService = processEngine.getTaskService();
                        taskService.createTaskQuery()
                                   .processInstanceBusinessKey(businessKey)
                                   .active()
                                   .list().forEach(
                            task -> {
                                // TODO: create task POJO
                                JsonObject model = new JsonObject(task.getFormKey());
                                if (role.equals(model.getString("position")))
                                    taskService.setAssignee(task.getId(), userId);
                            }
                        );

                        // (2) set changed roles as process variables
                        final RuntimeService runtimeService = processEngine.getRuntimeService();
                        runtimeService.createExecutionQuery()
                                      .processInstanceBusinessKey(businessKey)
                                      .list().forEach(
                            execution -> {
                                runtimeService.setVariable(execution.getId(), role, userId);
                            }
                        );
                    }
                }
                break;
        }
    }

    public static void deleteCollaboration(String id) {
        RuntimeService runtimeService = config.processEngine().getRuntimeService();
        runtimeService.createProcessInstanceQuery()
                      .processInstanceBusinessKey(id)
                      .list().forEach(
            processInstance -> {
                logger.debug("deleting process " + processInstance.getId() + "...");
                runtimeService.deleteProcessInstance(processInstance.getId(), "removed by admin");
                logger.debug("...process " + processInstance.getId() + " deleted!");
            }
        );
    }

    public static void completeTask(JsonObject task, String decision) {
        Objects.requireNonNull(task);
        String taskId = task.getString("id");
        Objects.requireNonNull(taskId);
        config.processEngine().getTaskService().complete(taskId, new HashMap<String, Object>() {{
            if (decision != null) put("decision", decision);
        }});
    }
}
