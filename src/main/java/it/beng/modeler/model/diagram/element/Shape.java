package it.beng.modeler.model.diagram.element;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.beng.modeler.model.diagram.DiagramElement;
import it.beng.modeler.model.diagram.graphics.Bounds;
import it.beng.modeler.model.diagram.graphics.Label;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class Shape extends DiagramElement {

    @JsonProperty(required = true)
    @JsonPropertyDescription("Bounds of this Shape")
    public Bounds bounds;

    Shape() {
    }

    public Shape(String id, String diagramId, String semanticId, String ownerId, String eServiceId, Label label, Bounds bounds) {
        super(id, diagramId, semanticId, ownerId, eServiceId, label);
        this.bounds = bounds;
    }

}
