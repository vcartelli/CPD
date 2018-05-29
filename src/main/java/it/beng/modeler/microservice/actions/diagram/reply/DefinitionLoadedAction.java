package it.beng.modeler.microservice.actions.diagram.reply;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.modeler.config;
import it.beng.modeler.microservice.actions.ReplyAction;
import it.beng.modeler.microservice.actions.diagram.DiagramAction;

import java.util.Map;
import java.util.logging.Logger;

public class DefinitionLoadedAction extends ReplyAction implements DiagramAction {
    private static Logger logger = Logger.getLogger(DefinitionLoadedAction.class.getName());
    public static final String TYPE = "[Diagram Action Reply] Definition Loaded";

    public DefinitionLoadedAction(JsonObject definition) {
        super(new JsonObject()
            .put("address", ADDRESS)
            .put("type", TYPE)
            .put("definition", definition));
        if (isValid()) {
/*
            export interface DiagramDefinition<N extends Notation = Notation> {
                id: type$uuid;
                diagram: Diagram<N>;
                plane: Di.Plane;
                elements: Di.Depiction[];
                root: Root<N>;
                childs: Child<N>[];
            }
*/
            updateDomainCollections("diagram", "models");
            updateDomainCollections("plane", "diagrams");
            updateDomainCollections("elements", "diagrams");
            updateDomainCollections("root", "models");
            updateDomainCollections("childs", "models");
        }
    }

    @Override
    public boolean isValid() {
        return super.isValid() && definition() != null;
    }

    public JsonObject definition() {
        return json.getJsonObject("definition");
    }

    private static void updateDC(String $domain, String collection) {
        final Map<String, String> dc = config.DOMAIN_COLLECTIONS;
        if (!dc.containsKey($domain)) {
            dc.put($domain, collection);
            logger.finest("added domain collection { \"" + $domain + "\": \"" + collection + "\" }");
        }
    }

    private void updateDomainCollections(String ddField, String collection) {
        final Object ddValue = definition().getValue(ddField);
        if (ddValue instanceof JsonObject) {
            updateDC(((JsonObject) ddValue).getString("$domain"), collection);
        } else if (ddValue instanceof JsonArray) {
            ((JsonArray) ddValue).stream()
                                 .filter(item -> item instanceof JsonObject)
                                 .map(item -> (JsonObject) item)
                                 .forEach(item -> {
                                     updateDC(item.getString("$domain"), collection);
                                 });
        }
    }
}
