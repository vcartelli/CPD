package it.beng.modeler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public abstract class Entity implements Typed {

    private static Map<String, Entity> CACHE = new LinkedHashMap<>();

    public static <T extends Entity> T get(String id) {
        return (T) CACHE.get(id);
    }

    public static <T extends Entity> T get(String id, Class<T> entityClass) {
        return entityClass.cast(CACHE.get(id));
    }

    public static boolean put(Entity entity) {
        CACHE.put(entity.id, entity);
        return true;
    }

    protected static <T extends Entity> List<T> list(Class<T> entityClass) {
        if (entityClass == null)
            throw new IllegalStateException("cannot list entities of null class");
        return CACHE.values().stream()
            .filter(element -> entityClass.isInstance(element))
            .map(element -> (T) element)
            .collect(Collectors.toList());
    }

    @JsonProperty(required = true)
    @JsonPropertyDescription("ID of this Entity")
    public String id;

    protected Entity() {
    }

    public Entity(String id) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        put(this);
    }

}
