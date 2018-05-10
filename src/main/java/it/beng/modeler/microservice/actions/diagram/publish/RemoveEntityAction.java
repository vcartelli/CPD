package it.beng.modeler.microservice.actions.diagram.publish;

import io.vertx.core.json.JsonObject;
import it.beng.modeler.microservice.actions.PublishAction;
import it.beng.modeler.microservice.actions.diagram.DiagramAction;

public class RemoveEntityAction extends PublishAction implements DiagramAction {
    public static final String TYPE = "[Diagram Action Publish] Remove Entity";

    public RemoveEntityAction(JsonObject action) {
        super(action);
    }

    @Override
    protected String innerType() {
        return TYPE;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && true;
    }

}
