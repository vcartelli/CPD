package it.beng.modeler.microservice.subroute.auth;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import it.beng.modeler.config;

import java.util.logging.Logger;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class OAuth2AuthCodeSubRoute extends OAuth2SubRoute {

    private static Logger logger = Logger.getLogger(OAuth2AuthCodeSubRoute.class.getName());

    public static final String FLOW_TYPE = "AUTH_CODE";

    public OAuth2AuthCodeSubRoute(Vertx vertx, Router router, config.OAuth2Config oauth2Config) {
        super(vertx, router, oauth2Config, FLOW_TYPE);
    }

    @Override
    protected void init() {

        if (oauth2Flow.getUserProfile != null) {
            this.oauth2ClientOptions.setUserInfoPath(oauth2Flow.getUserProfile);
        }

        // NOTE: only Google auth code flow is supported at the moment

        // create OAuth2 handler
        OAuth2AuthHandler oAuth2Handler = OAuth2AuthHandler.create(oauth2Provider, config.oauth2.origin);
        for (String scope : oauth2Flow.scope) {
            oAuth2Handler.addAuthority(scope);
        }
        oAuth2Handler.setupCallback(router.get(baseHref + "oauth2/server/callback"));
        router.route(HttpMethod.GET, path + "login/handler").handler(oAuth2Handler);
        router.route(HttpMethod.GET, path + "login/handler").handler(this::providerLoginHandler);
    }

    private void providerLoginHandler(final RoutingContext context) {

        final AccessToken user = (AccessToken) context.user();

        if (user == null) {
            context.next();
            return;
        } else {

            user.userInfo(userInfo -> {
                if (userInfo.succeeded()) {
                    logger.finest("user info: " + userInfo.result().encodePrettily());

                    final JsonObject state = new JsonObject(
                        base64.decode(context.session().remove("encodedState")));
                    final JsonObject loginState = state.getJsonObject("loginState");
                    final String provider = loginState.getString("provider");

                    getOrCreateAccount(userInfo.result(), provider, readOrCreateUser -> {
                        if (readOrCreateUser.succeeded()) {
                            user.principal().put("account", readOrCreateUser.result());
                            // redirect
                            logger.finest(
                                "auth_code flow user principal: " + context.user().principal().encodePrettily());
                            redirect(context, config.server.appPath(context) + loginState
                                .getString("redirect"));
                        } else {
                            context.fail(readOrCreateUser.cause());
                        }
                    });
                } else {
                    context.setUser(null);
                    context.fail(userInfo.cause());
                }
            });
        }
    }
}
