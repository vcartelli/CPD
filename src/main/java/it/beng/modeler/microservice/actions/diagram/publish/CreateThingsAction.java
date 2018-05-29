package it.beng.modeler.microservice.actions.diagram.publish;

import io.vertx.core.json.JsonObject;
import it.beng.modeler.microservice.actions.PublishAction;
import it.beng.modeler.microservice.actions.diagram.DiagramAction;

public class CreateThingsAction extends PublishAction implements DiagramAction {
    public static final String TYPE = "[Diagram Action Publish] Create Things";

    public CreateThingsAction(JsonObject action) {
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
