package it.beng.modeler.microservice.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import it.beng.microservice.db.MongoDB;
import it.beng.modeler.config;
import it.beng.modeler.microservice.actions.IncomingAction;
import it.beng.modeler.microservice.utils.EventBusUtils;

public abstract class ActionService extends BridgeEventService {

    protected final Logger logger = Logger.getLogger(this.getClass().getName());

    private static final Map<String, Class<? extends IncomingAction>> INCOMING_ACTIONS = new HashMap<>();

    protected static void registerIncomingAction(String type, Class<? extends IncomingAction> actionClass) {
        INCOMING_ACTIONS.put(type, actionClass);
    }

    protected static IncomingAction createIncomingAction(BridgeEvent event) {
        JsonObject json = event.getRawMessage().getJsonObject("body");
        if (json == null) {
            EventBusUtils.fail(event, "illegal state: no json found in message body");
            return null;
        }
        String type = json.getString("type");
        if (type == null) {
            EventBusUtils.fail(event, "illegal state: no type found in action json");
            return null;
        }
        Class<? extends IncomingAction> actionClass = INCOMING_ACTIONS.get(type);
        if (actionClass == null) {
            EventBusUtils.fail(event, "illegal state: no incoming action registered for type " + type);
            return null;
        }
        try {
            IncomingAction action = actionClass.getDeclaredConstructor(JsonObject.class).newInstance(json);
            if (!action.isValid()) {
                EventBusUtils.fail(event, "illegal state: action is invalid");
                return null;
            }
            return action;
        } catch (Exception e) {
            EventBusUtils.fail(event, e);
            return null;
        }
    }

    protected static final MongoDB mongodb = config.mongoDB();

    private final Pattern publishAddressPattern;

    public ActionService(Vertx vertx) {
        super(vertx);
        publishAddressPattern = Pattern.compile("^" + address() + "::[a-z0-9]{8}-(?:[a-z0-9]{4}-){3}[a-z0-9]{12}$");
    }

    protected abstract String address();

    protected Pattern publishAddressPattern() {
        return publishAddressPattern;
    }

    @Override
    protected void init() {
        vertx.eventBus().consumer(address(), (Message<JsonObject> message) -> {
            message.reply(message.body());
        });
    }

    @Override
    public Collection<PermittedOptions> inboundPermitted() {
        return Arrays.asList(
            new PermittedOptions().setAddress(address()),
            new PermittedOptions().setAddressRegex(publishAddressPattern().toString()));
    }

    @Override
    public Collection<PermittedOptions> outboundPermitted() {
        return inboundPermitted();
    }

    @Override
    public boolean handle(BridgeEvent event) {
        JsonObject message = event.getRawMessage();
        if (message == null) return false;
        String address = message.getString("address");
        if (address == null) return false;
        if (publishAddressPattern().matcher(address).matches()) {
            handlePublish(event);
            return true;
        } else if (address().equals(address)) {
            handleSend(event);
            return true;
        }
        return false;
    }

    private void handlePublish(BridgeEvent event) {
        switch (event.type()) {
            case PUBLISH:
                process(event);
                break;
            case SEND:
                EventBusUtils.fail(event, "illegal state: cannot send messages over a publish/register address");
                break;
            case RECEIVE:
            case REGISTER:
            case UNREGISTER:
            default:
                EventBusUtils.complete(event);
        }
    }

    private void handleSend(BridgeEvent event) {
        switch (event.type()) {
            case SEND:
                process(event);
                break;
            case PUBLISH:
                EventBusUtils.fail(event, "illegal state: cannot publish messages over a send/reply address");
                break;
            case REGISTER:
                EventBusUtils.fail(event, "illegal state: cannot register address over a send/reply address");
                break;
            case RECEIVE:
            case UNREGISTER:
            default:
                EventBusUtils.complete(event);
        }
    }

    private void process(BridgeEvent event) {
        createIncomingAction(event).handle(EventBusUtils.account(event), action -> {
            if (action.succeeded()) {
                event.getRawMessage().put("body", action.result());
                EventBusUtils.complete(event);
            } else {
                EventBusUtils.fail(event, action.cause());
            }
        });
    }

}
