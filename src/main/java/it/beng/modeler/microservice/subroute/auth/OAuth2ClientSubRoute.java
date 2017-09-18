package it.beng.modeler.microservice.subroute.auth;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.web.Router;
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
public final class OAuth2ClientSubRoute extends OAuth2SubRoute {

    private static Logger logger = Logger.getLogger(OAuth2ClientSubRoute.class.getName());

    public static final String FLOW_TYPE = "CLIENT";

    public OAuth2ClientSubRoute(Vertx vertx, Router router, MongoDB mongodb,
                                SchemaTools schemaTools, ModelTools modelTools, config.OAuth2Config oAuth2Config) {
        super(vertx, router, mongodb, schemaTools, modelTools, oAuth2Config, FLOW_TYPE);
    }

    public static AccessToken getToken(Vertx vertx) {
        return vertx.getOrCreateContext().get(FLOW_TYPE + "_ACCESS_TOKEN");
    }

    public static void setToken(Vertx vertx, AccessToken accessToken) {
        vertx.getOrCreateContext().put(FLOW_TYPE + "_ACCESS_TOKEN", accessToken);
    }

    @Override
    protected void init() {
        JsonObject tokenConfig = new JsonObject()
            .put("client_id", oauth2Config.clientId)
            .put("client_secret", oauth2Config.clientSecret);
        oauth2Provider.getToken(tokenConfig, ar -> {
            if (ar.succeeded()) {
                AccessToken accessToken = ar.result();
                setToken(vertx, accessToken);
                logger.info("client access token correctly created: " + accessToken.principal().encodePrettily());
            } else {
                logger.warning("COULD NOT GET CLIENT TOKEN");
                ar.cause().printStackTrace();
            }
        });
    }

}
