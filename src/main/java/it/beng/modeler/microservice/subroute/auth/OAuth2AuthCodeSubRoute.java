package it.beng.modeler.microservice.subroute.auth;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import it.beng.microservice.db.MongoDB;
import it.beng.microservice.schema.SchemaTools;
import it.beng.modeler.config;
import it.beng.modeler.model.ModelTools;

import java.util.logging.Logger;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class OAuth2AuthCodeSubRoute extends OAuth2SubRoute {

    private static Logger logger = Logger.getLogger(OAuth2AuthCodeSubRoute.class.getName());

    public static final String FLOW_TYPE = "AUTH_CODE";

    public OAuth2AuthCodeSubRoute(Vertx vertx, Router router, MongoDB mongodb,
                                  SchemaTools schemaTools, ModelTools modelTools, config.OAuth2Config oauth2Config) {
        super(vertx, router, mongodb, schemaTools, modelTools, oauth2Config, FLOW_TYPE);
    }

    @Override
    protected void init() {

        // create OAuth2 handler
        OAuth2AuthHandler oAuth2Handler = OAuth2AuthHandler.create(oauth2Provider, config.oauth2.origin);
        for (String scope : oauth2Flow.scope.split("(\\s|,)"))
            oAuth2Handler.addAuthority(scope);
        oAuth2Handler.setupCallback(router.get(baseHref + "oauth2/server/callback"));
        router.route(HttpMethod.GET, path + "login/handler").handler(oAuth2Handler);
        router.route(HttpMethod.GET, path + "login/handler").handler(this::providerLoginHandler);

    }

    private static String getUserId(String[] encodedJsons) {
        String userId = null;
        for (String s : encodedJsons) {
            try {
                String decoded = base64.decode(s);
                JsonObject o = new JsonObject(decoded);
                userId = o.getString("sub");
                if (userId != null) {
                    logger.finest("userId found: " + userId);
                    break;
                }
            } catch (Exception e) {
                continue;
            }
        }
        return userId;
    }

    private void providerLoginHandler(RoutingContext rc) {

        final AccessToken user = (AccessToken) rc.user();

        if (user == null) {
            rc.next();
        } else {

            final String userId = getUserId(user.principal().getString("id_token").split("\\."));

            if (userId == null) throw new IllegalStateException("user-id not found in user principal!");
            user.principal().put("id", "userId");

            final JsonObject profile = new JsonObject().put("provider", oauth2Config.provider);
            user.principal().put("profile", profile);

            final String token = user.principal().getString("access_token");
            final WebClient client = WebClient.create(
                vertx,
                new WebClientOptions()
                    .setUserAgent("CPD-WebClient/1.0")
                    .setFollowRedirects(false));
            client.requestAbs(HttpMethod.GET, oauth2Flow.getUserProfile.replace("{userId}", userId))
                  .putHeader("Accept", "application/json")
                  .putHeader("Authorization", "Bearer " + token)
                  .as(BodyCodec.jsonObject())
                  .send(cr -> {
                      if (cr.succeeded()) {
                          HttpResponse<JsonObject> response = cr.result();
                          if (response.statusCode() == HttpResponseStatus.OK.code()) {
                              profile.mergeIn(response.body());
                          }
                      }
                      setUserRoles(rc, user.principal());
                      logger.finest("auth_code user principal: " + Json.encodePrettily(rc.user().principal()));
                      client.close();
                      rc.next();
                  });
        }

    }

}
