package it.beng.modeler.microservice.subroute;

import com.fasterxml.jackson.databind.SerializationFeature;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.db.MongoDB;
import it.beng.microservice.schema.SchemaTools;
import it.beng.modeler.config;
import it.beng.modeler.microservice.UserIsNotAuthorizedError;
import it.beng.modeler.microservice.auth.local.impl.LocalUser;
import it.beng.modeler.model.ModelTools;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public abstract class SubRoute<T> {

    private static Logger logger = Logger.getLogger(SubRoute.class.getName());

    static HttpServerResponse JSON_HEADER_RESPONSE(RoutingContext rc) {
        return rc.response().putHeader("content-type", "application/json; charset=utf-8");
    }

    static void JSON_NULL_RESPONSE(RoutingContext rc) {
        JSON_HEADER_RESPONSE(rc).end("null");
    }

    static void JSON_OBJECT_RESPONSE_END(RoutingContext rc, JsonObject jsonObject) {
        if (jsonObject != null)
            JSON_HEADER_RESPONSE(rc).end(config.develop ? jsonObject.encodePrettily() : jsonObject.encode());
        else
            JSON_NULL_RESPONSE(rc);
    }

    static void JSON_ARRAY_RESPONSE_END(RoutingContext rc, JsonArray jsonArray) {
        if (jsonArray != null)
            JSON_HEADER_RESPONSE(rc).end(config.develop ? jsonArray.encodePrettily() : jsonArray.encode());
        else
            JSON_NULL_RESPONSE(rc);
    }

    static void JSON_ARRAY_RESPONSE_END(RoutingContext rc, List list) {
        JSON_ARRAY_RESPONSE_END(rc, new JsonArray(list));
    }

    static {
        Json.mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        Json.mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
        Json.prettyMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        Json.prettyMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
    }

    protected final String baseHref;
    protected final String path;
    protected final Vertx vertx;
    protected final Router router;
    protected final MongoDB mongodb;
    protected final SchemaTools schemaTools;
    protected final ModelTools modelTools;

    public SubRoute(String path, Vertx vertx, Router router, MongoDB mongodb, SchemaTools schemaTools,
                    ModelTools modelTools, T userData) {
        this.baseHref = config.server.baseHref;
        this.path = baseHref + path;
        logger.info("sub-route registered: " + this.path);
        this.vertx = vertx;
        this.router = router;
        this.mongodb = mongodb;
        this.schemaTools = schemaTools;
        this.modelTools = modelTools;
        this.init(userData);
    }

    protected abstract void init(T userData);

    // TODO: this is just for simulating a remote call lagtime. Delete it when done.
    static void simLagTime(Long simLagTime) {
        if (!config.develop) return;
        if (simLagTime == null)
            simLagTime = config.server.simLagTime;
        if (simLagTime > 0) try {
            long ms = (long) (Math.max(0, simLagTime * (1 + new Random().nextGaussian() / 3)));
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void simLagTime() {
        simLagTime(config.server.simLagTime);
    }

    public static final class base64 {
        public static String decode(String encoded) {
            return new String(Base64.getDecoder().decode(encoded
                .replace('_', '/')
                .replace('-', '+')
            ), StandardCharsets.ISO_8859_1);
        }

        public static String encode(String decoded) {
            return Base64.getEncoder().encodeToString(decoded.getBytes(StandardCharsets.ISO_8859_1))
                         .replace('/', '_')
                         .replace('+', '-');
        }
    }

    static void isAuthorized(RoutingContext rc, String role, Handler<AsyncResult<Boolean>> handler) {
        User user = rc.user();
        if (user instanceof LocalUser)
            user.isAuthorised(role, handler);
        else if (user instanceof AccessToken)
            LocalUser.isPermitted(role, user.principal().getJsonObject("roles").getJsonObject("cpd"), handler);
        else
            handler.handle(Future.succeededFuture(false));
    }

    void isAuthorized(RoutingContext rc, String context, String role, Handler<AsyncResult<Void>> handler) {
        final User user = rc.user();
        if (user != null) {
            final String authority = context + "|" + role;
            if (user instanceof LocalUser)
                user.isAuthorised(authority, isAuthorized -> {
                    if (isAuthorized.succeeded()) {
                        if (isAuthorized.result())
                            handler.handle(Future.succeededFuture());
                        else
                            handler.handle(Future.failedFuture(new UserIsNotAuthorizedError()));
                    } else {
                        handler.handle(Future.failedFuture(isAuthorized.cause()));
                    }
                });
            else if (user instanceof AccessToken)
                LocalUser.isPermitted(authority, user.principal().getJsonObject("roles").getJsonObject("cpd"),
                    isPermitted -> {
                        if (isPermitted.succeeded()) {
                            if (isPermitted.result())
                                handler.handle(Future.succeededFuture());
                            else
                                handler.handle(Future.failedFuture(new UserIsNotAuthorizedError()));
                        } else {
                            handler.handle(Future.failedFuture(isPermitted.cause()));
                        }
                    });
        } else handler.handle(Future.failedFuture(new UserIsNotAuthorizedError()));
    }

    void isDiagramEditor(RoutingContext rc, String rootId, Handler<AsyncResult<Void>> handler) {
        isAuthorized(rc,
            "diagram|" + rootId,
            "role:cpd:context:diagram:editor",
            handler);
    }

    void isDiagramEditor(RoutingContext rc, JsonObject entity, Handler<AsyncResult<Void>> handler) {
        isDiagramEditor(rc, modelTools.getDiagramRootId(entity), handler);
    }

    void isModelEditor(RoutingContext rc, JsonObject entity, Handler<AsyncResult<Void>> handler) {
        User user = rc.user();
        if (user == null) {
            handler.handle(Future.failedFuture(new UserIsNotAuthorizedError()));
            return;
        }
        if (config.role.cpd.access
            .civilServant.equals(user.principal().getJsonObject("roles").getJsonObject("cpd").getString("access")))
            handler.handle(Future.succeededFuture());
        else
            handler.handle(Future.failedFuture(new UserIsNotAuthorizedError()));
/*
        List<String> rootIds = new LinkedList<>();
        Counter entityCounter = new Counter(diagramIds);
        for (String entityId : diagramIds) {
            modelTools.getDiagramEntity(entityId, getDiagramEntity -> {
                if (getDiagramEntity.succeeded()) {
                    rootIds.add(modelTools.getDiagramRootId(getDiagramEntity.result()));
                }
                if (!entityCounter.next()) {
                    Counter rootCounter = new Counter(rootIds);
                    rootCounter.data().put("isAuthorized", false);
                    for (String rootId : rootIds) {
                        isDiagramEditor(rc, rootId, isDiagramEditor -> {
                            if (isDiagramEditor.succeeded() && !rootCounter.data().getBoolean("isAuthorized")) {
                                rootCounter.data().put("isAuthorized", true);
                            }
                            if (!rootCounter.next()) {
                                if (rootCounter.data().getBoolean("isAuthorized"))
                                    handler.handle(Future.succeededFuture());
                                else
                                    handler.handle(Future.failedFuture(new UserIsNotAuthorizedError()));
                            }
                        });
                    }
                }
            });
        }
*/
    }

    static OffsetDateTime parseDateTime(String value) {
        if (value == null) return null;
        OffsetDateTime dateTime = null;
        try {
            dateTime = OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {}
        if (dateTime == null) {
            try {
                dateTime = OffsetDateTime.parse(value + "+00:00");
            } catch (DateTimeParseException ignored) {}
        }
        if (dateTime == null) {
            try {
                dateTime = OffsetDateTime.parse(value + "T00:00:00+00:00");
            } catch (DateTimeParseException ignored) {}
        }
        return dateTime;
    }

    static JsonObject mongoDateTime(OffsetDateTime dateTime) {
        return new JsonObject()
            .put("$date", dateTime != null ? dateTime.toString() : null);
    }

    public static void redirect(RoutingContext rc, final String location) {
        final String _location = location.replaceAll("(?<!:)/{2,}","/");
        logger.finest("REDIRECT: " + _location);
        if (_location.length() != location.length()) {
            logger.finest("DOUBLE SLASH FOUND: " + location + " --> " + _location);
        }
        rc.response()
          .setStatusCode(HttpResponseStatus.FOUND.code())
          .putHeader("Location", _location)
          .end();
    }

}
