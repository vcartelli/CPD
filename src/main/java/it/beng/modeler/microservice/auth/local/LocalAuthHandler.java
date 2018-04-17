package it.beng.modeler.microservice.auth.local;

import it.beng.modeler.microservice.auth.local.impl.LocalAuthHandlerImpl;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public interface LocalAuthHandler /*extends AuthHandler*/ {
    static LocalAuthHandler create(LocalAuthProvider authProvider) {
        return new LocalAuthHandlerImpl(authProvider) {};
    }
}
