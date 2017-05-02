package it.beng.modeler.microservice.subroute;

import com.fasterxml.jackson.databind.SerializationFeature;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import it.beng.modeler.config;
import it.beng.modeler.microservice.ResponseError;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public abstract class SubRoute {

    protected static final HttpServerResponse JSON_RESPONSE(RoutingContext rc) {
        return rc.response().putHeader("content-type", "application/json; charset=utf-8");
    }

    protected static final void NULL_RESPONSE(RoutingContext rc) {
        JSON_RESPONSE(rc).end("null");
    }

    protected final String baseHref;
    protected final String path;
    protected final Vertx vertx;
    protected final Router router;
    protected final MongoClient mongodb;

    public SubRoute(String path, Vertx vertx, Router router, MongoClient mongodb, Object userData) {
        this.baseHref = config.server.baseHref;
        this.path = (path.startsWith("/") ? "" : this.baseHref) + path;
        System.out.println("sub-route registered: " + this.path);
        this.vertx = vertx;
        this.router = router;
        this.mongodb = mongodb;
        this.init(userData);
    }

    public SubRoute(String path, Vertx vertx, Router router, MongoClient mongodb) {
        this(path, vertx, router, mongodb, null);
    }

    static {
        Json.mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        Json.mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        Json.prettyMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        Json.prettyMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    }

    protected abstract void init(Object userData);

    private static class Replacer {

        public static Replacer DB_REPLACER = new Replacer(Pattern
            .compile("^(id|\\$){1}(.+)?$"), "_$1$2");
        public static Replacer CLIENT_REPLACER = new Replacer(Pattern
            .compile("^_(id|\\$){1}(.+)?$"), "$1$2");

        public final Pattern pattern;
        public final String replace;

        public Replacer(Pattern pattern, String replace) {
            this.pattern = pattern;
            this.replace = replace;
        }

    }

    public static boolean isDateTime(String value) {
        return value != null && parseDateTime((String) value) != null;
    }

    public static boolean isDateTime(JsonObject value) {
        return value != null && value.size() == 1 && value.containsKey("$date");
    }

    private static Object transform(Object value, Replacer replacer, boolean expandDateTime) {
        if (expandDateTime && value instanceof String && isDateTime((String) value))
            return mongoDateTime(parseDateTime((String) value));
        if (!expandDateTime && value instanceof JsonObject && isDateTime((JsonObject) value))
            return ((JsonObject) value).getString("$date");
        if (value instanceof JsonObject || value instanceof Map) {
            Map<String, Object> map = value instanceof JsonObject ? ((JsonObject) value).getMap() : (Map) value;
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Matcher matcher = replacer.pattern.matcher(key);
                if (matcher.matches()) {
                    result.put(matcher.replaceFirst(replacer.replace), transform(entry
                        .getValue(), replacer, expandDateTime));
                } else {
                    result.put(key, transform(entry.getValue(), replacer, expandDateTime));
                }
            }
            return result;
        }
        if (value instanceof JsonArray || value instanceof List) {
            List<?> list = value instanceof JsonArray ? ((JsonArray) value).getList() : (List) value;
            List<Object> result = new LinkedList<>();
            for (Object item : list) {
                result.add(transform(item, replacer, expandDateTime));
            }
            return result;
        }
        return value;
    }

    public static JsonObject toDb(Object object) {
        return new JsonObject(Json.encode(transform(object, Replacer.DB_REPLACER, true)));
    }

    private static String toClient(Object object, boolean pretty) {
        Object result = transform(object, Replacer.CLIENT_REPLACER, false);
        if (pretty)
            return Json.encodePrettily(result);
        else
            return Json.encode(result);
    }

    public static String toClient(Object object) {
        return toClient(object, config.develop);
    }

    // TODO: this is just for simulating a remote call lagtime. Delete it when done.
    protected static void simLagTime(Long simLagTime) {
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

    protected static void simLagTime() {
        simLagTime(config.server.simLagTime);
    }

    protected static ZonedDateTime parseDateTime(String value) {
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

    protected static JsonObject mongoDateTime(ZonedDateTime dateTime) {
        return new JsonObject()
            .put("$date", dateTime != null ? dateTime.toString() : null);
    }

    public static void redirect(RoutingContext rc, String location) {
        rc.response()
          .setStatusCode(302)
          .putHeader("Location", location)
          .end();
    }

    protected static void checkAuthenticated(RoutingContext rc) {
        if (rc.user() == null) throw new ResponseError(rc, "user is not authenticated");
    }

}
