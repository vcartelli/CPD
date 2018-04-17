package it.beng.modeler.microservice.actions;

import io.vertx.core.json.JsonObject;

public abstract class PublishAction extends IncomingAction {
    public PublishAction(JsonObject action) {
        super(action);
    }
}
