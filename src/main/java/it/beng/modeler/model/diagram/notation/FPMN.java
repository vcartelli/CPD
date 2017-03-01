package it.beng.modeler.model.diagram.notation;

import it.beng.modeler.model.diagram.Notation;

/**
 * <p>This file is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class FPMN extends Notation {

    private static FPMN instance = new FPMN();

    public static FPMN getInstance() {
        return instance;
    }

    private FPMN() {
    }

}
