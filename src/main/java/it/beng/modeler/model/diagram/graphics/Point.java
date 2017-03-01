package it.beng.modeler.model.diagram.graphics;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.beng.modeler.model.basic.Typed;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class Point implements Typed {

    @JsonProperty(required = true)
    @JsonPropertyDescription("x coordinate of this Point")
    public double x;
    @JsonProperty(required = true)
    @JsonPropertyDescription("y coordinate of this Point")
    public double y;

    Point() {
    }

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

}
