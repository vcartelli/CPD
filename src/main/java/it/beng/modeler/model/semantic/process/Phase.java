package it.beng.modeler.model.semantic.process;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.beng.modeler.model.Entity;
import it.beng.modeler.model.semantic.SemanticElement;

import java.util.List;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class Phase extends SemanticElement {

    public static List<Phase> list() {
        return Entity.list(Phase.class);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyDescription("the id of the previous Phase")
    public String prevPhaseId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyDescription("the id of the next Phase")
    public String nextPhaseId;

    protected Phase() {
    }

    public Phase(String id, String ownerId, String name, String documentation, String prevPhaseId, String nextPhaseId) {
        super(id, ownerId, name, documentation);
        this.prevPhaseId = prevPhaseId;
        this.nextPhaseId = nextPhaseId;
    }

}
