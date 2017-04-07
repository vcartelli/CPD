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
import java.time.format.DateTimeParseException;
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
        router.route(HttpMethod.GET, path + "element/:elementId")
              .handler(this::getElement);
        router.route(HttpMethod.GET, path + "diagram/:diagramId/elements")
              .handler(this::getDiagramElements);

        // semantic
        router.route(HttpMethod.GET, path + "semantic/list")
              .handler(this::getSemanticList);
        router.route(HttpMethod.GET, path + "semantic/list/:type")
              .handler(this::getSemanticListByType);
        router.route(HttpMethod.GET, path + "semantic/:semanticId")
              .handler(this::getSemanticElement);
        router.route(HttpMethod.PUT, path + "semantic/:semanticId")
              .handler(this::putSemanticElement);

        // feedback
        router.route(HttpMethod.GET, path + "user/feedback/:fromDateTime")
              .handler(this::getUserFeedback);
        router.route(HttpMethod.GET, path + "user/feedback/:fromDateTime/:toDateTime")
              .handler(this::getUserFeedback);

        /*** STATIC RESOURCES (swagger-ui) ***/

        // IMPORTANT!!1: redirect api to api/
        // it MUST be done with regex (i.e. must be exactly "api") to avoid infinite redirections
        router.routeWithRegex(HttpMethod.GET, "^" + Pattern.quote(path.substring(0, path.length() - 1)) + "$")
              .handler(rc -> redirect(rc, path));
        router.route(HttpMethod.GET, path + "*").handler(StaticHandler.create("web/swagger-ui"));
    }

    private void getDiagramEServiceCount(RoutingContext rc) {
        simLagTime();
        String diagramId = rc.request().getParam("diagramId");
        JsonObject command = new JsonObject()
            .put("aggregate", "diagram.elements")
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
            .put("aggregate", "diagrams")
            .put("pipeline", new JsonArray()
                .add(new JsonObject()
                    .put("$lookup", new JsonObject()
                        .put("from", "semantic.elements")
                        .put("localField", "semanticId")
                        .put("foreignField", "_id")
                        .put("as", "semantic")))
                .add(new JsonObject()
                    .put("$unwind", "$semantic"))
                .add(new JsonObject()
                    .put("$project", new JsonObject()
                        .put("_id", 1)
                        .put("notation", 1)
                        .put("name", "$semantic.name")
                        .put("documentation", "$semantic.documentation")
                        .put("url", new JsonObject()
                            .put("$concat", new JsonArray()
                                .add(config.server.pub.appHref(rc) + config.app.diagramPath)
                                .add("$_id")))
                        .put("svg", new JsonObject()
                            .put("$concat", new JsonArray()
                                .add(config.server.pub.assetsHref() + "svg/")
                                .add("$_id")
                                .add(".svg"))))));
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

    private void getDiagramEServiceSummary(RoutingContext rc) {
        simLagTime();
        String eServiceId = rc.request().getParam("eServiceId");

        JsonObject command = new JsonObject()
            .put("aggregate", "diagram.elements")
            .put("pipeline", new JsonArray()
                .add(new JsonObject()
                    .put("$match", new JsonObject()
                        .put("eServiceId", eServiceId)))
                .add(new JsonObject()
                    .put("$lookup", new JsonObject()
                        .put("from", "semantic.elements")
                        .put("localField", "semanticId")
                        .put("foreignField", "_id")
                        .put("as", "documentation")))
                .add(new JsonObject()
                    .put("$unwind", "$documentation"))
                .add(new JsonObject()
                    .put("$addFields", new JsonObject()
                        .put("documentation", "$documentation.documentation")))
                .add(new JsonObject()
                    .put("$lookup", new JsonObject()
                        .put("from", "diagrams")
                        .put("localField", "diagramId")
                        .put("foreignField", "_id")
                        .put("as", "notation")))
                .add(new JsonObject()
                    .put("$unwind", "$notation"))
                .add(new JsonObject()
                    .put("$addFields", new JsonObject()
                        .put("notation", "$notation.notation")))
                .add(new JsonObject()
                    .put("$graphLookup", new JsonObject()
                        .put("from", "diagram.elements")
                        .put("startWith", "$_id")
                        .put("connectFromField", "ownerId")
                        .put("connectToField", "_id")
                        .put("as", "path")))
                .add(new JsonObject()
                    .put("$unwind", "$path"))
                .add(new JsonObject()
                    .put("$addFields", new JsonObject()
                        .put("path", "$path.semanticId")))
                .add(new JsonObject()
                    .put("$lookup", new JsonObject()
                        .put("from", "semantic.elements")
                        .put("localField", "path")
                        .put("foreignField", "_id")
                        .put("as", "name")))
                .add(new JsonObject()
                    .put("$unwind", "$name"))
                .add(new JsonObject()
                    .put("$addFields", new JsonObject()
                        .put("name", "$name.name")))
                .add(new JsonObject()
                    .put("$group", new JsonObject()
                        .put("_id", "$diagramId")
                        .put("elementId", new JsonObject()
                            .put("$first", "$_id"))
                        .put("eServiceId", new JsonObject()
                            .put("$first", "$eServiceId"))
                        .put("notation", new JsonObject()
                            .put("$first", "$notation"))
                        .put("documentation", new JsonObject()
                            .put("$first", "$documentation"))
                        .put("path", new JsonObject()
                            .put("$push", "$name"))))
                .add(new JsonObject()
                    .put("$project", new JsonObject()
                        .put("_id", 1)
                        .put("elementId", 1)
                        .put("eServiceId", 1)
                        .put("notation", 1)
                        .put("documentation", 1)
                        .put("path", new JsonObject()
                            .put("$reduce", new JsonObject()
                                .put("input", "$path")
                                .put("initialValue", "")
                                .put("in", new JsonObject()
                                    .put("$concat", new JsonArray()
                                        .add("$$value").add("//").add("$$this")))))
                        .put("url", new JsonObject()
                            .put("$concat", new JsonArray()
                                .add(config.server.pub.appHref(rc) + config.app.diagramPath)
                                .add("$_id").add("/")
                                .add("$elementId")))
                        .put("svg", new JsonObject()
                            .put("$concat", new JsonArray()
                                .add(config.server.pub.assetsHref() + "svg/").add("$_id").add(".svg"))))));
        if (config.develop) System.out.println("getDiagramEServiceSummary command: " + command.encodePrettily());
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

    private void getSemanticElement(RoutingContext rc) {
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

    private void putSemanticElement(RoutingContext rc) {
        simLagTime();

        JsonObject body = rc.getBodyAsJson();
        String contextName = body.getString("contextName");
        String contextId = body.getString("contextId");
        rc.user().isAuthorised(contextName + "|" + contextId + "|" + config.role.cpd.context.diagram.editor, aar -> {
            if (aar.succeeded()) {
                if (aar.result()) {
                    JsonObject semantic = body.getJsonObject("semantic");
                    mongodb.save("semantic.elements", toDb(semantic), ar -> {
                        if (ar.succeeded()) {
                            // TODO: try rc.reroute
                            mongodb.findOne("semantic.elements", new JsonObject()
                                .put("_id", semantic.getString("id")), new JsonObject(), s -> {
                                if (s.succeeded()) {
                                    rc.response()
                                      .setStatusCode(HttpResponseStatus.ACCEPTED.code())
                                      .putHeader("content-type", "application/json; charset=utf-8")
                                      .end(toClient(s.result()));
                                } else {
                                    throw new ResponseError(rc, s.cause());
                                }
                            });
                        } else {
                            throw new ResponseError(rc, ar.cause());
                        }
                    });
                } else {
                    throw new ResponseError(rc, "user is not authorized for this operation");
                }
            } else throw new ResponseError(rc, aar.cause());
        });
    }

    private static ZonedDateTime parseDateTime(String value) {
        if (value == null) return null;
        ZonedDateTime dateTime = null;
        try {
            dateTime = ZonedDateTime.parse(value);
        } catch (DateTimeParseException e) {}
        if (dateTime == null) {
            try {
                dateTime = ZonedDateTime.parse(value + "+00:00");
            } catch (DateTimeParseException e) {}
        }
        if (dateTime == null) {
            try {
                dateTime = ZonedDateTime.parse(value + "T00:00:00+00:00");
            } catch (DateTimeParseException e) {}
        }
        return dateTime;
    }

    private static JsonObject mongoDateTime(ZonedDateTime dateTime) {
        return new JsonObject()
            .put("$date", dateTime.toString());
    }

    private void getUserFeedback(RoutingContext rc) {
        JsonObject query = new JsonObject().put("dateTime", new JsonObject());

        String fromDateTimeStr = rc.request().getParam("fromDateTime");
        ZonedDateTime fromDateTime = parseDateTime(fromDateTimeStr);
        if (fromDateTime == null) {
            JSON_RESPONSE(rc).end(toClient(new JsonObject().put("error", "invalid date: '" + fromDateTimeStr + "'")));
            return;
        }
        query.getJsonObject("dateTime").put("$gte", mongoDateTime(fromDateTime));

        String toDateTimeStr = rc.request().getParam("toDateTime");
        ZonedDateTime toDateTime = parseDateTime(toDateTimeStr);
        if (toDateTime != null) {
            query.getJsonObject("dateTime").put("$lt", mongoDateTime(toDateTime));
        }

        mongodb.find("user.feedback", query, ar -> {
            if (ar.succeeded()) {
                JsonObject result = new JsonObject()
                    .put("dateRange", new JsonObject().put("from", mongoDateTime(fromDateTime)))
                    .put("feedbackList", ar.result());
                if (toDateTime != null)
                    result.getJsonObject("dateRange").put("to", mongoDateTime(toDateTime));
                JSON_RESPONSE(rc).end(toClient(result));
            } else {
                throw new ResponseError(rc, ar.cause());
            }
        });
    }

}
