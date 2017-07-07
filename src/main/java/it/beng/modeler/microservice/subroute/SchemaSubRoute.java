package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.common.Counter;
import it.beng.microservice.db.MongoDB;
import it.beng.microservice.schema.SchemaTools;
import it.beng.microservice.schema.ValidateResult;
import it.beng.modeler.config;
import it.beng.modeler.microservice.ResponseError;
import it.beng.modeler.model.ModelTools;

import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class SchemaSubRoute extends VoidSubRoute {

    public SchemaSubRoute(Vertx vertx, Router router, MongoDB mongodb, SchemaTools schemaTools, ModelTools modelTools) {
        super(config.server.schema.path, vertx, router, mongodb, schemaTools, modelTools);
    }

    @Override
    protected void init() {

        router.route(HttpMethod.GET, path + "")
              .handler(this::getList);
        router.routeWithRegex(HttpMethod.GET, "^" + Pattern.quote(path) + ".*/merged$")
              .handler(this::getMergedSchema);
        router.routeWithRegex(HttpMethod.GET, "^" + Pattern.quote(path) + ".*/validate/([^\\/]+)$")
              .handler(this::validateCollection);
        router.route(HttpMethod.GET, path + "*")
              .handler(this::getSchema);
        router.route(HttpMethod.POST, path + "*")
              .handler(this::validate);
        router.route(HttpMethod.PUT, path + "*")
              .handler(this::save);
        router.route(HttpMethod.DELETE, path + "*")
              .handler(this::delete);

    }

    private void getList(RoutingContext rc) {
        simLagTime();
        schemaTools.getSchemaList(getSchemaList -> {
            if (getSchemaList.succeeded())
                JSON_ARRAY_RESPONSE_END(rc, getSchemaList.result());
            else
                throw new ResponseError(rc, getSchemaList.cause());
        });
    }

    private void getMergedSchema(RoutingContext rc) {
        simLagTime();
        String $id;
        {
            String abs = config.server.schema.fixUriScheme(rc.request().absoluteURI());
            $id = abs.substring(0, abs.length() - "/merged".length());
        }
        schemaTools.getMergedSchema($id, getMergedSchema -> {
            if (getMergedSchema.succeeded())
                JSON_OBJECT_RESPONSE_END(rc, getMergedSchema.result());
            else
                throw new ResponseError(rc, getMergedSchema.cause());
        });
    }

    private void validateCollection(RoutingContext rc) {
        simLagTime();
        String collection = rc.request().getParam("param0");
        if (collection == null) {
            JSON_NULL_RESPONSE(rc);
            return;
        }
        String $id;
        {
            String abs = config.server.schema.fixUriScheme(rc.request().absoluteURI());
            $id = abs.substring(0, abs.length() - ("/validate/").length() - collection.length());
        }
        schemaTools.getSchema($id, getSchema -> {
            if (getSchema.succeeded()) {
                final JsonObject schema = getSchema.result();
                if (schema != null) {
                    JsonObject query = new JsonObject();
                    String clazz = null;
                    {
                        JsonObject properties = schema.getJsonObject("properties");
                        if (properties != null) {
                            JsonObject c = properties.getJsonObject("class");
                            if (c != null) {
                                clazz = c.getString("const");
                            }
                        }
                    }
                    if (clazz != null) query.put("class", clazz);
                    mongodb.find(collection, query, ModelTools.JSON_ENTITY_TO_MONGO_DB, find -> {
                        if (find.succeeded()) {
                            List<JsonObject> instances = find.result();
                            rc.response().putHeader("content-type", "text/plain; charset=utf-8");
                            if (instances.size() > 0) {
                                Counter counter = new Counter(instances.size());
                                StringBuffer result = new StringBuffer();
                                for (JsonObject instance : instances) {
                                    schemaTools.validate($id, instance, validate -> {
                                        result.append("instance: ").append(instance.encodePrettily()).append("\n--> ");
                                        if (validate.succeeded()) {
                                            ValidateResult validation = validate.result();
                                            if (validation.isValid())
                                                result.append("successfully validated.");
                                            else {
                                                result.append("validation error: ")
                                                      .append(validation.errors().encodePrettily());
                                            }
                                        } else {
                                            result.append("could not be validated: ")
                                                  .append(validate.cause().getMessage());
                                        }
                                        if (counter.next())
                                            result.append("\n\n");
                                        else
                                            rc.response().end(result.toString());
                                    });
                                }
                            } else {
                                rc.response().end("nothing to validate!");
                            }
                        } else {
                            throw new ResponseError(rc, find.cause());
                        }
                    });
                } else
                    JSON_NULL_RESPONSE(rc);
            } else
                throw new ResponseError(rc, getSchema.cause());
        });
    }

    private void getSchema(RoutingContext rc) {
        simLagTime();
        final String $id = config.server.schema.fixUriScheme(rc.request().absoluteURI());
        schemaTools.getSchema($id, getSchema -> {
            if (getSchema.succeeded())
                JSON_OBJECT_RESPONSE_END(rc, getSchema.result());
            else
                throw new ResponseError(rc, getSchema.cause());
        });
    }

    private void validate(RoutingContext rc) {
        final String $id = config.server.schema.fixUriScheme(rc.request().absoluteURI());
        final JsonObject body = rc.getBodyAsJson();
        boolean ok = body != null;
        if (ok) {
            final Object value = rc.getBodyAsJson().getValue("value");
            ok = value != null;
            if (ok) {
                schemaTools.validate($id, value, validate -> {
                    if (validate.succeeded())
                        JSON_OBJECT_RESPONSE_END(rc, validate.result().toJson());
                    else
                        throw new ResponseError(rc, validate.cause());
                });
            }
        }
        if (!ok) throw new ResponseError(rc, "no value to validate against " + $id);
    }

    private void save(RoutingContext rc) {
//        isAuthorized(rc, "schema", "editor", isAuthorized -> {
//            if (isAuthorized.succeeded()) {
        final String $id = config.server.schema.fixUriScheme(rc.request().absoluteURI());
        final JsonObject body = rc.getBodyAsJson();
        boolean ok = body != null;
        if (ok) {
            final JsonObject schema = body.getJsonObject("schema");
            ok = schema != null;
            if (ok) {
                schema.put("$id", $id);
                schemaTools.saveSchema(schema, saveSchema -> {
                    if (saveSchema.succeeded())
                        JSON_OBJECT_RESPONSE_END(rc, saveSchema.result().toJson());
                    else
                        throw new ResponseError(rc, saveSchema.cause());
                });
            }
        }
        if (!ok) throw new ResponseError(rc, "no schema to save for " + $id);
//            } else throw new ResponseError(rc, isAuthorized.cause());
//        });
    }

    private void delete(RoutingContext rc) {
        throw new ResponseError(rc, "API not implemented");
/*
        isAuthorized(rc, "schema", "editor", isAuthorized -> {
            if (isAuthorized.succeeded()) {
                schemaTools.deleteSchema(rc.request().absoluteURI(), deleteSchema -> {
                    if (deleteSchema.succeeded())
                        JSON_OBJECT_RESPONSE_END(rc, deleteSchema.result());
                    else
                        throw new ResponseError(rc, deleteSchema.cause());
                });
            } else throw new ResponseError(rc, isAuthorized.cause());
        });
*/
    }

}
