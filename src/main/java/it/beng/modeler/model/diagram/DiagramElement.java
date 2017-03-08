package it.beng.modeler.model.diagram;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.beng.modeler.model.Entity;
import it.beng.modeler.model.diagram.graphics.Label;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public abstract class DiagramElement extends Entity {

    public static DiagramElement getByEService(String eServiceId) {
        return Entity.list(DiagramElement.class).stream()
            .filter(element -> (element.eServiceId != null && element.eServiceId.equals(eServiceId)))
            .findFirst()
            .orElse(null);
    }

    //    public static List<DiagramElement> list(Predicate<? super T> predicate) {
//        return Entity.list(DiagramElement.class);
//    }
    public static List<DiagramElement> list() {
        return Entity.list(DiagramElement.class);
    }

    public static List<DiagramElement> listByDiagram(String diagramId) {
        return list().stream()
            .filter(element -> element.diagramId.equals(diagramId))
            .collect(Collectors.toList());
    }

    @JsonProperty(required = true)
    @JsonPropertyDescription("ID of the Diagram containing this DiagramElement")
    public String diagramId;
    @JsonProperty(required = true)
    @JsonPropertyDescription("ID of the associated SemanticElement")
    public String semanticId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyDescription("optional ID of the DiagramElement owning this one (for svg grouping)")
    public String ownerId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyDescription("optional ID of the e-service associated to this Element")
    public String eServiceId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyDescription("Label of this Element")
    public Label label;

    protected DiagramElement() {
    }

    public DiagramElement(String id, String diagramId, String semanticId, String ownerId, String eServiceId, Label label) {
        super(id);
        this.diagramId = diagramId;
        this.semanticId = semanticId;
        this.ownerId = ownerId;
        this.eServiceId = eServiceId;
        this.label = label;
    }

    public DiagramSummary diagramSummary() {
        return new DiagramSummary(Entity.get(this.diagramId, Diagram.class), this.id);
    }

}
