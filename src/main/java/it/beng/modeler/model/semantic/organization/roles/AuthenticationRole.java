package it.beng.modeler.model.semantic.organization.roles;

import it.beng.modeler.model.basic.Typed;
import it.beng.modeler.model.semantic.organization.UserProfile;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public enum AuthenticationRole implements Typed {
    ADMINISTRATOR(Typed.type(AuthenticationRole.class) + ":admin"),
    CITIZEN(Typed.type(AuthenticationRole.class) + ":citizen"),
    CIVIL_SERVANT(Typed.type(AuthenticationRole.class) + ":civil-servant");

    private final String value;

    AuthenticationRole(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
