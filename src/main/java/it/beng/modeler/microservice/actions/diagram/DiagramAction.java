package it.beng.modeler.microservice.actions.diagram;

import it.beng.modeler.config;

public interface DiagramAction {
    public static final String ADDRESS = config.server.eventBus.diagramAddress;
    public static final String COMMAND_PATH = "actions/diagram/";
}
