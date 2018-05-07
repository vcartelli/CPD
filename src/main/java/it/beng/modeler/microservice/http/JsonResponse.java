package it.beng.modeler.microservice.http;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.common.ServerError;
import it.beng.modeler.config;

import java.time.Instant;
import java.util.logging.Logger;

public final class JsonResponse {

    private static final Logger logger = Logger.getLogger(JsonResponse.class.getName());

    public static HttpServerResponse setHeader(RoutingContext context) {
        return context.response().putHeader("Content-Type", "application/json; charset=utf-8");
    }

    private final RoutingContext context;
    private final HttpServerResponse response;

    public JsonResponse(RoutingContext context) {
        this.context = context;
        this.response = setHeader(context);
    }

    public static void endWithEmptyObject(RoutingContext context) {
        setHeader(context).end("{}");
    }

    public static void endWithEmptyArray(RoutingContext context) {
        setHeader(context).end("[]");
    }

    private String toJson(Object value) {
        return config.develop ? Json.encodePrettily(value) : Json.encode(value);
    }

    public JsonResponse chunked() {
        this.response.setChunked(true);
        return this;
    }

    public JsonResponse status(HttpResponseStatus status) {
        this.response.setStatusCode(status.code());
        return this;
    }

    public JsonResponse write(Object value) {
        this.response.write(toJson(value));
        return this;
    }

    public void end(Object value) {
        this.response.end(toJson(value));
    }

    public void end() {
        this.response.end();
    }

    public void fail() {
        if (this.response.ended()) {
            return;
        }
        Throwable failure;
        HttpResponseStatus status;
        if (this.context.failed()) {
            failure = this.context.failure() != null
                ? this.context.failure()
                : new NullPointerException();
            status = this.context.statusCode() != -1
                ? HttpResponseStatus.valueOf(this.context.statusCode())
                : HttpResponseStatus.INTERNAL_SERVER_ERROR;
        } else {
            failure = null;
            status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        }
        JsonObject error = new JsonObject()
            .put("timestamp", Instant.now().toEpochMilli())
            .put("code", status.code())
            .put("message", failure != null ? failure.getLocalizedMessage() : status.reasonPhrase())
            .put("cause", failure != null ? new JsonObject(Json.encode(failure.getCause())) : "null");
        if (failure instanceof ServerError) {
            error.put("payload", ((ServerError) failure).payload);
        }
        logger.severe("ERROR (" + status + "): " + error.encodePrettily());
        this.response.setStatusCode(status.code()).end(error.encode());
    }

}
