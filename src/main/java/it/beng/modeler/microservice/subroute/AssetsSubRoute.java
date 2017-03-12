package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import it.beng.modeler.config;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class AssetsSubRoute extends SubRoute {

    public AssetsSubRoute(Vertx vertx, Router router, MongoClient mongodb) {
        super(config.ASSETS_PATH, vertx, router, mongodb);
    }

    @Override
    protected void init() {
        router.route(HttpMethod.GET, path + "*").handler(
            StaticHandler.create("web/assets")
                         .setDirectoryListing(config.server.assets.allowListing));
    }

}
