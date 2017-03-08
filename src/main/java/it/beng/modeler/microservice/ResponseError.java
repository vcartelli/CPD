package it.beng.modeler.microservice;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class ResponseError extends RuntimeException {

    public ResponseError(RoutingContext rc, Throwable t) {
        rc.put("error", json(rc, t));
    }

    public ResponseError(RoutingContext rc, String message) {
        this(rc, new IllegalStateException(message));
    }

    public static JsonObject json(RoutingContext rc, Throwable t) {
        if (t == null) t = new IllegalStateException("Unknown Error");
        HttpResponseStatus status =
            rc.statusCode() >= 0 ?
                HttpResponseStatus.valueOf(rc.statusCode()) :
                HttpResponseStatus.INTERNAL_SERVER_ERROR;
        return new JsonObject()
            .put("timestamp", System.nanoTime())
            .put("remote", rc.request().remoteAddress().host() + ":" + rc.request().remoteAddress().port())
            .put("request", "[" + rc.request().method() + "] " + rc.request().uri())
            .put("statusCode", status.code())
            .put("error", status.reasonPhrase())
            .put("message", t.getMessage());
    }

}
