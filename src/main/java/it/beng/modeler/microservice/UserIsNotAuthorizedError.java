package it.beng.modeler.microservice;

import io.vertx.core.impl.NoStackTraceThrowable;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class UserIsNotAuthorizedError extends NoStackTraceThrowable {

    public UserIsNotAuthorizedError(String message) {
        super(message != null ? message : "user is not authorized for this operation");
    }

    public UserIsNotAuthorizedError() {
        this(null);
    }
}
