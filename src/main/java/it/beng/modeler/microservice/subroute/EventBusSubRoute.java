package it.beng.modeler.microservice.subroute;

import java.util.logging.Logger;

import io.vertx.core.Vertx;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import it.beng.modeler.config;
import it.beng.modeler.microservice.services.BridgeEventService;
import it.beng.modeler.microservice.services.DiagramActionService;
import it.beng.modeler.microservice.utils.EventBusUtils;;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class EventBusSubRoute extends VoidSubRoute {

    static Logger logger = Logger.getLogger(EventBusSubRoute.class.getName());

    static {
        // register the bridge event services here.
        // NOTE: insertion order is used for handler evaluation
        BridgeEventService.registerService(DiagramActionService.class);
    }

    public EventBusSubRoute(Vertx vertx, Router router) {
        super(config.server.eventBus.path, vertx, router, false);
    }

    @Override
    protected void init() {

        // start the bridge event registered services
        BridgeEventService.start(vertx);

        BridgeOptions bridgeOptions = new BridgeOptions();
        for (BridgeEventService service : BridgeEventService.services()) {
            for (PermittedOptions permitted : service.inboundPermitted())
                bridgeOptions.addInboundPermitted(permitted);
            for (PermittedOptions permitted : service.outboundPermitted())
                bridgeOptions.addOutboundPermitted(permitted);
        }

        SockJSHandler sockJSHandler = SockJSHandler.create(vertx, new SockJSHandlerOptions().setInsertJSESSIONID(true))
            .bridge(bridgeOptions, event -> {
                EventBusUtils.log(event);
                boolean handled = false;
                for (BridgeEventService service : BridgeEventService.services()) {
                    if (service.handle(event)) {
                        handled = true;
                        break;
                    }
                }
                if (!handled) {
                    event.complete(true);
                }
            });

        router.route(path + "*").handler(sockJSHandler);

        logger.info("Event Bus correctly created");
    }
}
