package it.beng.modeler.microservice;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import it.beng.microservice.common.MicroServiceVerticle;
import it.beng.modeler.config;

import java.util.logging.Logger;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class ModelerConfigVerticle extends MicroServiceVerticle {

    private static Logger logger = Logger.getLogger(ModelerConfigVerticle.class.getName());

    @Override
    public void start() {
        super.start();
        vertx.executeBlocking(future -> config.set(vertx, config(), configSet -> {
            if (configSet.succeeded()) {
                vertx.deployVerticle(new ModelerServerVerticle(), new DeploymentOptions().setConfig(config()),
                    complete -> {
                        if (complete.succeeded()) {
                            logger.info("Succesfully deployed ModelerServerVerticle: " + complete.result());
                            future.complete();
                        } else {
                            logger.severe("Cannot deploy ModelerServerVerticle: " + complete.cause().getMessage());
                            future.fail(complete.cause());
                        }
                    });
            } else {
                future.fail(configSet.cause());
            }
        }), false, done -> {
            if (done.failed()) {
                throw new IllegalStateException(done.cause());
            }
        });

        // config.set(this.vertx, config(), configSet -> {
        //     if (configSet.succeeded()) {
        //         vertx.deployVerticle(new ModelerServerVerticle(), new DeploymentOptions().setConfig(config()), ar -> {
        //             if (ar.succeeded()) {
        //                 logger.info("Succesfully deployed ModelerServerVerticle: " + ar.result());
        //             } else {
        //                 logger.severe("Cannot deploy ModelerServerVerticle: " + ar.cause().getMessage());
        //             }
        //         });

        //         // Publish the services in the discovery infrastructure.
        //         /*
        //         publishMessageSource("api", "data", rec -> {
        //             if (!rec.succeeded()) {
        //                 rec.cause().printStackTrace();
        //             }
        //             System.out.println("Modeler API service published: " + rec.succeeded());
        //         });

        //         publishHttpEndpoint("quotes", "localhost", config().getInteger("http.port", 8080), null, ar -> {
        //             if (ar.failed()) {
        //                 ar.cause().printStackTrace();
        //             } else {
        //                 System.out.println("Quotes (REST endpoint) service published : " + ar.succeeded());
        //             }
        //         });
        //         */
        //     } else {
        //         throw new IllegalStateException(configSet.cause());
        //     }
        // });

    }

    @Override
    public void stop(Future<Void> future) throws Exception {
        logger.info("Shutting down server...");
        super.stop(future);
    }
}
