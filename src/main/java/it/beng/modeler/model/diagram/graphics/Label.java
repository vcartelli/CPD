package it.beng.modeler.model.diagram.graphics;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.beng.modeler.model.Typed;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class Label implements Typed {

    public enum HAlign {
        START("start"),
        END("end"),
        LEFT("left"),
        RIGHT("right"),
        CENTER("center"),
        JUSTIFY("justify");

        private final String value;

        HAlign(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    public enum VAlign {
        BASELINE("baseline"),
        BOTTOM("bottom"),
        LENGTH("length"),
        MIDDLE("middle"),
        PERCENTAGE("percentage"),
        SUB("sub"),
        SUPER("super"),
        TEXT_BOTTOM("text-bottom"),
        TEXT_TOP("text-top"),
        TOP("top");

        private final String value;

        VAlign(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    @JsonProperty(required = true, defaultValue = "start")
    @JsonPropertyDescription("horizontal alignment of text in this Label")
    public HAlign hAlign;
    @JsonProperty(required = true, defaultValue = "top")
    @JsonPropertyDescription("vertical alignment of text in this Label")
    public VAlign vAlign;
    @JsonProperty(required = true, defaultValue = "name")
    @JsonPropertyDescription("SemanticElement's property name to render as text in this Label")
    public String property;
    @JsonProperty(required = true)
    @JsonPropertyDescription("Bounds of this Label")
    public Bounds bounds;

    Label() {
    }

    public Label(HAlign hAlign, VAlign vAlign, String property, Bounds bounds) {
        if (hAlign != null) this.hAlign = hAlign;
        else this.hAlign = HAlign.LEFT;
        if (vAlign != null) this.vAlign = vAlign;
        else this.vAlign = VAlign.TOP;
        if (property != null) this.property = property;
        else this.property = "name";
        this.bounds = bounds;
    }

}
