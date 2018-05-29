package it.beng.modeler.microservice.actions.diagram;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import it.beng.modeler.config;
import it.beng.modeler.microservice.utils.AuthUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface DiagramAction {
    String ADDRESS = config.server.eventBus.diagramAddress;
    String COMMAND_PATH = "actions/diagram/";

    static JsonObject authority(final String diagramId, final List<String> roles) {
        return AuthUtils.authority(null, null, new HashMap<String, Map<String, List<String>>>() {{
            put("diagram", new HashMap<String, List<String>>() {{
                put(diagramId, roles);
            }});
        }});
    }

    static void isAuthorized(JsonObject account, JsonObject authority, Handler<AsyncResult<Boolean>> handler) {
        if (account == null) {
            handler.handle(Future.succeededFuture(false));
        }
        AuthUtils.isAuthorized(authority, account.getJsonObject("roles"), handler);
    }

    static void isAuthorized(JsonObject account, final String diagramId, final List<String> roles, Handler<AsyncResult<Boolean>> handler) {
        isAuthorized(account, authority(diagramId, roles), handler);
    }
}
