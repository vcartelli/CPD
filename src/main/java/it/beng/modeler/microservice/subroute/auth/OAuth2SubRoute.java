package it.beng.modeler.microservice.subroute.auth;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.UserSessionHandler;
import it.beng.microservice.db.MongoDB;
import it.beng.microservice.schema.SchemaTools;
import it.beng.modeler.config;
import it.beng.modeler.microservice.ResponseError;
import it.beng.modeler.microservice.subroute.SubRoute;
import it.beng.modeler.model.ModelTools;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public abstract class OAuth2SubRoute extends SubRoute<OAuth2SubRoute.Config> {

    public final static String AUTHORIZATION_CODE_GRANT = "AUTHORIZATION_CODE_GRANT";
    public final static String CLIENT_CREDENTIALS_GRANT = "CLIENT_CREDENTIALS_GRANT";
    public final static String IMPLICIT_GRANT = "IMPLICIT_GRANT";

    private static final JsonObject CITIZEN_ROLE = new JsonObject()
        .put("access", config.role.cpd.access.citizen)
        .put("context", new JsonObject()
            .put("diagram", new JsonObject()));

    private static final JsonObject EDIT_ALL_ROLE = new JsonObject()
        .put("access", config.role.cpd.access.civilServant)
        .put("context", new JsonObject()
            .put("diagram", new JsonObject()
                .put("*", new JsonArray()
                    .add("role:cpd:context:diagram:editor"))));

    protected config.OAuth2Config oauth2Config;
    protected config.OAuth2Config.Flow oauth2Flow;
    protected OAuth2ClientOptions oauth2ClientOptions;
    protected OAuth2Auth oauth2Provider;

    static class Config {
        config.OAuth2Config oauth2Config;
        String oauth2FlowType;

        Config(config.OAuth2Config oauth2Config, String oauth2FlowType) {
            this.oauth2Config = oauth2Config;
            this.oauth2FlowType = oauth2FlowType;
        }
    }

    public OAuth2SubRoute(Vertx vertx, Router router, MongoDB mongodb, SchemaTools schemaTools, ModelTools modelTools,
                          config.OAuth2Config oauth2Config, String flowType) {
        super(config.server.auth.path + oauth2Config.provider + "/", vertx, router, mongodb, schemaTools,
            modelTools, new OAuth2SubRoute.Config(oauth2Config, flowType));
    }

    protected abstract void init();

    private static OAuth2FlowType flowType(String flowType) {
        if (flowType != null)
            try {
                return OAuth2FlowType.valueOf(flowType);
            } catch (IllegalArgumentException ignored) {}
        return OAuth2FlowType.AUTH_CODE;
    }

    @Override
    protected final void init(Config oauth2SubRouteConfig) {

        this.oauth2Config = oauth2SubRouteConfig.oauth2Config;
        this.oauth2Flow = oauth2SubRouteConfig.oauth2Config.flows.get(oauth2SubRouteConfig.oauth2FlowType);

        this.oauth2ClientOptions = new OAuth2ClientOptions(new HttpClientOptions().setSsl(true))
            .setSite(oauth2Config.site)
            .setClientID(oauth2Config.clientId)
            .setClientSecret(oauth2Config.clientSecret)
            .setScopeSeparator(" ");
        if (this.oauth2Config.authPath != null)
            oauth2ClientOptions.setAuthorizationPath(oauth2Config.authPath);
        if (this.oauth2Config.tokenPath != null)
            oauth2ClientOptions.setTokenPath(oauth2Config.tokenPath);
        if (this.oauth2Config.introspectionPath != null)
            oauth2ClientOptions.setIntrospectionPath(oauth2Config.introspectionPath);

        // create OAuth2 Provider
        oauth2Provider = OAuth2Auth.create(vertx, flowType(oauth2SubRouteConfig.oauth2FlowType), oauth2ClientOptions);

        // create OAuth2 user session handler
        router.route().handler(UserSessionHandler.create(oauth2Provider));

        init();

    }

    private String generateUserId(JsonObject profile) {
        String displayName = profile.getString("displayName");
        if (displayName == null || displayName.trim().length() == 0) {
            displayName = "anonymous";
            profile.put("displayName", displayName);
        }
        String userId = displayName.toLowerCase().replace(" ", "-");
        profile.put("id", userId);
        return userId;
    }

    protected void setUserRoles(RoutingContext rc, JsonObject principal) {
        final String userId = generateUserId(principal.getJsonObject("profile"));
        vertx.fileSystem()
             .readFile(config.DATA_PATH + "roles.json", ar -> {
                 if (ar.succeeded()) {
                     final JsonArray document = ar.result().toJsonArray();
                     if (document.contains(userId))
                         principal.put("roles", new JsonObject()
                             .put("cpd", EDIT_ALL_ROLE));
                     else
                         principal.put("roles", new JsonObject()
                             .put("cpd", CITIZEN_ROLE));
                 } else {
                     throw new ResponseError(rc, ar.cause());
                 }
             });

    }

}
