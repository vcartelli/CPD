package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class RootSubRoute extends SubRoute {

    public RootSubRoute(Vertx vertx, Router router, MongoClient mongodb) {
        super(vertx, router, mongodb);
    }

    @Override
    protected void init() {

        // let the ROOT application handle /diagram/* calls
        router.get("/diagram/*").handler(rc -> {
            rc.reroute("/");
        });
        // let the ROOT application handle /login calls
        router.get("/login*").handler(rc -> {
            rc.reroute("/");
        });

        // /*
        router.get("/*").handler(StaticHandler.create("web/ROOT")
                                              .setDirectoryListing(false)
                                              .setAllowRootFileSystemAccess(false)
                                              .setAlwaysAsyncFS(true)
                                              .setCachingEnabled(true)
                                              .setFilesReadOnly(true)
        );

    }
}
