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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public abstract class SubRoute {

    protected final Vertx vertx;
    protected final Router router;
    protected final MongoClient mongodb;

    public SubRoute(Vertx vertx, Router router, MongoClient mongodb) {
        this.vertx = vertx;
        this.router = router;
        this.mongodb = mongodb;
        Json.mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        Json.mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        Json.prettyMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        Json.prettyMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        this.init();
    }

    protected abstract void init();

    protected static class Replacer {

        public static Replacer DB_REPLACER = new Replacer(Pattern.compile("^(\\$)(.*)$"), "_\\$$2");
        public static Replacer CLIENT_REPLACER = new Replacer(Pattern.compile("^(_\\$)(.*)$"), "\\$$2");

        public final Pattern pattern;
        public final String replace;

        public Replacer(Pattern pattern, String replace) {
            this.pattern = pattern;
            this.replace = replace;
        }
    }

    protected static Object transform(Object value, String fromId, String toId, Replacer replacer) {
        if (value instanceof JsonObject || value instanceof Map) {
            Map<String, Object> map = value instanceof JsonObject ? ((JsonObject) value).getMap() : (Map) value;
            Map<String, Object> result = new LinkedHashMap<>();
            boolean idFound = false;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String _key = entry.getKey();
                Object _value = transform(entry.getValue(), fromId, toId, replacer);
                Matcher matcher = replacer.pattern.matcher(_key);
                if (!idFound && fromId.equals(_key)) {
                    idFound = true;
                    result.put(toId, _value);
                } else if (matcher.matches()) {
                    result.put(matcher.replaceFirst(replacer.replace), _value);
                } else {
                    result.put(_key, _value);
                }
            }
            return result;
        }
        if (value instanceof JsonArray || value instanceof List) {
            List<Object> list = value instanceof JsonArray ? ((JsonArray) value).getList() : (List) value;
            List<Object> result = new LinkedList<>();
            for (Object item : list) {
                result.add(transform(item, fromId, toId, replacer));
            }
            return result;
        }
        return value;
    }

    public static JsonObject toDb(Object object) {
        return new JsonObject(Json.encode(transform(object, "id", "_id", Replacer.DB_REPLACER)));
    }

    private static String toClient(Object object, boolean pretty) {
        Object result = transform(object, "_id", "id", Replacer.CLIENT_REPLACER);
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

    protected static void redirect(HttpServerResponse response, String location) {
        response
            .setStatusCode(301)
            .putHeader("Location", location)
            .end();
    }

    protected static boolean isAuthenticated(RoutingContext rc) {
        return rc.user() != null;
    }

    protected static String getQueryParameter(String query, String paramName) {
        if (query != null && paramName != null)
            for (String s : query.split("&")) {
                String[] entry = s.split("=");
                if (paramName.equals(entry[0])) {
                    return entry[1];
                }
            }
        return null;
    }

}
