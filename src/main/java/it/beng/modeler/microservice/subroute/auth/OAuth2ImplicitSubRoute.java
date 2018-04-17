package it.beng.modeler.microservice.subroute.auth;

import java.util.UUID;
import java.util.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.impl.AccessTokenImpl;
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import it.beng.microservice.common.ServerError;
import it.beng.modeler.config;
import it.beng.modeler.microservice.http.JsonResponse;
import it.beng.modeler.microservice.subroute.AuthSubRoute;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class OAuth2ImplicitSubRoute extends OAuth2SubRoute {

    private static Logger logger = Logger.getLogger(OAuth2ImplicitSubRoute.class.getName());

    public static final String FLOW_TYPE = "IMPLICIT";

    public OAuth2ImplicitSubRoute(Vertx vertx, Router router, config.OAuth2Config oAuth2Config) {
        super(vertx, router, oAuth2Config, FLOW_TYPE);
    }

    /*
        https://simpatico.business-engineering.it/cpd/oauth2/server/callback,
        https://localhost:8901/cpd/oauth2/client/callback,
        https://simpatico.business-engineering.it/cpd/oauth2/client/callback,
        https://localhost:8901/cpd/oauth2/server/callback
    
    */

    @Override
    protected void init() {
        router.route(HttpMethod.GET, path + "login/handler").handler(this::loginHandler);
        router.route(HttpMethod.GET, baseHref + "oauth2/client/callback").handler(context -> {
            // TODO: check why reroute keeps the hash segment in safari
            context.reroute(config.server.appPath(context) + "oauth2/client/callback");
            /* this doesn't work in safari as it cleans the hash fragment on redirects... */
            // redirect(context, config.server.appPath(context) + "oauth2/client/callback");
        });
        router.route(HttpMethod.POST, path + "hash").handler(this::loginWithHash);
    }

    private void loginHandler(RoutingContext context) {
        StringBuilder url = new StringBuilder(oauth2ClientOptions.getSite() + oauth2ClientOptions.getAuthorizationPath())
            .append("?")
            .append("response_type=token")
            .append("&")
            .append("grant_type=implicit")
            .append("&")
            .append("redirect_uri=")
            .append(config.server.origin())
            .append(baseHref).append("oauth2/client/callback")
            .append("&")
            .append("client_id=")
            .append(oauth2ClientOptions.getClientID())
            .append("&")
            .append("scope=")
            .append(oauth2Flow.scope/* .replace(" ", ",") */)
            .append("&")
            .append("state=")
            .append((String) context.session().get("encodedState"));
        redirect(context, url.toString());
    }

    private void loginWithHash(final RoutingContext context) {

        final JsonObject hash = context.getBodyAsJson();
        logger.finest("receiverd hash: " + hash.encodePrettily());
        AuthSubRoute.checkEncodedStateStateCookie(context, hash.getString("state"));
        hash.remove("state");
        final WebClient client = WebClient.create(vertx,
            new WebClientOptions().setUserAgent("CPD-WebClient/1.0").setFollowRedirects(false));
        client.requestAbs(HttpMethod.GET, oauth2Flow.getUserProfile)
            .putHeader("Accept", "application/json")
            .putHeader("Authorization", "Bearer " + hash.getString("access_token"))
            .putHeader("scope", String.join(",", oauth2Flow.scope.split("(\\s|,)")))
            .as(BodyCodec.jsonObject())
            .send(cr -> {
                client.close();
                if (cr.succeeded()) {
                    HttpResponse<JsonObject> response = cr.result();
                    if (response.statusCode() == HttpResponseStatus.OK.code()) {
                        final JsonObject body = response.body();
                        logger.finest("body: " + body.encodePrettily());
                        final JsonObject state = new JsonObject(
                            base64.decode(context.session().remove("encodedState")));
                        final JsonObject loginState = state.getJsonObject("loginState");
                        final String provider = loginState.getString("provider");
                        final String firstName = PROVIDER_MAPS.get(provider).get(FIRST_NAME);
                        final String lastName = PROVIDER_MAPS.get(provider).get(LAST_NAME);
                        final String displayName = PROVIDER_MAPS.get(provider).get(DISPLAY_NAME);
                        final String email = PROVIDER_MAPS.get(provider).get(EMAIL);
                        JsonObject account = new JsonObject();
                        body.getJsonObject("accounts")
                            .stream()
                            .filter(entry -> entry.getValue() instanceof JsonObject)
                            .limit(1)
                            .forEach(entry -> {
                                JsonObject values = (JsonObject) entry.getValue();
                                // use email as account ID
                                account.put("id", values.getString(email, UUID.randomUUID().toString()));
                                account.put(FIRST_NAME, values.getString(firstName, "Guest"));
                                account.put(LAST_NAME, values.getString(lastName, ""));
                                account.put(DISPLAY_NAME, values.getString(displayName, ""));
                                // generate displayName if it does not exists
                                if ("".equals(account.getString(DISPLAY_NAME, "").trim()))
                                    account.put(DISPLAY_NAME,
                                        (account.getString(FIRST_NAME) + " " + account.getString(LAST_NAME))
                                            .trim());
                            });
                        // create user
                        hash.put("scope", oauth2Flow.scope).put("loginState", loginState);
                        final AccessToken user = new AccessTokenImpl((OAuth2AuthProviderImpl) oauth2Provider, hash);
                        Session session = context.session();
                        if (session != null) {
                            // the user has upgraded from unauthenticated to authenticated
                            // session should be upgraded as recommended by owasp
                            session.regenerateId();
                        }
                        // set user account
                        user.principal().put("account", account);
                        // add user to the session
                        context.setUser(user);
                        // set user roles
                        getUserRoles(account, roles -> {
                            if (roles.succeeded()) {
                                // respond
                                logger.finest("oauth2 implicit flow user principal: "
                                        + context.user().principal().encodePrettily());
                                new JsonResponse(context).end(user.principal());
                            } else {
                                context.fail(roles.cause());
                            }
                        });
                    } else {
                        context.fail(ServerError.message("error while fetching user account"));
                    }
                } else {
                    context.fail(cr.cause());
                }
            });
    }

}
