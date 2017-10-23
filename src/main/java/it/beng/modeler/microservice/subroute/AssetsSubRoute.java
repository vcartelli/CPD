package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import it.beng.microservice.db.MongoDB;
import it.beng.microservice.schema.SchemaTools;
import it.beng.modeler.config;
import it.beng.modeler.model.ModelTools;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class AssetsSubRoute extends VoidSubRoute {

    public AssetsSubRoute(Vertx vertx, Router router, MongoDB mongodb, SchemaTools schemaTools, ModelTools modelTools) {
        super(config.app.path + config.ASSETS_PATH, vertx, router, mongodb, schemaTools, modelTools);
    }

    @Override
    protected void init() {
        router.route(HttpMethod.GET, path + "*").handler(
            StaticHandler.create("web/assets")
                         .setDirectoryListing(config.server.assets.allowListing)
        );
    }

}
