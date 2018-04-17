package it.beng.modeler.microservice.actions;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.db.MongoDB;
import it.beng.modeler.config;

public abstract class Action {
    protected static final MongoDB mongodb = config.mongoDB();

    public final JsonObject json;

    public Action(JsonObject action) {
        this.json = action;
    }

    public boolean isValid() {
        return address() != null && type() != null;
    }

    public String address() {
        return json.getString("address");
    }

    public String type() {
        return json.getString("type");
    }

    public <T> T getValue(String field, Handler<AsyncResult<JsonObject>> handler) {
        Object value = json.getValue(field);
        if (value == null) {
            handler.handle(Future.failedFuture(
                new IllegalStateException("field ''" + field + "'' is undefined for action: " + json.encodePrettily())));
            return null;
        }
        return (T) value;
    }
}
