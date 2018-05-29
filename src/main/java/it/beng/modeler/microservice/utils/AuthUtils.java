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

    public static void isAuthorized(JsonObject authority, JsonObject userRoles,
                                    Handler<AsyncResult<Boolean>> handler) {
        logger.finest("checking authority " + authority.encodePrettily() + " vs. " + userRoles.encodePrettily());
        final JsonArray authoritySystem = authority.getJsonArray("system");
        final JsonArray authorityInteraction = authority.getJsonArray("interaction");
        final JsonObject authorityThings = authority.getJsonObject("things");
        final JsonObject userThings = userRoles.getJsonObject("things");
        try {
            handler.handle(Future.succeededFuture((
                    authoritySystem == null || authoritySystem
                        .contains(userRoles.getString("system"))
                ) && (
                    authorityInteraction == null || authorityInteraction
                        .contains(userRoles.getString("interaction"))
                ) && (
                    // things = { "thing name": { "thing id": [ "thing role", ... ], ... }, ... };
                    // e.g. { "diagram": { ... }, "process": { ... }, ... };
                    authorityThings == null || authorityThings
                        .stream()
                        .map(authorityThingsEntry -> {
                            // thing = <"thing name", { "thing id": [ "thing role", ... ], ... }>;
                            // e.g. <"diagram", { ... }>;
                            final JsonObject userThing = userThings.getJsonObject(authorityThingsEntry.getKey());
                            return (userThing != null &&
                                ((JsonObject) authorityThingsEntry.getValue())
                                    .stream()
                                    .map(authorityThingRolesEntry -> {
                                        // thingRoles = <"thing id", [ "thing role", ... ]>;
                                        // e.g. <"ff5fc3e8-2858-4ab0-a18f-00a1166c4a0b", [ "editor", "reviewer", ... ]>
                                        final JsonArray userThingRoles = userThing
                                            .getJsonArray(authorityThingRolesEntry.getKey());
                                        final JsonArray authorityThingRoles = (JsonArray) authorityThingRolesEntry.getValue();
                                        return (authorityThingRoles.isEmpty() ||
                                            userThingRoles != null && !userThingRoles.isEmpty() && userThingRoles
                                                .stream()
                                                .map(authorityThingRoles::contains)
                                                .reduce(true, (a, b) -> a && b));
                                    }).reduce(true, (a, b) -> a && b));
                        }).reduce(true, (a, b) -> a && b)))
            );
        } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
        }
//        boolean authorized = true;
//        if (authorized && authoritySystem != null) {
//            authorized = authoritySystem.contains(userRoles.getString("system"));
//        }
//        if (authorized && authorityInteraction != null) {
//            authorized = authorityInteraction.contains(userRoles.getString("interaction"));
//        }
//        if (authorized && authorityThings != null) {
//            for (Map.Entry<String, Object> authorityThing : authorityThings) {
//                final JsonObject userThing = userRoles.getJsonObject("things").getJsonObject(authorityThing.getKey());
//                authorized = authorized && userThing != null;
//                if (authorized) {
//                    for (Map.Entry<String, Object> authorityThingRoles : (JsonObject) authorityThing.getValue()) {
//                        final JsonArray userThingRoles = userThing.getJsonArray(authorityThingRoles.getKey());
//                        authorized = authorized && userThingRoles != null;
//                        if (authorized) {
//                            authorized = !((JsonArray)
//                                authorityThingRoles.getValue()).stream()
//                                                               .filter(userThingRoles::contains)
//                                                               .collect(Collectors.toList())
//                                                               .isEmpty();
//                            if (!authorized) {
//                                break;
//                            }
//                        }
//                    }
//                    if (!authorized) {
//                        break;
//                    }
//                }
//            }
//        }
//        handler.handle(Future.succeededFuture(authorized));
    }

}
