package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import it.beng.modeler.config;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class AppSubRoute extends VoidSubRoute {

    public AppSubRoute(Vertx vertx, Router router) {
        super("" /* config.app.path */, vertx, router, false);
    }

    @Override
    protected void init() {
        for (String locale : config.app.locales) {
            /* STATIC RESOURCES (CPD app) */
            router.route(HttpMethod.GET, path + locale + "/*")
                  .handler(StaticHandler.create("web/ROOT/" + locale)
                                        .setDirectoryListing(false)
                                        .setAllowRootFileSystemAccess(false)
                                        .setAlwaysAsyncFS(true)
                                        .setCachingEnabled(true)
                                        .setFilesReadOnly(true));
            // let the application handle also it's own dynamic routes
            router.route(HttpMethod.GET, path + locale + "/*").handler(context -> {
                context.reroute(path + locale);
            });
        }
    }
}
