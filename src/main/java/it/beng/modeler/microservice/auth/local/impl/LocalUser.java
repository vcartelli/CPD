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
        final JsonObject roles;
        final String name;
        final String id;
        final String role;
        final String prefix = config.role.cpd.context.prefix;

        public ContextRole(JsonObject roles, String rolePipe) {
            if (roles != null)
                this.roles = roles;
            else
                this.roles = new JsonObject();
            String[] roleSplit = rolePipe.split("\\|");
            this.name = roleSplit.length > 0 ? roleSplit[0] : "";
            this.id = roleSplit.length > 1 ? roleSplit[1] : "";
            this.role = roleSplit.length > 2 ? roleSplit[2] : "";
        }

        boolean isValid() {
            return !roles.isEmpty() && !name.isEmpty() && !id.isEmpty() && !role.isEmpty() &&
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
            String rolePrefix = prefix + ":" + name;
            if (role.startsWith(rolePrefix)) {
                JsonObject context = this.roles.getJsonObject(name);
                return doCheck(context.getJsonArray("*"), rolePrefix, this.role) ||
                    doCheck(context.getJsonArray(this.id), rolePrefix, this.role);
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

    @Override
    protected void doIsPermitted(String role, Handler<AsyncResult<Boolean>> resultHandler) {
        if (config.develop) System.out.println("checking role " + role);
        boolean has = false;
        if (role != null) {
            final JsonObject cpdRoles = cpdRoles();
            if (!cpdRoles.isEmpty()) {
                if (role.startsWith(config.role.cpd.access.prefix))
                    has = role.equals(cpdRoles.getString("access"));
                else {
                    JsonObject contextRoles = cpdRoles.getJsonObject("context");
                    if (contextRoles != null) {
                        ContextRole contextRole = new ContextRole(contextRoles, role);
                        if (contextRole.isValid()) {
                            has = contextRole.check();
                        }
                    }
                }
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
