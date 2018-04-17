package it.beng.modeler.microservice.auth.local;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.AuthProvider;
import it.beng.modeler.microservice.auth.local.impl.LocalAuthProviderImpl;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public interface LocalAuthProvider extends AuthProvider {

    static LocalAuthProvider create(Vertx vertx) {
        return new LocalAuthProviderImpl(vertx);
    }

}
