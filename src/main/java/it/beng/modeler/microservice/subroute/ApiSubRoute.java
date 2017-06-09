package it.beng.modeler.microservice.subroute;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import it.beng.modeler.config;
import it.beng.modeler.microservice.ResponseError;

import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class ApiSubRoute extends SubRoute {

    public ApiSubRoute(Vertx vertx, Router router, MongoClient mongodb) {
        super(config.server.api.path, vertx, router, mongodb);
    }

    @Override
    protected void init(Object userData) {

        // allow body handling for all post, put, delete calls to /api/*
        router.route(path + "*").handler(BodyHandler.create());

        // stats
        router.route(HttpMethod.GET, path + "stats/diagram/:diagramId/eServiceCount")
              .handler(this::getDiagramEServiceCount);

        // type
        router.route(HttpMethod.GET, path + "type/ids")
              .handler(this::getTypeIds);
        router.route(HttpMethod.GET, path + "type/:typeId")
              .handler(this::getType);

        // summary
        router.route(HttpMethod.GET, path + "diagram/summary/list")
              .handler(this::getDiagramSummaryList);
        router.route(HttpMethod.GET, path + "diagram/eService/:eServiceId/summary")
              .handler(this::getDiagramEServiceSummary);

        // diagram
        router.route(HttpMethod.GET, path + "diagram/:diagramId")
              .handler(this::getDiagram);
        router.route(HttpMethod.GET, path + "diagram/:diagramId/semantics")
              .handler(this::getDiagramSemantics);
        router.route(HttpMethod.GET, path + "diagram/:diagramId/elements")
              .handler(this::getDiagramElements);
        router.route(HttpMethod.GET, path + "diagram/semantic/:semanticId")
              .handler(this::getSemantic);
        router.route(HttpMethod.PUT, path + "diagram/semantic")
              .handler(this::putSemantic);
        router.route(HttpMethod.GET, path + "diagram/element/:elementId")
              .handler(this::getElement);
        router.route(HttpMethod.PUT, path + "diagram/element")
              .handler(this::putElement);
        router.route(HttpMethod.DELETE, path + "diagram/element/:data")
              .handler(this::delElement);

        // semantic
        router.route(HttpMethod.GET, path + "semantic/list")
              .handler(this::getSemanticList);
        router.route(HttpMethod.GET, path + "semantic/list/:type")
              .handler(this::getSemanticListByType);
        // feedback
        router.route(HttpMethod.GET, path + "user/feedback/:fromDateTime")
              .handler(this::getUserFeedback);
        router.route(HttpMethod.GET, path + "user/feedback/:fromDateTime/:toDateTime")
              .handler(this::getUserFeedback);

        // data
        router.route(HttpMethod.GET, path + "data/stencilSetDefinition/:notation")
              .handler(this::getStencilSetDefinition);

        /*** STATIC RESOURCES (swagger-ui) ***/

        // IMPORTANT!!1: redirect api to api/
        // it MUST be done with regex (i.e. must be exactly "api") to avoid infinite redirections
        router.routeWithRegex(HttpMethod.GET, "^" + Pattern.quote(path.substring(0, path.length() - 1)) + "$")
              .handler(rc -> redirect(rc, path));
        router.route(HttpMethod.GET, path + "*").handler(StaticHandler.create("web/swagger-ui"));
    }

    private void put(RoutingContext rc, String contextName, String contextId, String collection, JsonObject object) {
        AuthSubRoute
            .isAuthorized(rc, contextName + "|" + contextId + "|" + config.role.cpd.context.diagram.editor, ar -> {
                if (ar.succeeded()) {
                    if (ar.result()) {
                        mongodb.save(collection, toDb(object), save -> {
                            if (save.succeeded()) {
                                // TODO: try rc.reroute
                                mongodb.findOne(collection, new JsonObject()
                                    .put("_id", object.getString("id")), new JsonObject(), find -> {
                                    if (find.succeeded()) {
                                        JSON_RESPONSE(rc)
                                            .setStatusCode(HttpResponseStatus.ACCEPTED.code())
                                            .end(toClient(find.result()));
                                    } else
                                        throw new ResponseError(rc, find.cause());
                                });
                            } else
                                throw new ResponseError(rc, save.cause());
                        });
                    } else
                        throw new ResponseError(rc, "user is not authorized for this operation");
                } else
                    throw new ResponseError(rc, ar.cause());
            });
    }

    private void del(RoutingContext rc, String contextName, String contextId, String collection, JsonObject object) {
        AuthSubRoute
            .isAuthorized(rc, contextName + "|" + contextId + "|" + config.role.cpd.context.diagram.editor, ar -> {
                if (ar.succeeded()) {
                    if (ar.result()) {
                        mongodb.removeDocument(collection, toDb(object), remove -> {
                            if (remove.succeeded()) {
                                JSON_RESPONSE(rc)
                                    .setStatusCode(HttpResponseStatus.ACCEPTED.code())
                                    .end("true");
                            } else
                                throw new ResponseError(rc, remove.cause());
                        });
                    } else
                        throw new ResponseError(rc, "user is not authorized for this operation");
                } else
                    throw new ResponseError(rc, ar.cause());
            });
    }

    private void getDiagramEServiceCount(RoutingContext rc) {
        simLagTime();
        String diagramId = rc.request().getParam("diagramId");
        JsonObject command = new JsonObject()
            .put("aggregate", "semantic.elements")
            .put("pipeline", new JsonArray()
                .add(new JsonObject()
                    .put("$match", new JsonObject()
                        .put("diagramId", diagramId)
                        .put("eServiceId", new JsonObject()
                            .put("$exists", true))))
                .add(new JsonObject()
                    .put("$group", new JsonObject()
                        .put("_id", "$diagramId")
                        .put("count", new JsonObject()
                            .put("$sum", 1)))));
        if (config.develop) System.out.println("getDiagramEServiceCount command: " + command.encodePrettily());
        mongodb.runCommand("aggregate", command, ar -> {
            if (ar.succeeded()) {
                rc.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(toClient(ar.result().getJsonArray("result").getJsonObject(0)));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void getTypeIds(RoutingContext rc) {
        simLagTime();
        JsonObject query = new JsonObject();
        FindOptions options = new FindOptions()
            .setFields(new JsonObject().put("_id", "1"));
        mongodb.findWithOptions("types", query, options, ar -> {
            if (ar.succeeded()) {
                rc.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(toClient(ar.result()));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void getType(RoutingContext rc) {
        simLagTime();
        String typeId = rc.request().getParam("typeId");
        JsonObject query = new JsonObject().put("_id", typeId);
        JsonObject fields = new JsonObject();
        mongodb.findOne("types", query, fields, ar -> {
            if (ar.succeeded()) {
                rc.response()
                  .putHeader("content-typeId", "application/json; charset=utf-8")
                  .end(toClient(ar.result()));
            } else {
                throw new ResponseError(rc, ar.cause());
            }

        });
    }

    private void getDiagramSummaryList(RoutingContext rc) {
        simLagTime();
        JsonObject command = new JsonObject()
            .put("aggregate", "semantic.elements")
            .put("pipeline", new JsonArray()
                    .add(new JsonObject()
                        .put("$match", new JsonObject()
                            .put("type", "diagram:FPMN:semantic:Procedure")))
/*
    {
        "$match": {
            "type":"diagram:FPMN:semantic:Procedure"
        }
    },
*/
                    .add(new JsonObject()
                        .put("$lookup", new JsonObject()
                            .put("from", "diagrams")
                            .put("localField", "diagramId")
                            .put("foreignField", "_id")
                            .put("as", "diagram")))
/*
    {
        "$lookup" : {
          "from" : "diagrams",
          "localField" : "diagramId",
          "foreignField" : "_id",
          "as" : "diagram"
        }
    },
*/
                    .add(new JsonObject().put("$unwind", "$diagram"))
/*
    {
        "$unwind":"$diagram"
    },
*/
                    .add(new JsonObject()
                        .put("$lookup", new JsonObject()
                            .put("from", "semantic.elements")
                            .put("localField", "_id")
                            .put("foreignField", "ownerId")
                            .put("as", "phases")))
/*
    {
        "$lookup" : {
          "from" : "semantic.elements",
          "localField" : "_id",
          "foreignField" : "ownerId",
          "as" : "phases"
        }
    },
*/
                    .add(new JsonObject().put("$unwind", "$phases"))
/*
    {
        "$unwind":"$phases"
    },
*/
                    .add(new JsonObject()
                        .put("$match", new JsonObject()
                            .put("phases.nextPhaseId", new JsonObject()
                                .put("$in", new JsonArray().addNull()))
                            .put("phases.type", "diagram:FPMN:semantic:Phase")))
/*
    {
        "$match": {
            "phases.nextPhaseId":{
                "$in": [null]
            },
            "phases.type":"diagram:FPMN:semantic:Phase"
        }
    },
*/
                    .add(new JsonObject()
                        .put("$graphLookup", new JsonObject()
                            .put("from", "semantic.elements")
                            .put("startWith", "$phases._id")
                            .put("connectFromField", "prevPhaseId")
                            .put("connectToField", "_id")
                            .put("as", "phases")))
/*
    {
        "$graphLookup" : {
          "from" : "semantic.elements",
          "startWith" : "$phases._id",
          "connectFromField" : "prevPhaseId",
          "connectToField" : "_id",
          "as" : "phases"
        }
    },
*/
                    .add(new JsonObject()
                        .put("$addFields", new JsonObject()
                            .put("phases", new JsonObject()
                                .put("$map", new JsonObject()
                                    .put("input", "$phases")
                                    .put("as", "phase")
                                    .put("in", new JsonObject()
                                        .put("eServiceId", "$$phase.eServiceId")
                                        .put("name", "$$phase.name")
                                        .put("documentation", "$$phase.documentation")
                                    )))))
/*
    {
        "$addFields" : {
            "phases": {
                "$map" : {
                    "input" : "$phases",
                    "as" : "phase",
                    "in" : {
                      "eServiceId" : "$$phase.eServiceId",
                      "name" : "$$phase.name",
                      "documentation" : "$$phase.documentation"
                    }
                }
            }
        }
    },
*/
                    .add(new JsonObject()
                        .put("$project", new JsonObject()
                            .put("_id", 0)
                            .put("diagramId", "$diagram._id")
                            .put("notation", "$diagram.notation")
                            .put("name", "$name")
                            .put("documentation", "$documentation")
                            .put("phases", "$phases")
                            .put("url", new JsonObject()
                                .put("$concat", new JsonArray()
                                    .add(config.server.pub.appHref(rc) + config.app.diagramPath)
                                    .add("$_id")))
                            .put("svg", new JsonObject()
                                .put("$concat", new JsonArray()
                                    .add(config.server.pub.assetsHref() + "svg/")
                                    .add("$_id")
                                    .add(".svg")))))
/*
    {
        "$project" : {
            "_id" : 0,
            "diagramId":"$diagram._id",
            "notation" : "$diagram.notation",
            "name" : "$name",
            "documentation" : "$documentation",
            "phases":"$phases",
            "url" : {
                "$concat" : [ "https://localhost:8901/cpd/en/diagram/", "$_id" ]
            },
            "svg" : {
                "$concat" : [ "https://localhost:8901/cpd/assets/svg/", "$_id", ".svg" ]
            }
        }
    }
*/


/*
                .add(new JsonObject()
                    .put("$project", new JsonObject()
                        .put("_id", 1)
                        .put("notation", 1)
                        .put("name", 1)
                        .put("documentation", 1)
                        .put("url", new JsonObject()
                            .put("$concat", new JsonArray()
                                .add(config.server.pub.appHref(rc) + config.app.diagramPath)
                                .add("$_id")))
                        .put("svg", new JsonObject()
                            .put("$concat", new JsonArray()
                                .add(config.server.pub.assetsHref() + "svg/")
                                .add("$_id")
                                .add(".svg")))
                                )));
*/
            );
        if (config.develop) System.out.println("getDiagramSummaryList command: " + command.encodePrettily());
        mongodb.runCommand("aggregate", command, ar -> {
            if (ar.succeeded()) {
                rc.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(toClient(ar.result().getJsonArray("result")));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void getDiagram(RoutingContext rc) {
        simLagTime();
        String diagramId = rc.request().getParam("diagramId");
        HttpServerResponse response = rc.response();
        JsonObject query = new JsonObject().put("_id", diagramId);
        JsonObject fields = new JsonObject();
        mongodb.findOne("diagrams", query, fields, ar -> {
            if (ar.succeeded()) {
                response
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(toClient(ar.result()));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void putDiagram(RoutingContext rc) {
        simLagTime();
        final JsonObject body = rc.getBodyAsJson();
        final String contextName = body.getString("contextName");
        final String contextId = body.getString("contextId");
        final JsonObject diagram = body.getJsonObject("diagram");
        final String collection = "diagram:Diagram".equals(diagram.getString("type"))
            ? "diagrams"
            : "diagram.elements";
        put(rc, contextName, contextId, collection, diagram);
    }

    private void getElement(RoutingContext rc) {
        simLagTime();
        String elementId = rc.request().getParam("elementId");
        JsonObject query = new JsonObject().put("elementId", elementId);
        mongodb.findOne("diagram.elements", query, new JsonObject(), ar -> {
            if (ar.succeeded()) {
                rc.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(toClient(ar.result()));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void putElement(RoutingContext rc) {
        simLagTime();
        final JsonObject body = rc.getBodyAsJson();
        final String contextName = body.getString("contextName");
        final String contextId = body.getString("contextId");
        final JsonObject element = body.getJsonObject("element");
        final String collection = "diagram:Diagram".equals(element.getString("type"))
            ? "diagrams"
            : "diagram.elements";
        put(rc, contextName, contextId, collection, element);
    }

    private void delElement(RoutingContext rc) {
        simLagTime();
        final JsonObject data = new JsonObject(new String(Base64.getDecoder().decode(rc.request().getParam("data"))));
        final String contextName = data.getString("contextName");
        final String contextId = data.getString("contextId");
        final JsonObject element = data.getJsonObject("element");
        final String collection = "diagram:Diagram".equals(element.getString("type"))
            ? "diagrams"
            : "diagram.elements";
        del(rc, contextName, contextId, collection, element);
    }

    private void getDiagramElements(RoutingContext rc) {
        simLagTime();
        String diagramId = rc.request().getParam("diagramId");
        JsonObject query = new JsonObject().put("diagramId", diagramId);
        mongodb.find("diagram.elements", query, ar -> {
            if (ar.succeeded()) {
                rc.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(toClient(ar.result()));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void getDiagramSemantics(RoutingContext rc) {
        simLagTime();
        String diagramId = rc.request().getParam("diagramId");
        JsonObject query = new JsonObject().put("diagramId", diagramId);
        mongodb.find("semantic.elements", query, ar -> {
            if (ar.succeeded()) {
                rc.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(toClient(ar.result()));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void getDiagramEServiceSummary(RoutingContext rc) {
        simLagTime();
        String eServiceId = rc.request().getParam("eServiceId");
        JsonObject command = new JsonObject()
            .put("aggregate", "diagram.elements")
            .put("pipeline", new JsonArray()
                .add(new JsonObject()
                    .put("$lookup", new JsonObject()
                        .put("from", "semantic.elements")
                        .put("localField", "semanticId")
                        .put("foreignField", "_id")
                        .put("as", "model")))
                .add(new JsonObject()
                    .put("$match", new JsonObject()
                        .put("model.eServiceId", eServiceId)))
                .add(new JsonObject().put("$unwind", "$model"))
                .add(new JsonObject()
                    .put("$lookup", new JsonObject()
                        .put("from", "diagrams")
                        .put("localField", "diagramId")
                        .put("foreignField", "_id")
                        .put("as", "procedure")))
                .add(new JsonObject().put("$unwind", "$procedure"))
                .add(new JsonObject()
                    .put("$addFields", new JsonObject()
                        .put("notation", "$procedure.notation")))
                .add(new JsonObject()
                    .put("$lookup", new JsonObject()
                        .put("from", "semantic.elements")
                        .put("localField", "model.ownerId")
                        .put("foreignField", "_id")
                        .put("as", "procedure")))
                .add(new JsonObject().put("$unwind", "$procedure"))
                .add(new JsonObject()
                    .put("$graphLookup", new JsonObject()
                        .put("from", "semantic.elements")
                        .put("startWith", "$semanticId")
                        .put("connectFromField", "ownerId")
                        .put("connectToField", "_id")
                        .put("as", "path")))
                .add(new JsonObject()
                    .put("$addFields", new JsonObject()
                        .put("path", new JsonObject()
                            .put("$reduce", new JsonObject()
                                .put("input", "$path.name")
                                .put("initialValue", "")
                                .put("in", new JsonObject()
                                    .put("$concat", new JsonArray()
                                        .add("$$value")
                                        .add("//")
                                        .add("$$this")))))))
                .add(new JsonObject()
                    .put("$graphLookup", new JsonObject()
                        .put("from", "semantic.elements")
                        .put("startWith", "$semanticId")
                        .put("connectFromField", "nextPhaseId")
                        .put("connectToField", "_id")
                        .put("as", "phases")
                        .put("restrictSearchWithMatch", new JsonObject()
                            .put("type", "diagram:FPMN:semantic:Phase"))))
                .add(new JsonObject().put("$unwind", "$phases"))
                .add(new JsonObject()
                    .put("$match", new JsonObject()
                        .put("phases.nextPhaseId", new JsonObject()
                            .put("$in", new JsonArray().addNull()))))
                .add(new JsonObject()
                    .put("$graphLookup", new JsonObject()
                        .put("from", "semantic.elements")
                        .put("startWith", "$phases._id")
                        .put("connectFromField", "prevPhaseId")
                        .put("connectToField", "_id")
                        .put("as", "phases")))
                .add(new JsonObject()
                    .put("$addFields", new JsonObject()
                        .put("phases", new JsonObject()
                            .put("$map", new JsonObject()
                                .put("input", "$phases")
                                .put("as", "phase")
                                .put("in", new JsonObject()
                                    .put("eServiceId", "$$phase.eServiceId")
                                    .put("name", "$$phase.name")
                                    .put("documentation", "$$phase.documentation")
                                )))))
                .add(new JsonObject()
                    .put("$project", new JsonObject()
                        .put("_id", 0)
                        .put("diagramId", "$diagramId")
                        .put("notation", "$notation")
                        .put("name", "$procedure.name")
                        .put("documentation", "$procedure.documentation")
                        .put("elementId", "$_id")
                        .put("eServiceId", "$model.eServiceId")
                        .put("path", "$path")
                        .put("phases", "$phases")
                        .put("url", new JsonObject()
                            .put("$concat", new JsonArray()
                                .add(config.server.pub.appHref(rc) + config.app.diagramPath)
                                .add("$diagramId")
                                .add("/")
                                .add("$_id")))
                        .put("svg", new JsonObject()
                            .put("$concat", new JsonArray()
                                .add(config.server.pub.assetsHref() + "svg/")
                                .add("$diagramId")
                                .add(".svg"))))));
        if (config.develop) System.out.println("getDiagramEServiceSummary command: " + command.encodePrettily());
        mongodb.runCommand("aggregate", command, ar -> {
            if (ar.succeeded()) {
                JsonArray result = ar.result().getJsonArray("result");
                rc.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(toClient(result.size() > 0 ? result.getJsonObject(0) : null));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void getSemanticList(RoutingContext rc) {
        simLagTime();
        mongodb.find("semantic.elements", new JsonObject(), ar -> {
            if (ar.succeeded()) {
                rc.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(toClient(ar.result()));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void getSemanticListByType(RoutingContext rc) {
        simLagTime();
        String type = rc.request().getParam("type");
        mongodb.find("semantic.elements", new JsonObject().put("type", type), ar -> {
            if (ar.succeeded()) {
                rc.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(toClient(ar.result()));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void getSemantic(RoutingContext rc) {
        simLagTime();
        String semanticId = rc.request().getParam("semanticId");
        mongodb.findOne("semantic.elements", new JsonObject().put("_id", semanticId), new JsonObject(), ar -> {
            if (ar.succeeded()) {
                rc.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(toClient(ar.result()));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void putSemantic(RoutingContext rc) {
        simLagTime();
        final JsonObject body = rc.getBodyAsJson();
        final String contextName = body.getString("contextName");
        final String contextId = body.getString("contextId");
        final JsonObject semantic = body.getJsonObject("semantic");
        final String collection = "diagram:Diagram".equals(semantic.getString("type"))
            ? "diagrams"
            : "semantic.elements";
        put(rc, contextName, contextId, collection, semantic);
    }

    private static JsonObject mongoDateTimeRange(ZonedDateTime from, ZonedDateTime to) {
        JsonObject range = new JsonObject();
        if (from != null)
            range.put("$gte", mongoDateTime(from));
        if (to != null)
            range.put("$lt", mongoDateTime(to));
        return range;
    }

    private static JsonObject clientDateTimeRange(ZonedDateTime from, ZonedDateTime to) {
        JsonObject range = new JsonObject();
        if (from != null)
            range.put("from", from.toString());
        if (to != null)
            range.put("to", to.toString());
        return range;
    }

    private void getUserFeedback(RoutingContext rc) {
        ZonedDateTime fromDateTime = parseDateTime(rc.request().getParam("fromDateTime"));
        ZonedDateTime toDateTime = parseDateTime(rc.request().getParam("toDateTime"));
        JsonObject command = new JsonObject()
            .put("aggregate", "user.feedback")
            .put("pipeline", new JsonArray()
                .add(new JsonObject()
                    .put("$match", new JsonObject()
                        .put("dateTime", mongoDateTimeRange(fromDateTime, toDateTime))))
                .add(new JsonObject()
                    .put("$graphLookup", new JsonObject()
                        .put("from", "diagram.elements")
                        .put("startWith", "$elementId")
                        .put("connectFromField", "ownerId")
                        .put("connectToField", "_id")
                        .put("as", "path")))
                .add(new JsonObject()
                    .put("$addFields", new JsonObject()
                        .put("path", "$path.semanticId")))
                .add(new JsonObject()
                    .put("$lookup", new JsonObject()
                        .put("from", "semantic.elements")
                        .put("localField", "path")
                        .put("foreignField", "_id")
                        .put("as", "path")))
                .add(new JsonObject()
                    .put("$addFields", new JsonObject()
                        .put("path", "$path.name")))
                .add(new JsonObject()
                    .put("$project", new JsonObject()
                        .put("_id", 1)
                        .put("dateTime", 1)
                        .put("userId", 1)
                        .put("feedback", 1)
                        .put("diagramId", 1)
                        .put("elementId", 1)
                        .put("url", new JsonObject()
                            .put("$concat", new JsonArray()
                                .add(config.server.pub.appHref(rc) + config.app.diagramPath)
                                .add("$diagramId")
                                .add("/")
                                .add("$elementId")))
                        .put("path", new JsonObject()
                            .put("$reduce", new JsonObject()
                                .put("input", "$path")
                                .put("initialValue", "")
                                .put("in", new JsonObject()
                                    .put("$concat", new JsonArray()
                                        .add("$$value").add("//").add("$$this"))))))));
        if (config.develop) System.out.println("getUserFeedback command: " + command.encodePrettily());
        mongodb.runCommand("aggregate", command, ar -> {
            if (ar.succeeded()) {
                JsonObject result = new JsonObject()
                    .put("dateRange", clientDateTimeRange(fromDateTime, toDateTime))
                    .put("feedbackList", ar.result().getJsonArray("result"));
                JSON_RESPONSE(rc).end(toClient(result));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

    private void getStencilSetDefinition(RoutingContext rc) {
        final String notation = rc.request().getParam("notation");
        vertx.fileSystem()
             .readFile(config.DATA_PATH + notation.replace(":", "/") + ".json", ar -> {
                 if (ar.succeeded()) {
                     final JsonObject document = ar.result().toJsonObject();
                     JSON_RESPONSE(rc).end(document.encode());
                 } else {
                     throw new ResponseError(rc, ar.cause());
                 }
             });
    }

}
