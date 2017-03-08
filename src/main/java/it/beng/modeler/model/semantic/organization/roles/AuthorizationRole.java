package it.beng.modeler.model.semantic.organization.roles;

import it.beng.modeler.model.Typed;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public enum AuthorizationRole implements Typed {
    OBSERVER(Typed.type(AuthorizationRole.class) + ":observer"),
    COLLABORATOR(Typed.type(AuthorizationRole.class) + ":collaborator"),
    EDITOR(Typed.type(AuthorizationRole.class) + ":editor"),
    REVIEWER(Typed.type(AuthorizationRole.class) + ":reviewer"),
    OWNER(Typed.type(AuthorizationRole.class) + ":owner");

    private final String value;

    AuthorizationRole(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
