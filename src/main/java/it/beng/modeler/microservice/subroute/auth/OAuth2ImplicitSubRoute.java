package it.beng.modeler.microservice.subroute.auth;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.impl.AccessTokenImpl;
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import it.beng.microservice.db.MongoDB;
import it.beng.microservice.schema.SchemaTools;
import it.beng.modeler.config;
import it.beng.modeler.microservice.subroute.AuthSubRoute;
import it.beng.modeler.model.ModelTools;

import static it.beng.modeler.microservice.subroute.AuthSubRoute.loginRedirect;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class OAuth2ImplicitSubRoute extends OAuth2SubRoute {

    public static final String FLOW_TYPE = "IMPLICIT";

    public OAuth2ImplicitSubRoute(Vertx vertx, Router router, MongoDB mongodb,
                                  SchemaTools schemaTools, ModelTools modelTools, config.OAuth2Config oAuth2Config) {
        super(vertx, router, mongodb, schemaTools, modelTools, oAuth2Config, FLOW_TYPE);
    }

    /*

        http://localhost:8901/cpd/oauth2/server/callback,
        https://simpatico.business-engineering.it/cpd/oauth2/server/callback,
        https://localhost:8901/cpd/oauth2/client/callback,
        http://localhost:8901/cpd/oauth2/client/callback,
        https://simpatico.business-engineering.it/cpd/oauth2/client/callback,
        https://localhost:8901/cpd/oauth2/server/callback

    */
    @Override
    protected void init() {
        router.route(HttpMethod.GET, path + "login/handler").handler(this::redirectUrlHandler);
        router.route(HttpMethod.GET, baseHref + "oauth2/client/callback").handler(rc -> {
            redirect(rc, config.server.appHref(rc) + "oauth2/client/callback");
        });
        router.route(HttpMethod.GET, path + "hash/:hash").handler(this::setupAccess);
    }

    private void redirectUrlHandler(RoutingContext rc) {
        StringBuilder url = new StringBuilder(
            oauth2ClientOptions.getSite() + oauth2ClientOptions.getAuthorizationPath());
        url.append("?");
        url.append("response_type=token");
        url.append("&");
        url.append("redirect_uri=");
        url.append(config.server.origin());
        url.append(baseHref + "oauth2/client/callback");
        url.append("&");
        url.append("client_id=");
        url.append(oauth2ClientOptions.getClientID());
        url.append("&");
        url.append("scope=");
        url.append(oauth2Flow.scope.replace(" ", ","));
        url.append("&");
        url.append("state=");
        url.append(AuthSubRoute.getBase64EncodedState(rc));
        redirect(rc, url.toString());
    }

    private void setupAccess(RoutingContext rc) {

        String encodedHash = rc.request().getParam("hash");

        JsonObject hash = new JsonObject(base64.decode(encodedHash));

        AuthSubRoute.checkState(rc, hash.getString("state"));

        final AccessToken user = new AccessTokenImpl((OAuth2AuthProviderImpl) oauth2Provider,
            hash.put("scope", oauth2Flow.scope));
        rc.setUser(user);
        final JsonObject profile = new JsonObject().put("provider", oauth2Config.provider);
        user.principal().put("profile", profile);

        final String token = hash.getString("access_token");
        WebClient client = WebClient.create(
            vertx,
            new WebClientOptions()
                .setUserAgent("CPD-WebClient/1.0")
                .setFollowRedirects(false)
        );
        client.requestAbs(HttpMethod.GET, oauth2Flow.getUserProfile)
              .putHeader("Accept", "application/json")
              .putHeader("Authorization", "Bearer " + token)
              .putHeader("scope", String.join(",", oauth2Flow.scope.split("(\\s|,)")))
              .as(BodyCodec.jsonObject())
              .send(cr -> {
                  if (cr.succeeded()) {
                      HttpResponse<JsonObject> response = cr.result();
                      if (response.statusCode() == HttpResponseStatus.OK.code()) {
                          profile.mergeIn(response.body());
                          // generate displayName if not exists
                          if (profile.getString("displayName") == null)
                              profile.put("displayName",
                                  (profile.getString("name", "") + " " +
                                      profile.getString("surname", "")).trim());
                      }
                  }
                  setUserRoles(rc, user.principal());
                  if (config.develop) System.out.println(Json.encodePrettily(rc.user().principal()));
                  client.close();
                  loginRedirect(rc);
              });
    }

}
