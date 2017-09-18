package it.beng.modeler.microservice;

import io.vertx.core.DeploymentOptions;
import it.beng.microservice.common.MicroServiceVerticle;
import it.beng.modeler.config;

import java.util.logging.Level;
import java.util.logging.LogManager;
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

        config.set(config());
        if (config.develop) LogManager.getLogManager().getLogger("it.beng").setLevel(Level.FINEST);

        super.start();

        vertx.deployVerticle(new ModelerServerVerticle(), new DeploymentOptions().setConfig(config()), ar -> {
                if (ar.succeeded()) {
                    logger.info("Succesfully deployed ModelerServerVerticle: " + ar.result());
                } else {
                    logger.severe("Cannot deploy ModelerServerVerticle: " + ar.cause().getMessage());
                }
            }
        );

        // Publish the services in the discovery infrastructure.
/*
        publishMessageSource("api", "data", rec -> {
            if (!rec.succeeded()) {
                rec.cause().printStackTrace();
            }
            System.out.println("Modeler API service published: " + rec.succeeded());
        });

        publishHttpEndpoint("quotes", "localhost", config().getInteger("http.port", 8080), null, ar -> {
            if (ar.failed()) {
                ar.cause().printStackTrace();
            } else {
                System.out.println("Quotes (REST endpoint) service published : " + ar.succeeded());
            }
        });
*/
    }

}
