package it.beng.modeler.microservice.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class JsonUtils {
    public static JsonObject firstOrNull(JsonArray jsonArray) {
        return jsonArray != null && jsonArray.size() > 0 ? jsonArray.getJsonObject(0) : null;
    }

    public static JsonObject firstOrNull(List<JsonObject> list) {
        return list != null && list.size() > 0 ? list.get(0) : null;
    }

    public static JsonObject coalesce(JsonObject jsonObject, JsonObject def) {
        return jsonObject != null ? jsonObject : def;
    }
}
