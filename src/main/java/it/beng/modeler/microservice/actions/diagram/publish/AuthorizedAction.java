package it.beng.modeler.microservice.actions.diagram.publish;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.common.Countdown;
import it.beng.modeler.microservice.actions.diagram.DiagramAction;
import it.beng.modeler.microservice.actions.diagram.DiagramPublishAction;
import it.beng.modeler.microservice.utils.AuthUtils;
import it.beng.modeler.microservice.utils.DBUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class AuthorizedAction extends DiagramPublishAction implements DiagramAction {

    private static final Collection<String> roles = Arrays.asList("owner", "reviewer", "editor");

    AuthorizedAction(JsonObject action) {
        super(action);
    }

    @Override
    public boolean isValid() {
        return super.isValid() && items() != null;
    }

    @Override
    public void handle(RoutingContext context, Handler<AsyncResult<JsonObject>> handler) {
        DBUtils.team(diagramId(), team -> {
            if (team.succeeded()) {
                DiagramAction.isPermitted(AuthUtils.getAccount(context), team.result(), roles, isPermitted -> {
                    if (isPermitted.succeeded()) {
                        if (isPermitted.result()) {
                            final Countdown countdown = new Countdown(this.items()).setCompleteHandler(zero -> {
                                handler.handle(Future.succeededFuture(json));
                            });
                            this.items().forEach(item -> this.forEach(item, done -> {
                                if (done.failed()) {
                                    handler.handle(Future.failedFuture(done.cause()));
                                    throw new RuntimeException(done.cause());
                                }
                                countdown.next();
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
