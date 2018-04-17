package it.beng.modeler.microservice.auth.local.impl;

import java.util.logging.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import it.beng.modeler.microservice.auth.local.LocalAuthProvider;
import it.beng.modeler.microservice.utils.AuthUtils;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class LocalUser extends AbstractUser implements User {

    private static Logger logger = Logger.getLogger(LocalUser.class.getName());

    private JsonObject principal;
    private LocalAuthProvider authProvider;

    public LocalUser(JsonObject principal, LocalAuthProvider authProvider) {
        this.principal = principal;
        this.authProvider = authProvider;
    }

    JsonObject getAccount() {
        return this.principal.getJsonObject("account");
    }

    JsonObject getRoles() {
        return this.getAccount().getJsonObject("roles");
    }

    public void isAuthorized(JsonObject authority, Handler<AsyncResult<Boolean>> resultHandler) {
        AuthUtils.isAuthorized(authority, this.getRoles(), resultHandler);
    }

    @Override
    protected void doIsPermitted(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
        isAuthorized(new JsonObject(authority), resultHandler);
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
