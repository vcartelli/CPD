package it.beng.modeler.microservice.subroute.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.UserSessionHandler;
import it.beng.modeler.config;
import it.beng.modeler.microservice.subroute.SubRoute;
import it.beng.modeler.microservice.utils.AuthUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public abstract class OAuth2SubRoute extends SubRoute<OAuth2SubRoute.Config> {

    private static Logger logger = Logger.getLogger(OAuth2SubRoute.class.getName());

    protected final static String FIRST_NAME = "firstName";
    protected final static String LAST_NAME = "lastName";
    protected final static String DISPLAY_NAME = "displayName";
    protected final static String EMAIL = "email";
    protected static final Map<String, Map<String, String>> PROVIDER_MAPS = new HashMap<>();
    protected static final Map<String, Map<String, String>> AAC_PROVIDER_MAPS = new HashMap<>();
    static {
        Map<String, String> provider;
        /* Google */
        provider = new HashMap<>();
        provider.put(FIRST_NAME, "given_name");
        provider.put(LAST_NAME, "family_name");
        provider.put(EMAIL, "email");
        PROVIDER_MAPS.put("Google", provider);
        /* AAC */
        provider = new HashMap<>();
        provider.put(FIRST_NAME, config.oauth2.aac.givenname);
        provider.put(LAST_NAME, config.oauth2.aac.surname);
        provider.put(EMAIL, "OIDC_CLAIM_email");
        AAC_PROVIDER_MAPS.put("google", provider);
        provider = new HashMap<>();
        provider.put(FIRST_NAME, config.oauth2.aac.givenname);
        provider.put(LAST_NAME, config.oauth2.aac.surname);
        provider.put(EMAIL, "id");
        AAC_PROVIDER_MAPS.put("facebook", provider);
        provider = new HashMap<>();
        provider.put(FIRST_NAME, config.oauth2.aac.givenname);
        provider.put(LAST_NAME, config.oauth2.aac.surname);
        provider.put(EMAIL, "email");
        AAC_PROVIDER_MAPS.put("internal", provider);
    }

    public final static String AUTHORIZATION_CODE_GRANT = "AUTHORIZATION_CODE_GRANT";
    public final static String CLIENT_CREDENTIALS_GRANT = "CLIENT_CREDENTIALS_GRANT";
    public final static String IMPLICIT_GRANT = "IMPLICIT_GRANT";

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

    public OAuth2SubRoute(Vertx vertx, Router router, config.OAuth2Config oauth2Config, String flowType) {
        super(config.server.auth.path + oauth2Config.provider + "/",
            vertx,
            router,
            false,
            new OAuth2SubRoute.Config(oauth2Config, flowType));
    }

    protected abstract void init();

    private static OAuth2FlowType flowType(String flowType) {
        if (flowType != null) try {
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

    protected void readOrCreateUser(final JsonObject account, Handler<AsyncResult<Void>> handler) {
        String id = account.getString("id");
        if ("".equals(id.trim())) {
            account.put("roles", AuthUtils.LOGGED_IN_CITIZEN_ROLES);
            handler.handle(Future.succeededFuture());
            return;
        }
        mongodb.findOne(config.USER_COLLECTION, new JsonObject().put("id", id), new JsonObject(), find -> {
            if (find.succeeded()) {
                JsonObject roles;
                if (find.result() != null) {
                    roles = find.result().getJsonObject("roles");
                } else {
                    roles = AuthUtils.LOGGED_IN_CITIZEN_ROLES;
                }
                account.put("roles", roles);
                if (account.equals(find.result())) {
                    handler.handle(Future.succeededFuture());
                } else {
                    // update the record if something has changed
                    logger.warning("user record for " + account.getString("id") + " has changed, updating db record");
                    mongodb.save(config.USER_COLLECTION, account, save -> {
                        if (save.succeeded()) {
                            handler.handle(Future.succeededFuture());
                        } else {
                            handler.handle(Future.failedFuture(save.cause()));
                            return;
                        }
                    });
                }
            } else {
                handler.handle(Future.failedFuture(find.cause()));
            }
        });
    }

}
