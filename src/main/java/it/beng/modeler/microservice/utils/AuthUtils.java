package it.beng.modeler.microservice.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class AuthUtils {

    private static Logger logger = Logger.getLogger(AuthUtils.class.getName());

    public static final JsonObject LOGGED_OUT_CITIZEN_ROLES = new JsonObject()
        .put("system", "guest")
        .put("interaction", "citizen")
        .put("things", new JsonObject());

    public static final JsonObject LOGGED_IN_CITIZEN_ROLES = new JsonObject()
        .put("system", "user")
        .put("interaction", "citizen")
        .put("things", new JsonObject());

    public static JsonObject getUserThingRoles(User user, String thing) {
        if (user == null) {
            return new JsonObject();
        }
        try {
            JsonObject userThingRoles = user.principal()
                                            .getJsonObject("account")
                                            .getJsonObject("roles")
                                            .getJsonObject("things")
                                            .getJsonObject(thing);
            return userThingRoles == null ? new JsonObject() : userThingRoles;
        } catch (NullPointerException e) {
            return new JsonObject();
        }
    }

    public static JsonObject authority(List<String> system, List<String> interaction,
                                       Map<String, Map<String, List<String>>> things) {
        final JsonObject authority = new JsonObject();
        if (system != null) authority.put("system", new JsonArray(system));
        if (interaction != null) authority.put("interaction", new JsonArray(interaction));
        if (things != null) authority.put("things", JsonObject.mapFrom(things));
        logger.finest("AUTHORITY: " + authority.encodePrettily());
        return authority;
    }

    public static void isAuthorized(JsonObject authority, JsonObject userRoles, Handler<AsyncResult<Boolean>> handler) {
        logger.finest("checking authority " + authority.encodePrettily() + " vs. " + userRoles.encodePrettily());
        final JsonArray authoritySystem = authority.getJsonArray("system");
        final JsonArray authorityInteraction = authority.getJsonArray("interaction");
        try {
            handler.handle(Future.succeededFuture((
                authoritySystem == null || authoritySystem.contains(userRoles.getString("system"))
            ) && (
                authorityInteraction == null || authorityInteraction.contains(userRoles.getString("interaction"))
            )));
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
    }

}
