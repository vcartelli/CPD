package it.beng.modeler.model.diagram;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.beng.modeler.model.Entity;
import it.beng.modeler.model.diagram.element.Shape;
import it.beng.modeler.model.diagram.graphics.Bounds;
import it.beng.modeler.model.diagram.graphics.Label;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class Diagram extends Entity {

    public static List<Diagram> list() {
        return Entity.list(Diagram.class);
    }

    public static List<DiagramSummary> summaryList() {
        return list().stream()
            .map(diagram -> diagram.summary())
            .collect(Collectors.toList());
    }

    public static List<DiagramSummary> summaryList(String notation) {
        return list().stream()
            .filter(diagram -> diagram.notation.equals(notation))
            .map(diagram -> diagram.summary())
            .collect(Collectors.toList());
    }

    public static DiagramSummary summary(String id) {
        Diagram diagram = Entity.get(id, Diagram.class);
        return diagram != null ? diagram.summary() : null;
    }

    @JsonProperty(required = true)
    @JsonPropertyDescription("Notation of this Diagram")
    public String notation;
    @JsonProperty(required = true)
    @JsonPropertyDescription("ID of the SemanticElement associated to this Diagram")
    public String semanticId;
    @JsonProperty(required = true)
    @JsonPropertyDescription("width of this Diagram's paper")
    public Double width;
    @JsonProperty(required = true)
    @JsonPropertyDescription("height of this Diagram's paper")
    public Double height;
    @JsonProperty(required = true)
    @JsonPropertyDescription("the DiagramElement list of this Diagram")
    public List<DiagramElement> elements;

    Diagram() {
    }

    public Diagram(String id, String notation, String semanticId, Double width, Double height, List<DiagramElement> elements) {
        super(id);
        this.notation = notation;
        this.semanticId = semanticId;
        this.width = width;
        this.height = height;
        this.elements = elements;
    }

    public DiagramElement element(String elementId) {
        if (elementId == null) return null;
        return this.elements.stream()
            .filter(e -> elementId.equals(e.id))
            .findFirst()
            .orElse(null);
    }

    public DiagramSummary summary() {
        return new DiagramSummary(this);
    }

    public static void init() {
        /* Diagram #1 */
        new Diagram(
            "43467de2-9f42-477f-9f00-13b70f53ce24",
            "diagram:notation:FPMN",
            "c9561247-d5a7-4578-98b9-58021ee68ae0",
            940d,
            200d,
            Arrays.asList(
                new Shape(
                    "126350b3-4c12-4fd7-b206-6d96e08a0ba1",
                    "43467de2-9f42-477f-9f00-13b70f53ce24",
                    "c9561247-d5a7-4578-98b9-58021ee68ae0",
                    null,
                    null,
                    new Label(
                        Label.HAlign.CENTER,
                        Label.VAlign.MIDDLE,
                        "name",
                        new Bounds(
                            20d,
                            20d,
                            900d,
                            40d)),
                    new Bounds(
                        20d,
                        20d,
                        900d,
                        160d)
                ),
                new Shape(
                    "c374b442-4216-4464-a860-ee9007578b25",
                    "43467de2-9f42-477f-9f00-13b70f53ce24",
                    "a46c29cc-5814-47d0-86a9-22f6d678335a",
                    "126350b3-4c12-4fd7-b206-6d96e08a0ba1",
                    "08677aef-2b6d-4fff-bf10-91b2fe82bdc5",
                    new Label(
                        Label.HAlign.CENTER,
                        Label.VAlign.MIDDLE,
                        "name",
                        new Bounds(
                            20d,
                            60d,
                            300d,
                            120d)),
                    new Bounds(
                        20d,
                        60d,
                        300d,
                        120d)
                ),
                new Shape(
                    "58664498-6cfc-4040-b121-a090a9db8701",
                    "43467de2-9f42-477f-9f00-13b70f53ce24",
                    "05c18b01-cd5e-4b0a-b003-242f968f68df",
                    "126350b3-4c12-4fd7-b206-6d96e08a0ba1",
                    null,
                    new Label(
                        Label.HAlign.CENTER,
                        Label.VAlign.MIDDLE,
                        "name",
                        new Bounds(
                            320d,
                            60d,
                            300d,
                            120d)),
                    new Bounds(
                        320d,
                        60d,
                        300d,
                        120d)
                ),
                new Shape(
                    "ea844b6c-7395-4436-a3d6-b7b06afbfb80",
                    "43467de2-9f42-477f-9f00-13b70f53ce24",
                    "2dcbb9e5-53dd-41d4-8e87-fd5cec84922d",
                    "126350b3-4c12-4fd7-b206-6d96e08a0ba1",
                    "96949573-5ba3-438d-b2dd-68c4fc41a8b7",
                    new Label(
                        Label.HAlign.CENTER,
                        Label.VAlign.MIDDLE,
                        "name",
                        new Bounds(
                            620d,
                            60d,
                            300d,
                            120d)),
                    new Bounds(
                        620d,
                        60d,
                        300d,
                        120d)
                )
            )
        );
        /* Diagram #2 */
        new Diagram(
            "62c02032-0b13-436c-9a96-00a7e479801b",
            "diagram:notation:FPMN",
            "32585f79-cce0-4aca-86bf-f8df7a641091",
            640d,
            200d,
            Arrays.asList(
                new Shape(
                    "bac66934-c93e-4b5b-a3ac-db46d2b000f3",
                    "62c02032-0b13-436c-9a96-00a7e479801b",
                    "32585f79-cce0-4aca-86bf-f8df7a641091",
                    null,
                    null,
                    new Label(
                        Label.HAlign.CENTER,
                        Label.VAlign.MIDDLE,
                        "name",
                        new Bounds(
                            20d,
                            20d,
                            600d,
                            40d)),
                    new Bounds(
                        20d,
                        20d,
                        600d,
                        160d)
                ),
                new Shape(
                    "1bf755de-3540-48af-b4d1-e13b2f0c96fe",
                    "62c02032-0b13-436c-9a96-00a7e479801b",
                    "1b3664a8-3e83-42c9-a1eb-98ea34877cd7",
                    "bac66934-c93e-4b5b-a3ac-db46d2b000f3",
                    "1745375e-6c8b-4bb7-80f8-e84aa4013b46",
                    new Label(
                        Label.HAlign.CENTER,
                        Label.VAlign.MIDDLE,
                        "name",
                        new Bounds(
                            20d,
                            60d,
                            300d,
                            120d)),
                    new Bounds(
                        20d,
                        60d,
                        300d,
                        120d)
                ),
                new Shape(
                    "f17b9bf0-f2b7-4eb3-b8a4-0bd638dda677",
                    "62c02032-0b13-436c-9a96-00a7e479801b",
                    "5f9166af-8cd5-4bd5-a114-3c9c87ebddf9",
                    "bac66934-c93e-4b5b-a3ac-db46d2b000f3",
                    null,
                    new Label(
                        Label.HAlign.CENTER,
                        Label.VAlign.MIDDLE,
                        "name",
                        new Bounds(
                            320d,
                            60d,
                            300d,
                            120d)),
                    new Bounds(
                        320d,
                        60d,
                        300d,
                        120d)
                )
            )
        );
    }

}
