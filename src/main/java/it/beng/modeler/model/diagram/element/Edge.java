package it.beng.modeler.model.diagram.element;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.beng.modeler.model.diagram.DiagramElement;
import it.beng.modeler.model.diagram.graphics.Label;
import it.beng.modeler.model.diagram.graphics.Point;

import java.util.List;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class Edge extends DiagramElement {

    @JsonProperty(required = true)
    @JsonPropertyDescription("list of Point of this Edge")
    public List<Point> waypoint;

    Edge() {
    }

    public Edge(String id, String diagramId, String semanticId, String ownerId, String eServiceId, Label label, List<Point> waypoint) {
        super(id, diagramId, semanticId, ownerId, eServiceId, label);
        this.waypoint = waypoint;
    }

}