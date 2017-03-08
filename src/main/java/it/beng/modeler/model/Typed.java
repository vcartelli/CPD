package it.beng.modeler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kjetland.jackson.jsonSchema.JsonSchemaConfig;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import it.beng.modeler.model.diagram.Diagram;
import it.beng.modeler.model.diagram.DiagramElement;
import it.beng.modeler.model.diagram.DiagramSummary;
import it.beng.modeler.model.diagram.Notation;
import it.beng.modeler.model.diagram.element.Edge;
import it.beng.modeler.model.diagram.element.Shape;
import it.beng.modeler.model.diagram.graphics.Bounds;
import it.beng.modeler.model.diagram.graphics.Label;
import it.beng.modeler.model.diagram.graphics.Point;
import it.beng.modeler.model.diagram.notation.FPMN;
import it.beng.modeler.model.semantic.SemanticElement;
import it.beng.modeler.model.semantic.organization.UserProfile;
import it.beng.modeler.model.semantic.organization.roles.AuthenticationRole;
import it.beng.modeler.model.semantic.organization.roles.AuthorizationRole;
import it.beng.modeler.model.semantic.process.Phase;
import it.beng.modeler.model.semantic.process.Procedure;

import java.io.InvalidClassException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public interface Typed {

    @JsonProperty(required = true)
    @JsonPropertyDescription("type of this Thing")
    default String getType() {
        return type(this.getClass());
    }

    static final String PREFIX_PATTERN = "^" + Pattern.quote(Typed.class.getPackage().getName()) + "\\.?";

    static String typePrefix(Class<? extends Typed> typeClass) {
        return typeClass.getPackage().getName()
                        .replaceFirst(PREFIX_PATTERN, "")
                        .replace('.', ':');
    }

    static String type(Class<? extends Typed> typeClass) {
        String prefix = typePrefix(typeClass);
        return (prefix.length() > 0 ? prefix + ':' : "") + typeClass.getSimpleName();
    }

    class Cache {
        private static Map<String, JsonNode> SCHEMAS = new LinkedHashMap<>();
    }

    static void register(Class<? extends Typed> typeClass) throws InvalidClassException, JsonProcessingException {
        if (typeClass != null) {
            String type = type(typeClass);
            if (Cache.SCHEMAS.get(type) != null) {
                throw new InvalidClassException("Schema type for class '" + typeClass.getCanonicalName() +
                    "' has already been registered. Try to unregister it first.");
            }
            JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(
                new ObjectMapper()
                    .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true),
                JsonSchemaConfig.html5EnabledSchema()
            );

            // If using JsonSchema to generate HTML5 GUI:
            // JsonSchemaGenerator html5 = new JsonSchemaGenerator(objectMapper, JsonSchemaConfig.html5EnabledSchema() );

            // If you want to confioure it manually:
            // JsonSchemaConfig config = JsonSchemaConfig.create(...);
            // JsonSchemaGenerator generator = new JsonSchemaGenerator(objectMapper, config);

            JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(typeClass);
            Cache.SCHEMAS.put(type, jsonSchema);

            // String jsonSchemaAsString = objectMapper.writeValueAsString(jsonSchema);

            System.out.println("schema for type '" + type + "' has been registered");
        }
    }

    static boolean unregister(String type) {
        return Cache.SCHEMAS.remove(type) != null;
    }

    public static Set<String> knownTypes() {
        return Cache.SCHEMAS.keySet();
    }

    public static JsonNode schema(String type) {
        return Cache.SCHEMAS.get(type);
    }

    public static void init() {
        try {
                /* Meta */
            register(Typed.class);
            register(Entity.class);
                /* Diagram */
            register(Diagram.class);
            register(DiagramSummary.class);
            register(Notation.class);
            register(FPMN.class);
            register(DiagramElement.class);
            register(Shape.class);
            register(Edge.class);
            register(Label.class);
            register(Bounds.class);
            register(Point.class);
                /* Semantic */
            register(SemanticElement.class);
            register(Procedure.class);
            register(Phase.class);
            register(AuthenticationRole.class);
            register(AuthorizationRole.class);
            register(UserProfile.class);
        } catch (InvalidClassException | JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}
