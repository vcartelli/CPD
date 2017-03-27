package it.beng.modeler.microservice.subroute.auth;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import it.beng.modeler.config;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class OAuth2ClientSubRoute extends OAuth2SubRoute {

    public static final String FLOW_TYPE = "CLIENT";

    public OAuth2ClientSubRoute(Vertx vertx, Router router, MongoClient mongodb, config.OAuth2Config oAuth2Config) {
        super(vertx, router, mongodb, oAuth2Config, FLOW_TYPE);
    }

    public static AccessToken getToken(Vertx vertx) {
        return vertx.getOrCreateContext().get(FLOW_TYPE + "_ACCESS_TOKEN");
    }

    public static void setToken(Vertx vertx, AccessToken accessToken) {
        vertx.getOrCreateContext().put(FLOW_TYPE + "_ACCESS_TOKEN", accessToken);
    }

    @Override
    protected void oauth2Init() {
        JsonObject tokenConfig = new JsonObject()
            .put("client_id", oauth2Config.clientId)
            .put("client_secret", oauth2Config.clientSecret);
        oauth2Provider.getToken(tokenConfig, ar -> {
            if (ar.succeeded()) {
                AccessToken accessToken = ar.result();
                setToken(vertx, accessToken);
                System.out
                    .println("client access token correctly created: " + accessToken.principal().encodePrettily());
            } else {
                System.err.println("COULD NOT GET CLIENT TOKEN");
                ar.cause().printStackTrace();
            }
        });
    }

}
