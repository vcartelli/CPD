package it.beng.modeler.microservice.http;

import java.time.Instant;
import java.util.Collections;
import java.util.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.common.ServerError;
import it.beng.modeler.config;

public final class JsonResponse {

    private static final Logger logger = Logger.getLogger(JsonResponse.class.getName());

    private final RoutingContext context;

    public JsonResponse(RoutingContext context) {
        this.context = context;
        context.response().putHeader("Content-Type", "application/json; charset=utf-8");
    }

    public static void endWithEmptyArray(RoutingContext context) {
        new JsonResponse(context).end(Collections.EMPTY_LIST);
    }

    private String json(Object value) {
        return config.develop ? Json.encodePrettily(value) : Json.encode(value);
    }

    public JsonResponse chunked() {
        context.response().setChunked(true);
        return this;
    }

    public JsonResponse status(HttpResponseStatus status) {
        context.response().setStatusCode(status.code());
        return this;
    }

    public JsonResponse write(Object value) {
        context.response().write(json(value));
        return this;
    }

    public void end(Object value) {
        context.response().end(json(value));
    }

    public void end() {
        context.response().end();
    }

    public void fail() {
        final Throwable failure = this.context.failure();
        JsonObject error = new JsonObject();
        HttpResponseStatus statusCode;
        try {
            statusCode = HttpResponseStatus.valueOf(context.statusCode());
        } catch (Exception e) {
            statusCode = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        }
        error.put("timestamp", Instant.now().toEpochMilli());
        error.put("code", statusCode.code());
        error.put("message", failure != null ? failure.getLocalizedMessage() : statusCode.reasonPhrase());
        error.put("cause", failure != null ? new JsonObject(Json.encode(failure.getCause())) : "null");
        if (failure instanceof ServerError) {
            error.put("payload", ((ServerError) failure).payload);
        }
        logger.severe("ERROR (" + statusCode + "): " + error.encodePrettily());
        context.response().setStatusCode(statusCode.code()).end(error.encode());
    }

}
