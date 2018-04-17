package it.beng.modeler.microservice.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;

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

    public static void isAuthorized(JsonObject authority, JsonObject userRoles,
                                    final Handler<AsyncResult<Boolean>> resultHandler) {
        logger.finest("checking authority " + authority.encodePrettily() + " vs. " + userRoles.encodePrettily());
        boolean authorized = true;
        final JsonArray authoritySystem = authority.getJsonArray("system");
        if (authorized && authoritySystem != null) {
            authorized = authoritySystem.contains(userRoles.getString("system"));
        }
        final JsonArray authorityInteraction = authority.getJsonArray("interaction");
        if (authorized && authorityInteraction != null) {
            authorized = authorityInteraction.contains(userRoles.getString("interaction"));
        }
        final JsonObject authorityThings = authority.getJsonObject("things");
        if (authorized && authorityThings != null) {
            for (Map.Entry<String, Object> authorityThing : authorityThings) {
                final JsonObject userThing = userRoles.getJsonObject("things").getJsonObject(authorityThing.getKey());
                authorized = authorized && userThing != null;
                if (authorized) {
                    for (Map.Entry<String, Object> authorityThingRoles : (JsonObject) authorityThing.getValue()) {
                        final JsonArray userThingRoles = userThing.getJsonArray(authorityThingRoles.getKey());
                        authorized = authorized && userThingRoles != null;
                        if (authorized) {
                            authorized = !((JsonArray) authorityThingRoles.getValue()).stream()
                                .filter(role -> userThingRoles.contains(role))
                                .collect(Collectors.toList()).isEmpty();
                            if (!authorized) {
                                break;
                            }
                        }
                    }
                    if (!authorized) {
                        break;
                    }
                }
            }
        }
        resultHandler.handle(Future.succeededFuture(authorized));
    }

}
