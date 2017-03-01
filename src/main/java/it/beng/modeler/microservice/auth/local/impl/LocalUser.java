package it.beng.modeler.microservice.auth.local.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import it.beng.modeler.model.basic.Typed;
import it.beng.modeler.model.semantic.organization.UserProfile;
import it.beng.modeler.model.semantic.organization.roles.AuthenticationRole;
import it.beng.modeler.model.semantic.organization.roles.AuthorizationRole;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class LocalUser extends AbstractUser implements User {

    private String username;
    private LocalAuthProviderImpl authProvider;
    private JsonObject principal;

    public LocalUser(String username, LocalAuthProviderImpl authProvider) {
        this.username = username;
        this.authProvider = authProvider;
    }

    @Override
    protected void doIsPermitted(String role, Handler<AsyncResult<Boolean>> resultHandler) {
        boolean has = false;
        if (role != null)
            if (role.startsWith(Typed.typePrefix(AuthenticationRole.class)))
                has = role.equals(principal().getString("authenticationRole"));
            else {
                String[] collaborationRole = role.split("|");
                String diagramId = collaborationRole[0];
                role = collaborationRole[1];
                if (role.startsWith(Typed.typePrefix(AuthorizationRole.class)))
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
        if (principal == null) {
            principal = new JsonObject()
                .put("provider", "local")
                .put("username", username)
                .put("profile", new JsonObject(Json.encode(UserProfile.get(username))));
        }
        return principal;
    }

    @Override
    public void setAuthProvider(AuthProvider authProvider) {
        if (authProvider instanceof LocalAuthProviderImpl) {
            this.authProvider = (LocalAuthProviderImpl) authProvider;
        } else {
            throw new IllegalArgumentException("Not a LocalAuthImpl");
        }
    }

}
