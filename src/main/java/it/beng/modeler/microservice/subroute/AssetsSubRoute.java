package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
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
        super(vertx, router, mongodb);
    }

    @Override
    protected void init() {
        String assets = config.server.assets.base;
        router.get(assets + "/*").handler(
            StaticHandler.create("web/assets")
//                         .setWebRoot("/")
                         .setDirectoryListing(config.server.assets.allowListing));
    }

}
