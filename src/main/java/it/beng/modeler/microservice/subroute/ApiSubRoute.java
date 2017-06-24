package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import it.beng.microservice.db.MongoDB;
import it.beng.microservice.schema.SchemaTools;
import it.beng.modeler.config;
import it.beng.modeler.microservice.ResponseError;
import it.beng.modeler.model.ModelTools;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 * @author vince
 */
public final class ApiSubRoute extends VoidSubRoute {

    public ApiSubRoute(Vertx vertx, Router router, MongoDB mongodb, SchemaTools schemaTools, ModelTools modelTools) {
        super(config.server.api.path, vertx, router, mongodb, schemaTools, modelTools);
    }

    @Override
    protected void init() {

        /* SIMPATICO public API */

        // stats
        router.route(HttpMethod.GET, path + "stats/diagram/:id/eServiceCount")
              .handler(this::getDiagramEServiceCount);
        router.route(HttpMethod.GET, path + "stats/diagram/:id/userFeedbackCount")
              .handler(this::getDiagramUserFeedbackCount);

        // summary
        router.route(HttpMethod.GET, path + "diagram/summary/list")
              .handler(this::getDiagramSummaryList);
        router.route(HttpMethod.GET, path + "diagram/eService/:eServiceId/summary")
              .handler(this::getDiagramEServiceSummary);

        // feedback
        router.route(HttpMethod.GET, path + "user/feedback/:fromDateTime")
              .handler(this::getUserFeedback);
        router.route(HttpMethod.GET, path + "user/feedback/:fromDateTime/:toDateTime")
              .handler(this::getUserFeedback);
        router.route(HttpMethod.POST, path + "user/feedback")
              .handler(this::postUserFeedback);

        /* CPD private API */

        // diagram
        router.route(HttpMethod.GET, path + "diagram/:id")
              .handler(this::getDiagramElement);
        router.route(HttpMethod.PUT, path + "diagram")
              .handler(this::putDiagramElement);
        router.route(HttpMethod.DELETE, path + "diagram/:id")
              .handler(this::delDiagramElement);

        // model
        router.route(HttpMethod.GET, path + "model/:id")
              .handler(this::getModelElement);
        router.route(HttpMethod.PUT, path + "model")
              .handler(this::putModelElement);
        router.route(HttpMethod.DELETE, path + "model/:id")
              .handler(this::delModelElement);

        // lists
        router.route(HttpMethod.GET, path + "diagram/:rootId/elements")
              .handler(this::getDiagramElements);
        router.route(HttpMethod.GET, path + "diagram/:rootId/models")
              .handler(this::getDiagramModels);

        // data
        router.route(HttpMethod.GET, path + "data/stencilSetDefinition/:notation")
              .handler(this::getStencilSetDefinition);

        /* STATIC RESOURCES (swagger-ui) */

        // IMPORTANT!!1: redirect api to api/
        // it MUST be done with regex (i.e. must be exactly "api") to avoid infinite redirections
        router.routeWithRegex(HttpMethod.GET, "^" + Pattern.quote(path.substring(0, path.length() - 1)) + "$")
              .handler(rc -> redirect(rc, path));
        router.route(HttpMethod.GET, path + "*").handler(StaticHandler.create("web/swagger-ui"));
    }

    /* PUBLIC API */

    private void getDiagramEServiceCount(RoutingContext rc) {
        simLagTime();
        String id = rc.request().getParam("id");
        MongoDB.Command command = mongodb.command(
            "getDiagramEServiceCount",
            new HashMap<String, String>() {{
                put("id", id);
            }}
        );
        mongodb.runCommand("aggregate", command, ModelTools.JSON_ENTITY_TO_MONGO_DB, ar -> {
            if (ar.succeeded()) {
                JsonArray result = ar.result().getJsonArray("result");
                if (result.size() > 0)
                    JSON_OBJECT_RESPONSE_END(rc, result.getJsonObject(0));
                else
                    JSON_NULL_RESPONSE(rc);
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void getDiagramUserFeedbackCount(RoutingContext rc) {
        simLagTime();
        String id = rc.request().getParam("id");
        JsonObject query = new JsonObject()
            .put("$or", new JsonArray()
                .add(new JsonObject().put("rootId", id))
                .add(new JsonObject().put("elementId", id)));
        mongodb.count("user.feedback", query, ModelTools.JSON_ENTITY_TO_MONGO_DB, count -> {
            if (count.succeeded()) {
                JSON_OBJECT_RESPONSE_END(rc, new JsonObject()
                    .put("diagramId", id)
                    .put("count", count.result())
                );
            } else {
                JSON_NULL_RESPONSE(rc);
            }
        });
    }

    private void getDiagramSummaryList(RoutingContext rc) {
        simLagTime();
        MongoDB.Command command = mongodb.command(
            "getDiagramSummaryList",
            new HashMap<String, String>() {{
                put("appDiagramUrl", config.server.pub.appHref(rc) + config.app.diagramPath);
                put("appDiagramSvg", config.server.pub.assetsHref() + "svg/");
            }}
        );
        mongodb.runCommand("aggregate", command, ModelTools.JSON_ENTITY_TO_MONGO_DB, ar -> {
            if (ar.succeeded()) {
                JSON_ARRAY_RESPONSE_END(rc, ar.result().getJsonArray("result"));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void getDiagramEServiceSummary(RoutingContext rc) {
        simLagTime();
        String eServiceId = rc.request().getParam("eServiceId");
        MongoDB.Command command = mongodb.command(
            "getDiagramEServiceSummary",
            new HashMap<String, String>() {{
                put("eServiceId", eServiceId);
                put("appDiagramUrl", config.server.pub.appHref(rc) + config.app.diagramPath);
                put("appDiagramSvg", config.server.pub.assetsHref() + "svg/");
            }}
        );
        mongodb.runCommand("aggregate", command, ModelTools.JSON_ENTITY_TO_MONGO_DB, ar -> {
            if (ar.succeeded()) {
                if (config.develop)
                    JSON_ARRAY_RESPONSE_END(rc, ar.result().getJsonArray("result"));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private static JsonObject mongoDateTimeRange(OffsetDateTime from, OffsetDateTime to) {
        JsonObject range = new JsonObject();
        if (from != null)
            range.put("$gte", mongoDateTime(from));
        if (to != null)
            range.put("$lt", mongoDateTime(to));
        return range;
    }

    private void getUserFeedback(RoutingContext rc) {
        OffsetDateTime fromDateTime = parseDateTime(rc.request().getParam("fromDateTime"));
        if (fromDateTime == null) {
            throw new ResponseError(rc, "cannot parse date-time from '" + rc.request().getParam("fromDateTime") + "'");
        }
        OffsetDateTime toDateTime = parseDateTime(rc.request().getParam("toDateTime"));
        JsonObject dateTimeRange = mongoDateTimeRange(fromDateTime, toDateTime);
        MongoDB.Command command = mongodb.command(
            "getUserFeedback",
            new HashMap<String, String>() {{
                put("dateTimeRange", dateTimeRange.encode());
                put("appDiagramUrl", config.server.pub.appHref(rc) + config.app.diagramPath);
                put("appDiagramSvg", config.server.pub.assetsHref() + "svg/");
            }}
        );
        mongodb.runCommand("aggregate", command, ModelTools.JSON_ENTITY_TO_MONGO_DB, ar -> {
            if (ar.succeeded()) {
                JSON_OBJECT_RESPONSE_END(rc, new JsonObject()
                    .put("dateTimeRange", dateTimeRange)
                    .put("feedbackList", ar.result().getJsonArray("result")));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void postUserFeedback(RoutingContext rc) {
        User user = rc.user();
        if (user == null)
            throw new ResponseError(rc, "user is not authenticated");
        JsonObject feedback = rc.getBodyAsJson();
        String message = feedback.getString("message");
        if (message == null || message.trim().length() == 0)
            throw new ResponseError(rc, "no message in feedback");
        String diagramId = feedback.getString("diagramId");
        if (diagramId == null || diagramId.trim().length() == 0)
            throw new ResponseError(rc, "no diagram ID in feedback");
        String elementId = feedback.getString("elementId");
        if (elementId == null || elementId.trim().length() == 0)
            throw new ResponseError(rc, "no element ID in feedback");
        feedback
            .put("id", UUID.randomUUID().toString())
            .put("userId", user.principal().getJsonObject("profile").getString("id"))
            .put("dateTime", mongoDateTime(OffsetDateTime.now()));
//        schemaTools.validate();

        mongodb.save("user.feedback", feedback, ModelTools.JSON_ENTITY_TO_MONGO_DB, save -> {
            if (save.succeeded()) {
//                JSON_OBJECT_RESPONSE_END(rc, new SaveResult<>(validate...));
                JSON_OBJECT_RESPONSE_END(rc, save.result());
            } else {
                throw new ResponseError(rc, save.cause());
            }
        });
    }

    /* PRIVATE API */

    private void getDiagramElement(RoutingContext rc) {
        simLagTime();
        String id = rc.request().getParam("id");
        modelTools.getDiagramEntity(id, getDiagram -> {
            if (getDiagram.succeeded()) {
                JSON_OBJECT_RESPONSE_END(rc, getDiagram.result());
            } else {
                throw new ResponseError(rc, getDiagram.cause());
            }
        });
    }

    private void putDiagramElement(RoutingContext rc) {
        simLagTime();
        JsonObject entity = rc.getBodyAsJson();
        isDiagramEditor(rc, entity, isDiagramEditor -> {
            if (isDiagramEditor.succeeded()) {
                modelTools.saveDiagramEntity(entity, saveDiagramEntity -> {
                    if (saveDiagramEntity.succeeded()) {
                        JSON_OBJECT_RESPONSE_END(rc, saveDiagramEntity.result().toJson());
                    } else {
                        throw new ResponseError(rc, saveDiagramEntity.cause());
                    }
                });
            } else {
                throw new ResponseError(rc, isDiagramEditor.cause());
            }
        });
    }

    private void delDiagramElement(RoutingContext rc) {
        simLagTime();
        String id = rc.request().getParam("id");
        isDiagramEditor(rc, id, isDiagramEditor -> {
            if (isDiagramEditor.succeeded()) {
                modelTools.deleteDiagramEntity(id, deleteDiagramEntity -> {
                    if (deleteDiagramEntity.succeeded())
                        JSON_OBJECT_RESPONSE_END(rc, deleteDiagramEntity.result().toJson());
                    else
                        throw new ResponseError(rc, deleteDiagramEntity.cause());
                });
            } else {
                throw new ResponseError(rc, isDiagramEditor.cause());
            }
        });
    }

    private void getModelElement(RoutingContext rc) {
        simLagTime();
        String id = rc.request().getParam("id");
        modelTools.getModelEntity(id, getModelEntity -> {
            if (getModelEntity.succeeded()) {
                JSON_OBJECT_RESPONSE_END(rc, getModelEntity.result());
            } else {
                JSON_NULL_RESPONSE(rc);
            }
        });
    }

    private void putModelElement(RoutingContext rc) {
        simLagTime();
        JsonObject entity = rc.getBodyAsJson();
        isModelEditor(rc, entity, isModelEditor -> {
            if (isModelEditor.succeeded()) {
                modelTools.saveModelEntity(entity, saveModelEntity -> {
                    if (saveModelEntity.succeeded()) {
                        JSON_OBJECT_RESPONSE_END(rc, saveModelEntity.result().toJson());
                    } else {
                        throw new ResponseError(rc, saveModelEntity.cause());
                    }
                });
            } else {
                throw new ResponseError(rc, isModelEditor.cause());
            }
        });
    }

    private void delModelElement(RoutingContext rc) {
        simLagTime();
        String id = rc.request().getParam("id");
        isDiagramEditor(rc, id, isDiagramEditor -> {
            if (isDiagramEditor.succeeded()) {
                modelTools.deleteModelEntity(id, deleteModelEntity -> {
                    if (deleteModelEntity.succeeded())
                        JSON_OBJECT_RESPONSE_END(rc, deleteModelEntity.result().toJson());
                    else
                        throw new ResponseError(rc, deleteModelEntity.cause());
                });
            } else {
                throw new ResponseError(rc, isDiagramEditor.cause());
            }
        });
    }

    private void getDiagramElements(RoutingContext rc) {
        simLagTime();
        String rootId = rc.request().getParam("rootId");
        modelTools.getDiagramElements(rootId, getDiagramElements -> {
            if (getDiagramElements.succeeded()) {
                JSON_ARRAY_RESPONSE_END(rc, getDiagramElements.result());
            } else {
                throw new ResponseError(rc, getDiagramElements.cause());
            }
        });
    }

    private void getDiagramModels(RoutingContext rc) {
        simLagTime();
        String rootId = rc.request().getParam("rootId");
        modelTools.getDiagramModels(rootId, getDiagramModels -> {
            if (getDiagramModels.succeeded()) {
                JSON_ARRAY_RESPONSE_END(rc, getDiagramModels.result());
            } else {
                throw new ResponseError(rc, getDiagramModels.cause());
            }
        });
    }

    private void getSemanticList(RoutingContext rc) {
        simLagTime();
        mongodb.find("semantic.entity", new JsonObject(), ModelTools.JSON_ENTITY_TO_MONGO_DB, ar -> {
            if (ar.succeeded()) {
                JSON_ARRAY_RESPONSE_END(rc, ar.result());
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void getSemanticListByType(RoutingContext rc) {
        simLagTime();
        String clazz = rc.request().getParam("class");
        mongodb
            .find("semantic.entity", new JsonObject().put("class", clazz), ModelTools.JSON_ENTITY_TO_MONGO_DB, ar -> {
                if (ar.succeeded()) {
                    JSON_ARRAY_RESPONSE_END(rc, ar.result());
                } else {
                    throw new ResponseError(rc, ar.cause());
                }
            });
    }

    private void getStencilSetDefinition(RoutingContext rc) {
        final String notation = rc.request().getParam("notation");
        vertx.fileSystem()
             .readFile(config.DATA_PATH + notation.replace(".", "/") + ".json", ar -> {
                 if (ar.succeeded()) {
                     JSON_OBJECT_RESPONSE_END(rc, ar.result().toJsonObject());
                 } else {
                     throw new ResponseError(rc, ar.cause());
                 }
             });
    }

}
