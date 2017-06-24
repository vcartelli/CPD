package it.beng.modeler.microservice.subroute;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import it.beng.microservice.db.MongoDB;
import it.beng.microservice.schema.SchemaTools;
import it.beng.modeler.model.ModelTools;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public abstract class VoidSubRoute extends SubRoute<Void> {

    public VoidSubRoute(String path, Vertx vertx, Router router, MongoDB mongodb,
                        SchemaTools schemaTools, ModelTools modelTools) {
        super(path, vertx, router, mongodb, schemaTools, modelTools, null);
    }

    protected abstract void init();

    @Override
    protected void init(Void userData) {
        init();
    }

}
