package it.beng.modeler.microservice.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import it.beng.modeler.config;
import it.beng.modeler.model.Domain;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.security.auth.login.AccountNotFoundException;
import java.util.Arrays;

public final class AuthUtils {
    private static final Log logger = LogFactory.getLog(AuthUtils.class);

    public static final JsonObject LOGGED_OUT_CITIZEN_ROLES = new JsonObject()
        .put("system", "guest")
        .put("interaction", "citizen")
        .put("things", new JsonObject());

    public static final JsonObject LOGGED_IN_CITIZEN_ROLES = new JsonObject()
        .put("system", "user")
        .put("interaction", "citizen")
        .put("things", new JsonObject());

    public static void isAuthorized(JsonObject authority, JsonObject userRoles, Handler<AsyncResult<Boolean>> handler) {
        logger.debug("checking authority " + authority.encodePrettily() + " vs. " + userRoles.encodePrettily());
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

    public static void isEngaged(User user, String collaborationId, Handler<Future<Boolean>> handler) {
        if (user == null || collaborationId == null) {
            handler.handle(Future.failedFuture(new NullPointerException()));
            return;
        }
        final JsonObject query = QueryUtils.or(
            Arrays.asList(
                "team.owner",
                "team.reviewer",
                "team.editor",
                "team.observer"),
            getAccount(user).getString("id")
        ).put("id", collaborationId);
        config.mongoDB().findOne(
            Domain.ofDefinition(Domain.Definition.DIAGRAM).getCollection(),
            query,
            new JsonObject(),
            findOne -> {
                if (findOne.succeeded()) {
                    handler.handle(Future.succeededFuture(findOne.result() != null));
                } else {
                    handler.handle(Future.failedFuture(findOne.cause()));
                }
            }
        );
    }

    public static JsonObject getAccount(User user) {
        return user != null && user.principal() != null
            ? user.principal().getJsonObject("account")
            : null;
    }

    public static JsonObject getAccount(RoutingContext context) {
        return getAccount(context.user());
    }

    public static void afterUserLogin(User user) throws AccountNotFoundException {
        JsonObject account = getAccount(user);
        if (account == null) {
            throw new AccountNotFoundException("illegal user after login");
        }

        // IdentityService id = config.processEngine().getIdentityService();

        logger.info("user " + account.getString("displayName") + " has logged in");
    }
}
