package it.beng.modeler.microservice.actions.diagram.publish;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.common.Counter;
import it.beng.modeler.microservice.actions.diagram.DiagramAction;
import it.beng.modeler.microservice.actions.diagram.DiagramPublishAction;
import it.beng.modeler.microservice.utils.QueryUtils;

import java.util.List;

public abstract class EditorAction extends DiagramPublishAction implements DiagramAction {

    public EditorAction(JsonObject action) {
        super(action);
    }

    @Override
    public boolean isValid() {
        return super.isValid() && items() != null;
    }

    @Override
    public void handle(JsonObject account, Handler<AsyncResult<JsonObject>> handler) {
        QueryUtils.team(diagramId(), team -> {
            if (team.succeeded()) {
                DiagramAction.isPermitted(account, team.result(), "editor", isPermitted -> {
                    if (isPermitted.succeeded()) {
                        if (isPermitted.result()) {
                            final Counter counter = new Counter(this.items());
                            this.items().forEach(item -> this.forEach(item, done -> {
                                if (done.failed()) {
                                    handler.handle(Future.failedFuture(done.cause()));
                                    throw new RuntimeException(done.cause());
                                }
                                if (!counter.next()) {
                                    handler.handle(Future.succeededFuture(json));
                                }
                            }));
                        } else {
                            handler.handle(Future.failedFuture("unauthorized"));
                        }
                    } else {
                        handler.handle(Future.failedFuture(isPermitted.cause()));
                    }
                });
            } else handler.handle(Future.failedFuture(team.cause()));
        });
    }

    protected abstract List<JsonObject> items();

    protected abstract void forEach(JsonObject item, Handler<AsyncResult<Void>> handler);
}
