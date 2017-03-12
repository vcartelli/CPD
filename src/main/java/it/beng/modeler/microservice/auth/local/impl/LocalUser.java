package it.beng.modeler.microservice.auth.local.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import it.beng.modeler.config;
import it.beng.modeler.microservice.auth.local.LocalAuthProvider;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class LocalUser extends AbstractUser implements User {

    private static String AUTHENTICATION_ROLE_PREFIX = "semantic:organization:roles:AuthenticationRole";
    private static String AUTHORIZATION_ROLE_PREFIX = "semantic:organization:roles:AuthorizationRole";

    private JsonObject principal;
    private LocalAuthProvider authProvider;

    public LocalUser(JsonObject principal, LocalAuthProvider authProvider) {
        this.principal = principal;
        this.authProvider = authProvider;
    }

    @Override
    protected void doIsPermitted(String role, Handler<AsyncResult<Boolean>> resultHandler) {
        if (config.develop) System.out.println("checking role " + role);
        boolean has = false;
        if (role != null)
            if (role.startsWith(AUTHENTICATION_ROLE_PREFIX))
                has = role.equals(principal().getString("authenticationRole"));
            else {
                String[] collaborationRole = role.split("|");
                String diagramId = collaborationRole[0];
                role = collaborationRole[1];
                if (role.startsWith(AUTHORIZATION_ROLE_PREFIX))
                    for (Object item : principal().getJsonObject("authorizationRoles").getJsonArray(diagramId))
                        if (role.equals(item)) {
                            has = true;
                            break;
                        }
            }
        resultHandler.handle(Future.succeededFuture(has));
    }

    @Override
    public JsonObject principal() {
        return principal;
    }

    @Override
    public void setAuthProvider(AuthProvider authProvider) {
        if (authProvider instanceof LocalAuthProvider) {
            this.authProvider = (LocalAuthProvider) authProvider;
        } else {
            throw new IllegalArgumentException(authProvider.getClass() + " is not a LocalAuthProvider");
        }
    }

}
