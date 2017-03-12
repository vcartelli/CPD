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
public final class RootSubRoute extends SubRoute {

    public RootSubRoute(Vertx vertx, Router router, MongoClient mongodb) {
        super("", vertx, router, mongodb);
    }

    @Override
    protected void init() {
        // let the ROOT application handle it's own routes
        for (String route : config.webapp.routes) {
            router.route(HttpMethod.GET, path + route).handler(rc -> {
                rc.reroute(path);
            });
        }
        router.route(HttpMethod.GET, path + "*").handler(StaticHandler.create("web/ROOT")
                                                                      .setDirectoryListing(false)
                                                                      .setAllowRootFileSystemAccess(false)
                                                                      .setAlwaysAsyncFS(true)
                                                                      .setCachingEnabled(true)
                                                                      .setFilesReadOnly(true));
    }
}
