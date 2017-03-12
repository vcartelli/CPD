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
    protected void init() {

        // allow body handling for all post, put, delete calls to /api/*
        router.route(path + "*").handler(BodyHandler.create());

        // stats
        router.route(HttpMethod.GET, path + "stats/diagram/:diagramId/eServiceCount")
              .handler(this::getDiagramEServiceCount);
        router.route(HttpMethod.GET, path + "stats/diagram/element/:elementId/eServiceCount")
              .handler(this::getDiagramElementEServiceCount);

        // summary
        router.route(HttpMethod.GET, path + "diagram/eService/:eServiceId/summary")
              .handler(this::getDiagramEServiceSummary);
        router.route(HttpMethod.GET, path + "diagram/summary/list")
              .handler(this::getDiagramSummaryList);

        // type
        router.route(HttpMethod.GET, path + "type/ids")
              .handler(this::getTypeIds);
        router.route(HttpMethod.GET, path + "type/:typeId")
              .handler(this::getType);

        // diagram
        router.route(HttpMethod.GET, path + "diagram/:diagramId")
              .handler(this::getDiagram);
        router.route(HttpMethod.GET, path + "diagram/:diagramId/elements")
              .handler(this::getDiagramElements);

        // semantic
        router.route(HttpMethod.GET, path + "semantic/list")
              .handler(this::getSemanticList);
        router.route(HttpMethod.GET, path + "semantic/list/:type")
              .handler(this::getSemanticListByType);
        router.route(HttpMethod.GET, path + "semantic/:semanticId")
              .handler(this::getSemanticElement);
        router.put(path + "semantic/:semanticId")
              .handler(this::putSemanticElement);

        /*** STATIC RESOURCES (swagger-ui) ***/

        // IMPORTANT!!1: redirect api to api/
        // it MUST be done with regex (must end exactly with "api") to avoid infinite redirections
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

    private void getDiagramElementEServiceCount(RoutingContext rc) {
        simLagTime();
        String elementId = rc.request().getParam("elementId");
        JsonObject command = new JsonObject()
            .put("aggregate", "diagram.elements")
            .put("pipeline", new JsonArray()
                .add(new JsonObject()
                    .put("$match", new JsonObject()
                        .put("_id", elementId)))
                .add(new JsonObject()
                    .put("$graphLookup", new JsonObject()
                        .put("from", "diagram.elements")
                        .put("startWith", "$_id")
                        .put("connectFromField", "_id")
                        .put("connectToField", "ownerId")
                        .put("as", "childs")))
                .add(new JsonObject()
                    .put("$unwind", "$childs"))
                .add(new JsonObject()
                    .put("$project", new JsonObject()
                        .put("_id", 1)
                        .put("childs._id", 1)
                        .put("childs.eServiceId", 1)))
                .add(new JsonObject()
                    .put("$match", new JsonObject()
                        .put("childs.eServiceId", new JsonObject()
                            .put("$exists", true))))
                .add(new JsonObject()
                    .put("$group", new JsonObject()
                        .put("_id", "$_id")
                        .put("count", new JsonObject()
                            .put("$sum", 1)))));
        if (config.develop) System.out.println("getDiagramElementEServiceCount command: " + command.encodePrettily());
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
                                .add(config.rootHref() + config.webapp.diagramPath)
                                .add("$_id")))
                        .put("svg", new JsonObject()
                            .put("$concat", new JsonArray()
                                .add(config.assetsHref() + "svg/")
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

    private void getDiagramElements(RoutingContext rc) {
        simLagTime();
        String diagramId = rc.request().getParam("diagramId");
        JsonObject command = new JsonObject()
            .put("aggregate", "diagram.elements")
            .put("pipeline", new JsonArray()
                .add(new JsonObject()
                    .put("$match", new JsonObject()
                        .put("diagramId", diagramId)))
                .add(new JsonObject()
                    .put("$graphLookup", new JsonObject()
                        .put("from", "diagram.elements")
                        .put("startWith", "$ownerId")
                        .put("connectFromField", "ownerId")
                        .put("connectToField", "_id")
                        .put("as", "level")))
                .add(new JsonObject()
                    .put("$addFields", new JsonObject()
                        .put("level", new JsonObject()
                            .put("$size", "$level"))))
                .add(new JsonObject()
                    .put("$sort", new JsonObject()
                        .put("level", 1)))
                .add(new JsonObject()
                    .put("$project", new JsonObject()
                        .put("level", 0))));
        if (config.develop) System.out.println("getDiagramElements command: " + command.encodePrettily());
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
                        .put("from", "diagrams")
                        .put("localField", "diagramId")
                        .put("foreignField", "_id")
                        .put("as", "diagram")))
                .add(new JsonObject()
                    .put("$unwind", "$diagram"))
                .add(new JsonObject()
                    .put("$lookup", new JsonObject()
                        .put("from", "semantic.elements")
                        .put("localField", "diagram.semanticId")
                        .put("foreignField", "_id")
                        .put("as", "semantic")))
                .add(new JsonObject()
                    .put("$unwind", "$semantic"))
                .add(new JsonObject()
                    .put("$project", new JsonObject()
                        .put("_id", "$diagram._id")
                        .put("elementId", "$_id")
                        .put("eServiceId", "$eServiceId")
                        .put("notation", "$diagram.notation")
                        .put("name", "$semantic.name")
                        .put("documentation", "$semantic.documentation")
                        .put("url", new JsonObject()
                            .put("$concat", new JsonArray()
                                .add(config.rootHref() + config.webapp.diagramPath)
                                .add("$diagram._id")
                                .add("/")
                                .add("$_id")))
                        .put("svg", new JsonObject()
                            .put("$concat", new JsonArray()
                                .add(config.assetsHref() + "/svg/")
                                .add("$diagram._id")
                                .add(".svg"))))));
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
        if (!isAuthenticated(rc)) {
            throw new ResponseError(rc, "user is not authenticated");
        }
        JsonObject json = rc.getBodyAsJson();
        mongodb.save("semantic.elements", toDb(json), ar -> {
            if (ar.succeeded()) {
                mongodb.findOne("semantic.elements", new JsonObject()
                    .put("_id", json.getString("id")), new JsonObject(), s -> {
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
    }

}
