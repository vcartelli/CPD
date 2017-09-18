package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
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
public final class AppSubRoute extends VoidSubRoute {

    private static Logger logger = Logger.getLogger(AppSubRoute.class.getName());

    public AppSubRoute(Vertx vertx, Router router, MongoDB mongodb, SchemaTools schemaTools, ModelTools modelTools) {
        super(config.app.path, vertx, router, mongodb, schemaTools, modelTools);
    }

    @Override
    protected void init() {
        // let the application handle it's own routes
        for (String route : config.app.routes) {
            router.route(HttpMethod.GET, path + ":locale/" + route).handler(rc -> {
                String locale = rc.request().getParam("locale");
                logger.finest("rerouting " + path + locale + "/" + route + " to app");
                rc.reroute(path + locale);
            });
            logger.info(path + ":locale/" + route + " will be managed by root web application");
        }

        /*** STATIC RESOURCES (swagger-ui) ***/

        for (String locale : config.app.locales) {
            router.route(HttpMethod.GET, path + locale + "/*").handler(StaticHandler.create("web/ROOT/" + locale)
                                                                                    .setDirectoryListing(false)
                                                                                    .setAllowRootFileSystemAccess(false)
                                                                                    .setAlwaysAsyncFS(true)
                                                                                    .setCachingEnabled(true)
                                                                                    .setFilesReadOnly(true));
        }
    }
}
