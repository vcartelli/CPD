package it.beng.modeler.model.diagram;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.beng.modeler.config;
import it.beng.modeler.model.basic.Entity;
import it.beng.modeler.model.basic.Typed;
import it.beng.modeler.model.semantic.SemanticElement;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class DiagramSummary implements Typed {

    private final Diagram diagram;
    @JsonProperty(required = true, access = JsonProperty.Access.READ_ONLY)
    @JsonPropertyDescription("link to the Diagram (with eventually the focused DiagramElement) in the web application")
    public String url;
    @JsonProperty(required = true, access = JsonProperty.Access.READ_ONLY)
    @JsonPropertyDescription("link to the svg image of the Diagram")
    public String svg;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonPropertyDescription("optional ID of the focused DiagramElement in the url")
    public String diagramElementId;

    DiagramSummary(Diagram diagram, String diagramElementId) {
        this.diagram = diagram;
        if (diagramElementId != null)
            this.url = config.diagramElementHref(this.diagram.id, diagramElementId);
        else
            this.url = config.diagramHref(this.diagram.id);
        this.svg = config.assetOrigin() + "/svg/" + this.diagram.id + ".svg";
        this.diagramElementId = diagramElementId;
    }

    DiagramSummary(Diagram diagram) {
        this(diagram, null);
    }

    @JsonProperty(required = true, access = JsonProperty.Access.READ_ONLY)
    @JsonPropertyDescription("ID of the Diagram")
    public String getDiagramId() {
        return this.diagram.id;
    }

    @JsonProperty(required = true, access = JsonProperty.Access.READ_ONLY)
    @JsonPropertyDescription("Notation of the Diagram")
    public String getNotation() {
        return this.diagram.notation;
    }

    @JsonProperty(required = true, access = JsonProperty.Access.READ_ONLY)
    @JsonPropertyDescription("name of the SemanticElement associated to the Diagram")
    public String getName() {
        return Entity.get(this.diagram.semanticId, SemanticElement.class).name;
    }

    @JsonProperty(required = true, access = JsonProperty.Access.READ_ONLY)
    @JsonPropertyDescription("documentation of the SemanticElement associated to the Diagram")
    public String getDocumentation() {
        return Entity.get(this.diagram.semanticId, SemanticElement.class).documentation;
    }

}
