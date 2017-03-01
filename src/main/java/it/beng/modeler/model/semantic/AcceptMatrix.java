package it.beng.modeler.model.semantic;

import java.util.*;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class AcceptMatrix {

    private static Map<String, List<String>> RULES = new LinkedHashMap<>();

    public static boolean addRule(String parentType, List<String> childTypes) {
        if (parentType == null || childTypes == null || childTypes.isEmpty())
            return false;
        List<String> c = RULES.get(parentType);
        if (c == null) {
            c = new LinkedList<>();
            RULES.put(parentType, c);
        }
        return c.addAll(childTypes);
    }

    public static List<String> accepts(SemanticElement parent) {
        List<String> accepts = RULES.get(parent.getType());
        return accepts != null ? accepts : Collections.emptyList();
    }

    public static boolean accepts(SemanticElement parent, SemanticElement child) {
        List<String> childs = RULES.get(parent.getType());
        return childs.stream()
            .filter(c -> child.getType().equals(c))
            .findFirst()
            .orElse(null) != null;
    }

}
