package it.beng.modeler.microservice.subroute;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.hazelcast.com.eclipsesource.json.ParseException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import it.beng.microservice.common.ServerError;
import it.beng.microservice.db.MongoDB;
import it.beng.modeler.config;
import it.beng.modeler.config.Thing;
import it.beng.modeler.microservice.http.JsonResponse;
import it.beng.modeler.microservice.utils.AuthUtils;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class ApiSubRoute extends VoidSubRoute {

    Logger logger = Logger.getLogger(ApiSubRoute.class.getName());

    public ApiSubRoute(Vertx vertx, Router router) {
        super(config.server.api.path, vertx, router, false);
    }

    @Override
    protected void init() {

        /* SIMPATICO public API */

        // stats
        // deprecated ...
        router.route(HttpMethod.GET, path + "stats/diagram/:procedureId/eServiceCount")
            .handler(this::getProcedureEServiceCount);
        // ... replaced by
        router.route(HttpMethod.GET, path + "stats/procedure/:procedureId/eServiceCount")
            .handler(this::getProcedureEServiceCount);
        // deprecated ...
        router.route(HttpMethod.GET, path + "stats/diagram/:procedureId/userFeedbackCount")
            .handler(this::getProcedureUserFeedbackCount);
        // ... replaced by
        router.route(HttpMethod.GET, path + "stats/procedure/:procedureId/userFeedbackCount")
            .handler(this::getProcedureUserFeedbackCount);

        // summary
        // deprecated ...
        router.route(HttpMethod.GET, path + "diagram/summary/list").handler(this::getProcedureSummary);
        // ... replaced by
        router.route(HttpMethod.GET, path + "procedure/summary/list").handler(this::getProcedureSummary);
        router.route(HttpMethod.GET, path + "procedure/:procedureId/summary").handler(this::getProcedureSummary);
        // Deprecated ...
        router.route(HttpMethod.GET, path + "diagram/eService/:eServiceId/summary").handler(this::getProcedureSummary);
        // ... replaced by
        router.route(HttpMethod.GET, path + "procedure/eService/:eServiceId/summary").handler(this::getProcedureSummary);

        // feedback
        router.route(HttpMethod.GET, path + "user/feedback/:fromDateTime").handler(this::getUserFeedback);
        router.route(HttpMethod.GET, path + "user/feedback/:fromDateTime/:toDateTime").handler(this::getUserFeedback);
        router.route(HttpMethod.POST, path + "user/feedback").handler(this::postUserFeedback);

        /* CPD API */

        // diagram
        // TODO: this sould return an array of svg, use root ID for single svg diagram
        router.route(HttpMethod.GET, path + "model/diagram/:id.svg").handler(this::getModelDiagramSVG);
        router.route(HttpMethod.GET, path + "model/diagram/my").handler(this::getModelDiagramMyList);
        router.route(HttpMethod.GET, path + "model/diagram/search/:text")
            .handler(this::getModelDiagramSearchTextList);
        router.route(HttpMethod.GET, path + "model/diagram/newer/:limit").handler(this::getModelDiagramNewerList);

        // // diagram
        // router.route(HttpMethod.GET, path + "diagram/:id").handler(this::getDiagramElement);
        // router.route(HttpMethod.PUT, path + "diagram").handler(this::putDiagramElement);
        // router.route(HttpMethod.DELETE, path + "diagram/:id").handler(this::delDiagramElement);

        // // model
        // router.route(HttpMethod.GET, path + "model/:id").handler(this::getModelElement);
        // router.route(HttpMethod.PUT, path + "model").handler(this::putModelElement);
        // router.route(HttpMethod.DELETE, path + "model/:id").handler(this::delModelElement);

        // // lists
        // router.route(HttpMethod.GET, path + "diagram/:rootId/elements").handler(this::getDiagramElements);
        // router.route(HttpMethod.GET, path + "diagram/:rootId/models").handler(this::getDiagramModels);

        // // data
        // router.route(HttpMethod.GET, path + "data/stencilSetDefinition/:notation")
        //         .handler(this::getStencilSetDefinition);

        /* STATIC RESOURCES (swagger-ui) */

        // IMPORTANT!!1: (strange behaviour with swagger) redirect "api" to "api/"
        // it MUST be done with regex (i.e. must be exactly "api") to avoid infinite redirections
        router.routeWithRegex(HttpMethod.GET, "^" + Pattern.quote(path.substring(0, path.length() - 1)) + "$")
            .handler(context -> redirect(context, path));
        router.route(HttpMethod.GET, path + "*").handler(StaticHandler.create("web/swagger-ui"));
    }

    /* PUBLIC API */

    private void getProcedureEServiceCount(RoutingContext context) {
        String procedureId = context.pathParam("procedureId");
        MongoDB.Command command = mongodb.command("getProcedureEServiceCount", new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put("procedureId", procedureId);
            }
        });
        mongodb.runCommand("aggregate", command, ar -> {
            if (ar.succeeded()) {
                JsonResponse response = new JsonResponse(context);
                JsonArray result = ar.result().getJsonArray("result");
                if (result.size() > 0)
                    response.end(result.getJsonObject(0));
                else
                    response.end(new JsonObject().put("id", procedureId).put("count", 0));
            } else {
                context.fail(ar.cause());
            }
        });
    }

    private void getProcedureUserFeedbackCount(RoutingContext context) {
        String procedureId = context.pathParam("procedureId");
        MongoDB.Command command = mongodb.command("getProcedureUserFeedbackCount", new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put("procedureId", procedureId != null ? "\"procedure._id\":\"" + procedureId + "\"," : "");
            }
        });
        mongodb.runCommand("aggregate", command, ar -> {
            if (ar.succeeded()) {
                JsonResponse response = new JsonResponse(context);
                JsonArray result = ar.result().getJsonArray("result");
                if (result.size() > 0) {
                    if (procedureId != null)
                        response.end(result.getJsonObject(0));
                    else
                        response.end(result);
                }
                else
                    response.end(new JsonObject().put("id", procedureId).put("count", 0));
            } else {
                context.fail(ar.cause());
            }
        });
    }

    private void getProcedureSummary(RoutingContext context) {
        String procedureId = context.pathParam("procedureId");
        String eServiceId = context.pathParam("eServiceId");
        MongoDB.Command command = mongodb.command("getProcedureSummary", new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put("procedureId", procedureId != null ? "\"procedure._id\":\"" + procedureId + "\"," : "");
                put("eServiceId", eServiceId != null ? "\"phases.eServiceIds\": [\"" + eServiceId + "\"]" : "");
                put("appDiagramUrl", config.server.appHref(context) + config.app.designerPath);
                put("appDiagramSvg", config.server.apiHref() + "model/diagram/");
            }
        });
        mongodb.runCommand("aggregate", command, ar -> {
            if (ar.succeeded()) {
                new JsonResponse(context).end(ar.result().getJsonArray("result"));
            } else {
                context.fail(ar.cause());
            }
        });
    }

    private static JsonObject mongoDateTimeRange(OffsetDateTime from, OffsetDateTime to) {
        JsonObject range = new JsonObject();
        if (from != null) range.put("$gte", mongoDateTime(from));
        if (to != null) range.put("$lt", mongoDateTime(to));
        return range;
    }

    private void getUserFeedback(RoutingContext context) {
        OffsetDateTime fromDateTime = parseDateTime(context.pathParam("fromDateTime"));
        if (fromDateTime == null) {
            context.fail(ServerError.message("cannot parse date-time from '" + context.pathParam("fromDateTime") + "'"));
            // context.fail(new ServerError("cannot parse date-time from '" + context.pathParam("fromDateTime") + "'"));
        }
        OffsetDateTime toDateTime = parseDateTime(context.pathParam("toDateTime"));
        JsonObject dateTimeRange = mongoDateTimeRange(fromDateTime, toDateTime);
        MongoDB.Command command = mongodb.command("getUserFeedback", new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;
            {
                put("dateTimeRange", dateTimeRange.encode());
                put("appDiagramUrl", config.server.appHref(context) + config.app.designerPath);
                put("appDiagramSvg", config.server.apiHref() + "model/diagram/");
            }
        });
        mongodb.runCommand("aggregate", command, ar -> {
            if (ar.succeeded()) {
                new JsonResponse(context).end(new JsonObject().put("dateTimeRange", dateTimeRange)
                    .put("feedbackList",
                        ar.result().getJsonArray("result")));
            } else {
                context.fail(ar.cause());
            }
        });
    }

    private void postUserFeedback(RoutingContext context) {
        if (isLoggedInFailOtherwise(context)) {
            User user = context.user();
            if (user == null) {
                context.fail(ServerError.message("user is not authenticated"));
                return;
            }
            JsonObject feedback = context.getBodyAsJson();
            String message = feedback.getString("message");
            if (message == null || message.trim().length() == 0) {
                context.fail(ServerError.message("no message in feedback"));
                return;
            }
            String diagramId = feedback.getString("diagramId");
            if (diagramId == null || diagramId.trim().length() == 0) {
                context.fail(ServerError.message("no diagram ID in feedback"));
                return;
            }
            String rootId = feedback.getString("rootId");
            if (rootId == null || rootId.trim().length() == 0) {
                context.fail(ServerError.message("no root ID in feedback"));
                return;
            }
            String elementId = feedback.getString("elementId");
            if (elementId == null || elementId.trim().length() == 0) {
                context.fail(ServerError.message("no element ID in feedback"));
                return;
            }
            feedback.put("id", UUID.randomUUID().toString())
                .put("userId", user.principal().getJsonObject("account").getString("id"))
                .put("dateTime", mongoDateTime(OffsetDateTime.now()));
            //        schemaTools.validate();

            mongodb.save("user.feedbacks", feedback, save -> {
                if (save.succeeded()) {
                    new JsonResponse(context).status(HttpResponseStatus.CREATED).end(save.result());
                } else {
                    context.fail(save.cause());
                }
            });
        }
    }

    /* PRIVATE API */

    // Diagram

    private void getModelDiagramSVG(RoutingContext context) {
        String id = context.pathParam("id");
        // TODO: generate or retrieve previously generated svg image
        // this is just a teporary code
        vertx.fileSystem().readFile("web/assets/svg/" + id + ".svg", file -> {
            if (file.succeeded()) {
                context.response().putHeader("Content-Type", "image/svg+xml").end(file.result());
            } else {
                context.fail(file.cause());
            }
        });
    }

    private void getModelDiagramList(RoutingContext context, List<String> diagramIds, String searchText, Integer limit) {
        if (diagramIds != null && diagramIds.isEmpty()) {
            JsonResponse.endWithEmptyArray(context);
            return;
        }
        final Thing.Query diagramQuery = Thing.query(Thing.Keys.DIAGRAM);
        final JsonArray andQuery = new JsonArray().add(diagramQuery.match);
        if (diagramIds != null) {
            andQuery.add(new JsonObject()
                .put("$or", new JsonArray(diagramIds.stream()
                    .filter(id -> id != null && !id.isEmpty())
                    .map(id -> new JsonObject().put("id", id))
                    .collect(Collectors.toList()))));
        }
        if (searchText != null) {
            andQuery.add(new JsonObject().put("$text",
                new JsonObject().put("$search", searchText)
            // .put("$language", config.languageCode(context))
            ));
        }
        final JsonObject query = new JsonObject().put("$and", andQuery);
        final FindOptions findOptions = new FindOptions().setSort(new JsonObject().put("lastModified", -1));
        if (limit != null) {
            findOptions.setLimit(limit);
        }
        mongodb.findWithOptions(diagramQuery.collection, query, findOptions, find -> {
            if (find.succeeded()) {
                new JsonResponse(context).end(find.result());
            } else {
                context.fail(find.cause());
            }
        });

    }

    private void getModelDiagramMyList(RoutingContext context) {
        final JsonObject diagramRoles = AuthUtils.getUserThingRoles(context.user(), Thing.Keys.DIAGRAM);
        getModelDiagramList(context, diagramRoles.stream()
            .filter(roleEntry -> !((JsonArray) roleEntry.getValue()).isEmpty())
            .map(roleEntry -> roleEntry.getKey())
            .collect(Collectors.toList()), null, null);
    }

    private void getModelDiagramSearchTextList(RoutingContext context) {
        String searchText = context.pathParam("text");
        getModelDiagramList(context, null, searchText, null);
    }

    private void getModelDiagramNewerList(RoutingContext context) {
        String limitStr = context.pathParam("limit");
        if (limitStr == null) {
            JsonResponse.endWithEmptyArray(context);
            return;
        }
        int limit;
        try {
            limit = Integer.parseInt(limitStr);
        } catch (ParseException e) {
            JsonResponse.endWithEmptyArray(context);
            return;
        }
        getModelDiagramList(context, null, null, limit);
    }

    // private void getDiagramElement(RoutingContext context) {
    //     String id = context.pathParam("id");
    //     modelTools.getDiagramEntity(id, getDiagram -> {
    //         if (getDiagram.succeeded()) {
    //             JSON_OBJECT_RESPONSE_END(context, getDiagram.result());
    //         } else {
    //             context.fail(getDiagram.cause());
    //         }
    //     });
    // }

    // private void putDiagramElement(RoutingContext context) {
    //     if (isLoggedIn(context)) {
    //         JsonObject entity = context.getBodyAsJson();
    //         // isDiagramEditor(context, entity, isDiagramEditor -> {
    //         //     if (isDiagramEditor.succeeded()) {
    //         //         modelTools.saveDiagramEntity(entity, saveDiagramEntity -> {
    //         //             if (saveDiagramEntity.succeeded()) {
    //         //                 JSON_OBJECT_RESPONSE_END(context, saveDiagramEntity.result().toJson());
    //         //             } else {
    //         //                 throw new ResponseError(context, saveDiagramEntity.cause());
    //         //             }
    //         //         });
    //         //     } else {
    //         //         throw new ResponseError(context, isDiagramEditor.cause());
    //         //     }
    //         // });
    //     }
    // }

    // private void delDiagramElement(RoutingContext context) {
    //     if (isLoggedIn(context)) {
    //         String id = context.pathParam("id");
    //         // isDiagramEditor(context, id, isDiagramEditor -> {
    //         //     if (isDiagramEditor.succeeded()) {
    //         //         modelTools.deleteDiagramEntity(id, deleteDiagramEntity -> {
    //         //             if (deleteDiagramEntity.succeeded())
    //         //                 JSON_OBJECT_RESPONSE_END(context, deleteDiagramEntity.result().toJson());
    //         //             else
    //         //                 throw new ResponseError(context, deleteDiagramEntity.cause());
    //         //         });
    //         //     } else {
    //         //         throw new ResponseError(context, isDiagramEditor.cause());
    //         //     }
    //         // });
    //     }
    // }

    // private void getModelElement(RoutingContext context) {
    //     String id = context.pathParam("id");
    //     modelTools.getModelEntity(id, getModelEntity -> {
    //         if (getModelEntity.succeeded()) {
    //             JSON_OBJECT_RESPONSE_END(context, getModelEntity.result());
    //         } else {
    //             JSON_NULL_RESPONSE_END(context);
    //         }
    //     });
    // }

    // private void putModelElement(RoutingContext context) {
    //     if (isLoggedIn(context)) {
    //         JsonObject entity = context.getBodyAsJson();
    //         // isModelEditor(context, entity, isModelEditor -> {
    //         //     if (isModelEditor.succeeded()) {
    //         //         modelTools.saveModelEntity(entity, saveModelEntity -> {
    //         //             if (saveModelEntity.succeeded()) {
    //         //                 JSON_OBJECT_RESPONSE_END(context, saveModelEntity.result().toJson());
    //         //             } else {
    //         //                 throw new ResponseError(context, saveModelEntity.cause());
    //         //             }
    //         //         });
    //         //     } else {
    //         //         throw new ResponseError(context, isModelEditor.cause());
    //         //     }
    //         // });
    //     }
    // }

    // private void delModelElement(RoutingContext context) {
    //     if (isLoggedIn(context)) {
    //         String id = context.pathParam("id");
    //         // isDiagramEditor(context, id, isDiagramEditor -> {
    //         //     if (isDiagramEditor.succeeded()) {
    //         //         modelTools.deleteModelEntity(id, deleteModelEntity -> {
    //         //             if (deleteModelEntity.succeeded())
    //         //                 JSON_OBJECT_RESPONSE_END(context, deleteModelEntity.result().toJson());
    //         //             else
    //         //                 throw new ResponseError(context, deleteModelEntity.cause());
    //         //         });
    //         //     } else {
    //         //         throw new ResponseError(context, isDiagramEditor.cause());
    //         //     }
    //         // });
    //     }
    // }

    // private void getDiagramElements(RoutingContext context) {
    //     String rootId = context.pathParam("rootId");
    //     modelTools.getDiagramElements(rootId, getDiagramElements -> {
    //         if (getDiagramElements.succeeded()) {
    //             JSON_ARRAY_RESPONSE_END(context, getDiagramElements.result());
    //         } else {
    //             context.fail(getDiagramElements.cause());
    //         }
    //     });
    // }

    // private void getDiagramModels(RoutingContext context) {
    //     String rootId = context.pathParam("rootId");
    //     modelTools.getDiagramModels(rootId, getDiagramModels -> {
    //         if (getDiagramModels.succeeded()) {
    //             JSON_ARRAY_RESPONSE_END(context, getDiagramModels.result());
    //         } else {
    //             context.fail(getDiagramModels.cause());
    //         }
    //     });
    // }

    // private void getSemanticList(RoutingContext context) {
    //     mongodb.find("semantic.entity", new JsonObject(), ar -> {
    //         if (ar.succeeded()) {
    //             JSON_ARRAY_RESPONSE_END(context, ar.result());
    //         } else {
    //             context.fail(ar.cause());
    //         }
    //     });
    // }

    // private void getSemanticListByType(RoutingContext context) {
    //     String clazz = context.pathParam("class");
    //     mongodb.find("semantic.entity", new JsonObject().put("class", clazz), ar -> {
    //         if (ar.succeeded()) {
    //             JSON_ARRAY_RESPONSE_END(context, ar.result());
    //         } else {
    //             context.fail(ar.cause());
    //         }
    //     });
    // }

    // private void getStencilSetDefinition(RoutingContext context) {
    //     final String notation = context.pathParam("notation");
    //     vertx.fileSystem().readFile(config.DATA_PATH + notation.replace(".", "/") + ".json", ar -> {
    //         if (ar.succeeded()) {
    //             JSON_OBJECT_RESPONSE_END(context, ar.result().toJsonObject());
    //         } else {
    //             context.fail(ar.cause());
    //         }
    //     });
    // }

}
