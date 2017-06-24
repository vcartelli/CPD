package it.beng.modeler.microservice.auth.local.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
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

    private JsonObject principal;
    private LocalAuthProvider authProvider;

    public LocalUser(JsonObject principal, LocalAuthProvider authProvider) {
        this.principal = principal;
        this.authProvider = authProvider;
    }

    private static class ContextRole {
        final JsonObject userRoles;
        final String context;
        final String id;
        final String role;
        final String prefix;

        public ContextRole(JsonObject userRoles, String rolePipe) {
            if (userRoles != null)
                this.userRoles = userRoles;
            else
                this.userRoles = new JsonObject();
            String[] roleSplit = rolePipe.split("\\|");
            if (roleSplit.length > 2) {
                this.role = roleSplit[2];
                this.id = roleSplit[1];
            } else if (roleSplit.length > 1) {
                this.role = roleSplit[1];
                this.id = "";
            } else {
                this.role = "";
                this.id = "";
            }
            this.context = roleSplit.length > 0 ? roleSplit[0] : "";
            this.prefix = config.role.cpd.context.prefix;
        }

        boolean isValid() {
            return !userRoles.isEmpty() && !context.isEmpty() && !role.isEmpty() &&
                !prefix.isEmpty() && role.startsWith(prefix);
        }

        private static boolean doCheck(JsonArray roles, String prefix, String role) {
            if (roles != null && prefix != null && role != null) {
                final String star = prefix + ":*";
                for (Object item : roles) {
                    if (star.equals(item) || role.equals(item)) {
                        return true;
                    }
                }
            }
            return false;
        }

        boolean check() {
            String rolePrefix = prefix + ":" + context;
            if (role.startsWith(rolePrefix)) {
                JsonObject context = this.userRoles.getJsonObject(this.context);
                return doCheck(context.getJsonArray("*"), rolePrefix, role)
                    || doCheck(context.getJsonArray(id), rolePrefix, role);
            }
            return false;
        }

    }

    public JsonObject roles() {
        JsonObject roles = principal().getJsonObject("roles");
        if (roles == null) {
            roles = new JsonObject();
            principal().put("roles", roles);
        }
        return roles;
    }

    public JsonObject cpdRoles() {
        JsonObject cpdRoles = roles().getJsonObject("cpd");
        if (cpdRoles == null) {
            cpdRoles = new JsonObject();
            roles().put("cpd", cpdRoles);
        }
        return cpdRoles;
    }

    public static void isPermitted(final String role, final JsonObject userRoles, final Handler<AsyncResult<Boolean>> resultHandler) {
        if (config.develop) System.out.println("checking role " + role);
        boolean has = false;
        if (role != null && !userRoles.isEmpty()) {
            if (role.startsWith(config.role.cpd.access.prefix))
                has = role.equals(userRoles.getString("access"));
            else {
                JsonObject userContextRoles = userRoles.getJsonObject("context");
                if (userContextRoles != null) {
                    ContextRole contextRole = new ContextRole(userContextRoles, role);
                    if (contextRole.isValid()) {
                        has = contextRole.check();
                    }
                }
            }
        }
        resultHandler.handle(Future.succeededFuture(has));
    }

    @Override
    protected void doIsPermitted(String role, Handler<AsyncResult<Boolean>> resultHandler) {
        isPermitted(role, this.cpdRoles(), resultHandler);
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
