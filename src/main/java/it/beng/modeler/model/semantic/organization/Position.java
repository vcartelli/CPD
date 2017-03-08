package it.beng.modeler.model.semantic.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.beng.modeler.model.Entity;
import it.beng.modeler.model.semantic.SemanticElement;

import java.util.List;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class Position extends SemanticElement {

    public static List<Position> list() {
        return Entity.list(Position.class);
    }

    @JsonProperty(required = true)
    @JsonPropertyDescription("the name of this Position")
    public String name;
    @JsonProperty(required = true)
    @JsonPropertyDescription("the id of Person occupying this Position")
    public String personId;

    protected Position() {
    }

    public Position(String id, String ownerId, String name, String documentation, String name1, String personId) {
        super(id, ownerId, name, documentation);
        this.name = name1;
        this.personId = personId;
    }
}
