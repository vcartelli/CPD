package it.beng.modeler.microservice.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.db.MongoDB;
import it.beng.modeler.config.cpd;
import it.beng.modeler.model.Domain;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class DBUtils {

    private static final MongoDB mongoDB = cpd.mongoDB();
    private static final Domain COLLABORATION = Domain.ofDefinition(Domain.Definition.DIAGRAM);

    /* COLLECTION */
    public static void loadCollection(String collection, JsonObject query, Handler<AsyncResult<List<JsonObject>>> handler) {
        mongoDB.find(collection, query, handler);
    }

    public static void loadCollection(String collection, Handler<AsyncResult<List<JsonObject>>> handler) {
        loadCollection(collection, new JsonObject(), handler);
    }

    /* EXTENSIONS */

    public static String langOrEN(JsonObject translations, String lang) {
        return translations.getString(lang, translations.getString("en"));
    }

    /* ID */

    public static JsonObject ID(String id) {
        return new JsonObject().put("id", id);
    }

    /* DATETIME */

    public static OffsetDateTime parseDateTime(String value) {
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

    public static JsonObject mongoDateTime(OffsetDateTime dateTime) {
        return new JsonObject().put("$date", dateTime != null ? dateTime.toString() : null);
    }


    /* FIELDS */

    public static JsonObject fields(Collection<String> fields, Object value) {
        if (value == null) value = 1;
        JsonObject result = new JsonObject();
        for (String field : fields) {
            result.put(field, value);
        }
        return result;
    }

    /* AND */

    public static JsonObject and(Collection<JsonObject> members) {
        List<JsonObject> a = members.stream()
                                    .filter(CommonUtils.NON_EMPTY_JSON_OBJECT)
                                    .collect(Collectors.toList());
        return a.isEmpty()
            ? new JsonObject()
            : a.size() == 1 ? a.get(0) : new JsonObject().put("$and", new JsonArray(a));
    }

    /* OR */

    public static JsonObject or(Collection<JsonObject> members) {
        List<JsonObject> a = members.stream()
                                    .filter(CommonUtils.NON_EMPTY_JSON_OBJECT)
                                    .collect(Collectors.toList());
        return a.isEmpty()
            ? new JsonObject()
            : a.size() == 1 ? a.get(0) : new JsonObject().put("$or", new JsonArray(a));
    }

    public static <T> JsonObject or(String field, Collection<T> values) {
        return or(
            values.stream()
                  .map(value -> new JsonObject().put(field, value))
                  .collect(Collectors.toList())
        );
    }

    public static <T> JsonObject or(Collection<String> fields, Object value) {
        return or(
            fields.stream()
                  .map(field -> new JsonObject().put(field, value))
                  .collect(Collectors.toList())
        );
    }

    /* TEXT */

    public static JsonObject text(String searchText, String languageCode) {
        return new JsonObject().put("$text",
            new JsonObject().put("$search", searchText)
                            .put("$language", languageCode)
        );
    }

    /* TEAM */

    public static void team(String collaborationId, Handler<AsyncResult<JsonObject>> handler) {
        mongoDB.findOne(
            COLLABORATION.getCollection(),
            and(Arrays.asList(COLLABORATION.getQuery(), ID(collaborationId))),
            fields(Arrays.asList("team"), 1),
            find -> {
                if (find.succeeded()) {
                    handler.handle(Future.succeededFuture(find.result()));
                } else handler.handle(Future.failedFuture(find.cause()));
            });
    }

}
