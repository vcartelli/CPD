package it.beng.modeler.microservice;

import io.vertx.core.DeploymentOptions;
import it.beng.microservice.common.MicroServiceVerticle;
import it.beng.modeler.config;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class ModelerConfigVerticle extends MicroServiceVerticle {

    @Override
    public void start() {
        super.start();

        config.set(config());

        // deploy the rest api
        vertx.deployVerticle(RestAPIVerticle.class.getName(), new DeploymentOptions().setConfig(config()));




        // Publish the services in the discovery infrastructure.
//        publishMessageSource("api", "data", rec -> {
//            if (!rec.succeeded()) {
//                rec.cause().printStackTrace();
//            }
//            System.out.println("Modeler API service published: " + rec.succeeded());
//        });

//        publishHttpEndpoint("quotes", "localhost", config().getInteger("http.port", 8080), null, ar -> {
//            if (ar.failed()) {
//                ar.cause().printStackTrace();
//            } else {
//                System.out.println("Quotes (REST endpoint) service published : " + ar.succeeded());
//            }
//        });
    }

}
