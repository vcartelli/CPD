package it.beng.modeler.model.semantic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import it.beng.modeler.model.basic.Entity;
import it.beng.modeler.model.basic.Typed;
import it.beng.modeler.model.semantic.process.Phase;
import it.beng.modeler.model.semantic.process.Procedure;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public abstract class SemanticElement extends Entity {

    public static List<? extends SemanticElement> list() {
        return Entity.list(SemanticElement.class);
    }

    public static List<? extends SemanticElement> listByType(String type) {
        return list().stream()
            .filter(semantic -> semantic.getType().equals(type))
            .collect(Collectors.toList());
    }

    public static SemanticElement createOrUpdate(JsonObject json) throws DecodeException {
        String type = json.getString("type");
        if (type != null) {
            json.remove("type");
            if (type.equals(Typed.type(Procedure.class)))
                return Json.decodeValue(json.encode(), Procedure.class);
            if (type.equals(Typed.type(Phase.class)))
                return Json.decodeValue(json.encode(), Phase.class);
        }
        return null;
    }

    @JsonPropertyDescription("optional ID of the SemanticElement owning this one")
    public String ownerId;
    @JsonProperty(required = true)
    @JsonPropertyDescription("name of this SemanticElement")
    public String name;
    @JsonProperty(required = true)
    @JsonPropertyDescription("documentation of this SemanticElement")
    public String documentation;

    protected SemanticElement() {
    }

    public SemanticElement(String id, String ownerId, String name, String documentation) {
        super(id);
        this.ownerId = ownerId;
        this.name = name;
        this.documentation = documentation;
    }

    public boolean accepts(SemanticElement s) {
        return AcceptMatrix.accepts(this, s);
    }

    public static void init() {
        /* Procedure #1 */
        new Procedure(
            "c9561247-d5a7-4578-98b9-58021ee68ae0",
            null,
            "Enrolment to day nursery service",
            "The day nursery service aims at offering day nursery for 0-3 year olds; the day-long care is based in a centre and the" +
                "\neducation and care programs are created around the developmental needs, interests and experience of each child. +" +
                "\nIn the project context, we are going to handle the enrolment process."
        );
        new Phase(
            "a46c29cc-5814-47d0-86a9-22f6d678335a",
            "c9561247-d5a7-4578-98b9-58021ee68ae0",
            "Service enrolment request",
            "The citizen (usually a parent) compiles the enrolment to day nursery service request module before a specific deadline.",
            null,
            "05c18b01-cd5e-4b0a-b003-242f968f68df"
        );
        new Phase(
            "05c18b01-cd5e-4b0a-b003-242f968f68df",
            "c9561247-d5a7-4578-98b9-58021ee68ae0",
            "Requests evaluation",
            "The council collects all the module requests and within 30 days after the deadline, based on the defined rules, the" +
                "\ncouncil produces a diagramList of the children entitled for the service.",
            "a46c29cc-5814-47d0-86a9-22f6d678335a",
            "2dcbb9e5-53dd-41d4-8e87-fd5cec84922d"
        );
        new Phase(
            "2dcbb9e5-53dd-41d4-8e87-fd5cec84922d",
            "c9561247-d5a7-4578-98b9-58021ee68ae0",
            "Service acceptance",
            "The citizen (usually a parent) within 10 day from the diagramList publication must compile the acceptance module.",
            "05c18b01-cd5e-4b0a-b003-242f968f68df",
            null
        );
        /* Procedure #2 */
        new Procedure(
            "32585f79-cce0-4aca-86bf-f8df7a641091",
            null,
            "Permit on acoustic derogation for temporary activities",
            "The service aims at managing the acoustic derogation for temporary activities permit. The service has different" +
                "\nspecialization, such as:" +
                "\n" +
                "\n* temporary acoustic derogation for building;" +
                "\n* temporary acoustic derogation musical entertainment at public premises or events derogation for concerts, events," +
                "\n  performances."
        );
        new Phase(
            "1b3664a8-3e83-42c9-a1eb-98ea34877cd7",
            "32585f79-cce0-4aca-86bf-f8df7a641091",
            "Permit request",
            "The requester compile the request for acoustic derogation for temporary activities permit.",
            null,
            "5f9166af-8cd5-4bd5-a114-3c9c87ebddf9"
        );
        new Phase(
            "5f9166af-8cd5-4bd5-a114-3c9c87ebddf9",
            "32585f79-cce0-4aca-86bf-f8df7a641091",
            "Release of permit",
            "The council evaluates the request and base on internal rules releases the acoustic derogation permit.",
            "1b3664a8-3e83-42c9-a1eb-98ea34877cd7",
            null
        );
    }

}
