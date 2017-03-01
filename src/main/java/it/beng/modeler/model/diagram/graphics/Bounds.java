package it.beng.modeler.model.diagram.graphics;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.beng.modeler.model.basic.Typed;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class Bounds implements Typed {

    @JsonProperty(required = true)
    @JsonPropertyDescription("x coordinate of the top-left corner of this Bounds")
    public double x;
    @JsonProperty(required = true)
    @JsonPropertyDescription("y coordinate of the top-left corner of this Bounds")
    public double y;
    @JsonProperty(required = true)
    @JsonPropertyDescription("width of this Bounds")
    public double width;
    @JsonProperty(required = true)
    @JsonPropertyDescription("height of this Bounds")
    public double height;

    Bounds() {
    }

    public Bounds(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

}
