package it.beng.modeler.microservice.utils;

import io.vertx.core.json.JsonArray;

import java.util.List;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class JsonUtils {
    public static <T> T firstOrNull(JsonArray jsonArray) {
        return jsonArray != null && jsonArray.size() > 0 ? (T) jsonArray.getValue(0) : null;
    }

    public static <T> T firstOrNull(List<T> list) {
        return list != null && list.size() > 0 ? list.get(0) : null;
    }
}
