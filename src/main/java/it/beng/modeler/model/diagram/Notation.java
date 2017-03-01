package it.beng.modeler.model.diagram;

import it.beng.modeler.model.basic.Typed;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public abstract class Notation implements Typed {

    private static Map<String, Notation> CACHE = new LinkedHashMap<>();

    protected static <T extends Notation> T get(String id) {
        return (T) CACHE.get(id);
    }

    protected static <T extends Notation> List<T> list(Class<T> notationClass) {
        if (notationClass == null)
            throw new IllegalStateException("cannot list notations of null class");
        return CACHE.values().stream()
            .filter(element -> notationClass.isInstance(element))
            .map(element -> (T) element)
            .collect(Collectors.toList());
    }

    public Notation() {
        CACHE.put(this.getType(), this);
    }

}
