package it.beng.modeler.microservice.services;

import io.vertx.core.Vertx;
import it.beng.modeler.microservice.actions.diagram.DiagramAction;
import it.beng.modeler.microservice.actions.diagram.publish.AddEntityAction;
import it.beng.modeler.microservice.actions.diagram.publish.ChangeEntityAction;
import it.beng.modeler.microservice.actions.diagram.publish.RemoveEntityAction;
import it.beng.modeler.microservice.actions.diagram.send.LoadDefinitionAction;

public class DiagramActionService extends ActionService {

    // TODO: creare un modello "Initial FPMN" da usare come template nella creazione di un nuovo diagramma

    // register here all handled IncomingActions (Publish/Send)
    static {
        /* PUBLISH */
        registerIncomingAction(AddEntityAction.TYPE, AddEntityAction.class);
        registerIncomingAction(ChangeEntityAction.TYPE, ChangeEntityAction.class);
        registerIncomingAction(RemoveEntityAction.TYPE, RemoveEntityAction.class);
        /* SEND */
        registerIncomingAction(LoadDefinitionAction.TYPE, LoadDefinitionAction.class);
    }

    public DiagramActionService(Vertx vertx) {
        super(vertx);
    }

    @Override
    protected String address() {
        return DiagramAction.ADDRESS;
    }

}
