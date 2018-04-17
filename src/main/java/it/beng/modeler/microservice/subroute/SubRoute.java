package it.beng.modeler.microservice.subroute;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.db.MongoDB;
import it.beng.microservice.schema.SchemaTools;
import it.beng.modeler.config;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public abstract class SubRoute<T> {

    protected Logger logger = Logger.getLogger(this.getClass().getName());

    static {
        Json.mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        // Json.mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
        Json.mapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(Include.ALWAYS, Include.NON_NULL));
        Json.prettyMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        // Json.prettyMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
        Json.prettyMapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(Include.ALWAYS, Include.NON_NULL));
    }

    protected final String baseHref;
    protected final String path;
    protected final Vertx vertx;
    protected final Router router;
    protected final MongoDB mongodb;
    protected final SchemaTools schemaTools;
    // protected final ModelTools modelTools;
    protected final boolean isPrivate;

    public SubRoute(String path, Vertx vertx, Router router, boolean isPrivate, T userData) {
        this.baseHref = config.server.baseHref;
        this.path = baseHref + path;
        logger.info("sub-route registered: " + this.path);
        this.vertx = vertx;
        this.router = router;
        this.mongodb = config.mongoDB();
        this.schemaTools = config.schemaTools();
        // this.modelTools = modelTools;
        this.isPrivate = isPrivate;
        if (isPrivate) {
            router.route(path + "*").handler(SubRoute::privateRoute);
        }
        this.init(userData);
    }

    protected abstract void init(T userData);

    protected static void privateRoute(RoutingContext context) {
        if (context.user() == null) {
            failUnauthorized(context);
        } else {
            context.next();
        }
    }

    protected static void failUnauthorized(RoutingContext context) {
        context.fail(HttpResponseStatus.UNAUTHORIZED.code());
    }

    protected static void failMethodNotAllowed(RoutingContext context) {
        context.fail(HttpResponseStatus.METHOD_NOT_ALLOWED.code());
    }

    public static final class base64 {

        public static String decode(String encoded) {
            return new String(Base64.getDecoder().decode(encoded.replace('_', '/').replace('-', '+')),
                StandardCharsets.ISO_8859_1);
        }

        public static String encode(String decoded) {
            return Base64.getEncoder()
                .encodeToString(decoded.getBytes(StandardCharsets.ISO_8859_1))
                .replace('/', '_')
                .replace('+', '-');
        }

        public static String encode(JsonObject jsonObject) {
            return encode(jsonObject.encode());
        }
    }

    protected static boolean isLoggedInFailOtherwise(RoutingContext context) {
        if (context.user() == null) {
            context.fail(HttpResponseStatus.UNAUTHORIZED.code());
            return false;
        }
        return true;
    }

    protected static boolean isAdminFailOtherwise(RoutingContext context) {
        User user = context.user();
        if (user == null
                || !"admin".equals(user.principal().getJsonObject("account").getJsonObject("roles").getString("system"))) {
            context.fail(HttpResponseStatus.UNAUTHORIZED.code());
            return false;
        }
        return true;
    }

    protected static OffsetDateTime parseDateTime(String value) {
        if (value == null)
            return null;
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

    protected static JsonObject mongoDateTime(OffsetDateTime dateTime) {
        return new JsonObject().put("$date", dateTime != null ? dateTime.toString() : null);
    }

    public static void redirect(RoutingContext context, final String location) {
        final String _location = location.replaceAll("(?<!:)/{2,}", "/");
        context.response().setStatusCode(HttpResponseStatus.FOUND.code()).putHeader("Location", _location).end();
    }

}
