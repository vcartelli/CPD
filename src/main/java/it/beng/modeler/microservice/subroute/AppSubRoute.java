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
public final class AppSubRoute extends VoidSubRoute {

    public AppSubRoute(Vertx vertx, Router router, MongoDB mongodb, SchemaTools schemaTools, ModelTools modelTools) {
        super(config.app.path, vertx, router, mongodb, schemaTools, modelTools);
    }

    @Override
    protected void init() {
        // let the application handle it's own routes
        for (String route : config.app.routes) {
            router.route(HttpMethod.GET, path + ":locale/" + route).handler(rc -> {
                String locale = rc.request().getParam("locale");
                if (config.develop) System.out.println("rerouting " + path + locale + "/" + route + " to app");
                rc.reroute(path + locale);
            });
            System.out.println(path + ":locale/" + route + " will be managed by root web application");
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
