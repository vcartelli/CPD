package it.beng.modeler.model.semantic.process;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.beng.modeler.model.basic.Entity;
import it.beng.modeler.model.basic.Typed;
import it.beng.modeler.model.semantic.AcceptMatrix;
import it.beng.modeler.model.semantic.SemanticElement;

import java.util.Arrays;
import java.util.List;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class Procedure extends SemanticElement {

    public static List<Procedure> list() {
        return Entity.list(Procedure.class);
    }

    static {
        AcceptMatrix.addRule(Typed.type(Procedure.class), Arrays.asList(Typed.type(Phase.class)));
    }

    @JsonPropertyDescription("the long-term objective explaining the existence of this procedure")
    public String mission;
    public String ownerPositionId;
    public List<String> regulatoryIds;


    protected Procedure() {
    }

    public Procedure(String id, String ownerId, String name, String documentation) {
        super(id, ownerId, name, documentation);
    }

}
