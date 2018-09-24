package it.beng.modeler.microservice.subroute;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.common.Counter;
import it.beng.microservice.db.DeleteResult;
import it.beng.modeler.config;
import it.beng.modeler.microservice.http.JsonResponse;
import it.beng.modeler.microservice.utils.QueryUtils;
import it.beng.modeler.model.Domain;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public final class CollaborationsSubRoute extends VoidSubRoute {

    private static Logger logger = Logger.getLogger(CollaborationsSubRoute.class.getName());

    public static final String PATH = "collaborations/";

    private final static FindOptions COLLABORATION_FIELDS = new FindOptions().setFields(new JsonObject()
        .put("id", 1)
        .put("language", 1)
        .put("name", 1)
        .put("documentation", 1)
        .put("team", 1)
        .put("$domain", 1)
    );

    public CollaborationsSubRoute(Vertx vertx, Router router) {
        super(PATH, vertx, router, false);
    }

    @Override
    protected void init() {
        router.route(HttpMethod.GET, path).handler(this::get);
        router.route(HttpMethod.GET, path + ":id").handler(this::get);
        router.route(HttpMethod.PUT, path + ":id").handler(this::put);
        router.route(HttpMethod.POST, path + "new").handler(this::post);
        router.route(HttpMethod.DELETE, path + ":id").handler(this::delete);
    }

    private void get(RoutingContext context) {
        final String id = context.pathParam("id");
        final JsonObject idQuery = new JsonObject();
        if (id != null) {
            idQuery.put("id", id);
        }
        final Domain diagramDomain = Domain.ofDefinition(Domain.Definition.DIAGRAM);
        final String collection = diagramDomain.getCollection();
        final JsonObject query = QueryUtils.and(Arrays.asList(diagramDomain.getQuery(), idQuery));
        mongodb.findWithOptions(collection, query, COLLABORATION_FIELDS, find -> {
            if (find.succeeded()) {
                new JsonResponse(context).end(find.result());
            } else {
                context.fail(find.cause());
            }
        });
    }

    private void put(RoutingContext context) {
        if (isAdminFailOtherwise(context)) {
            final String id = context.pathParam("id");
            if (id == null) {
                context.fail(new NullPointerException("no id"));
                return;
            }
            final JsonObject body = context.getBodyAsJson();
            if (body == null || body.isEmpty()) {
                context.fail(new NullPointerException("no body"));
                return;
            }
            final JsonObject idQuery = new JsonObject()
                .put("id", id);
            final Domain diagramDomain = Domain.ofDefinition(Domain.Definition.DIAGRAM);
            final String collection = diagramDomain.getCollection();
            final JsonObject query = QueryUtils.and(Arrays.asList(diagramDomain.getQuery(), idQuery));
            final JsonObject update = new JsonObject().put("$set", body);
            mongodb.findOneAndUpdate(collection, query, update, find -> {
                if (find.succeeded()) {
                    new JsonResponse(context).end(find.result());
                } else {
                    context.fail(find.cause());
                }
            });
        }
    }

    private void post(RoutingContext context) {
        if (isCivilServantFailOtherwise(context)) {
            final JsonArray userIdArray = new JsonArray().add(
                context.user().principal().getJsonObject("account").getString("id")
            );
            final String language = config.language(context);
            final JsonObject now = mongoDateTime(OffsetDateTime.now());

            final JsonObject newDiagram = new JsonObject();
            final JsonObject newPlane = new JsonObject();
            final JsonObject newRoot = new JsonObject();
            final JsonObject newRootShape = new JsonObject();
            final JsonObject newChild = new JsonObject();
            final JsonObject newChildShape = new JsonObject();
/*
{
    "_id" : "b2892ad2-997d-4ab7-a49a-0ae6dab1adf3",
    "notation" : "Model.Notation.FPMN",
    "version" : 1.0,
    "created" : ISODate("2016-01-01T00:00:00.000+0000"),
    "lastModified" : ISODate("2018-01-01T12:00:00.000+0000"),
    "language" : "italian",
    "name" : "Ammissione al servizio di nido d’infanzia",
    "documentation" : "Procedura per l'ammissione al servizio di nido d’infanzia.",
    "team" : {
        "owner" : [
            "civil servant 1"
        ],
        "reviewer" : [
            "civil servant 1"
        ],
        "editor" : [
            "25204"
        ],
        "observer" : [
            "citizen 1",
            "citizen 2"
        ]
    },
    "＄domain" : "Model.FPMN.Diagram"
}
*/
            newDiagram
                .put("id", UUID.randomUUID().toString())
                .put("notation", "Model.Notation.FPMN")
                .put("version", 1)
                .put("created", now)
                .put("lastModified", now)
                .put("language", language)
                .put("name", "New Diagram")
                .put("team", new JsonObject()
                    .put("owner", userIdArray)
                    .put("reviewer", userIdArray)
                    .put("editor", userIdArray)
                )
                .put("$domain", "Model.FPMN.Diagram");
/*
{
    "_id" : "b2892ad2-997d-4ab7-a49a-0ae6dab1adf3",
    "modelId" : "b2892ad2-997d-4ab7-a49a-0ae6dab1adf3",
    "unit" : "px",
    "bounds" : {
        "x" : 0.0,
        "y" : 0.0,
        "width" : 820.0,
        "height" : 360.0
    },
    "＄domain" : "Di.Plane"
}
*/
            newPlane
                .put("id", UUID.randomUUID().toString())
                .put("modelId", newDiagram.getString("id"))
                .put("unit", "px")
                .put("bounds", new JsonObject()
                    .put("x", 0.0)
                    .put("y", 0.0)
                    .put("width", 300.0)
                    .put("height", 240.0)
                )
                .put("$domain", "Di.Plane");

/*
{
    "_id" : "c9561247-d5a7-4578-98b9-58021ee68ae0",
    "designId" : "b2892ad2-997d-4ab7-a49a-0ae6dab1adf3",
    "parentId" : "b2892ad2-997d-4ab7-a49a-0ae6dab1adf3",
    "category" : "Model.FPMN.Procedure.Category.Childhood",
    "language" : "italian",
    "name" : "Ammissione al servizio di nido d’infanzia",
    "documentation" : "Il servizio di asilo nido si propone di offrire il servizio di asilo nido per bambini da 0 a tre anni di età. Il servizio giornaliero viene erogato presso un centro dove verranno creati programmi di istruzione e di assistenza attorno alle esigenze di sviluppo, gli interessi e l'esperienza di ogni bambino. Questa procedura gestisce il processo di richiesta di ammissione.\nPossono presentare domanda di ammissione ai nidi d’infanzia comunali i genitori, tutori o affidatari di bambini e bambine residenti nel Comune di Trento. Il bambino deve risultare residente con almeno un genitore.\nLa domanda di ammissione può essere presentata dal momento in cui il bambino/la bambina risulta iscritto/a all’anagrafe del Comune o qualora sia già stata presentata dichiarazione di cambio residenza. Solo per i bambini nati nel mese di aprile la domanda può essere presentata dalla data di nascita, purché la madre risulti residente nel Comune di Trento.\nLa domanda di ammissione di un bambino o una bambina in affidamento familiare, anche non residente nel Comune di Trento, può essere accolta solo qualora risulti residente la famiglia affidataria.",
    "mission" : "Rispondere alla richiesta di nido da parte delle famiglie attraverso la gestione della domanda, l’assegnazione dei posti e la gestione della frequenza sulla base dei criteri individuati.",
    "＄domain" : "Model.FPMN.Procedure"
}
*/
            newRoot
                .put("id", UUID.randomUUID().toString())
                .put("designId", newDiagram.getString("id"))
                .put("language", language)
                .put("name", "New Procedure")
                .put("$domain", "Model.FPMN.Procedure");
/*
{
    "_id" : "a46c29cc-5814-47d0-86a9-22f6d678335a",
    "modelId" : "a46c29cc-5814-47d0-86a9-22f6d678335a",
    "planeId" : "b2892ad2-997d-4ab7-a49a-0ae6dab1adf3",
    "label" : {
        "bounds" : {
            "x" : 40.0,
            "y" : 80.0,
            "width" : 220.0,
            "height" : 120.0
        }
    },
    "bounds" : {
        "x" : 40.0,
        "y" : 80.0,
        "width" : 220.0,
        "height" : 280.0
    },
    "＄domain" : "Di.Shape"
}
*/
            newRootShape
                .put("id", UUID.randomUUID().toString())
                .put("modelId", newRoot.getString("id"))
                .put("planeId", newPlane.getString("id"))
                .put("label", new JsonObject()
                    .put("bounds", new JsonObject()
                        .put("x", 40.0)
                        .put("y", 40.0)
                        .put("width", 220.0)
                        .put("height", 40.0)
                    )
                )
                .put("bounds", new JsonObject()
                    .put("x", 40.0)
                    .put("y", 40.0)
                    .put("width", 220.0)
                    .put("height", 160.0)
                )
                .put("$domain", "Di.Shape");
/*
{
    "_id" : "a46c29cc-5814-47d0-86a9-22f6d678335a",
    "designId" : "b2892ad2-997d-4ab7-a49a-0ae6dab1adf3",
    "parentId" : "c9561247-d5a7-4578-98b9-58021ee68ae0",
    "nextPhaseId" : "05c18b01-cd5e-4b0a-b003-242f968f68df",
    "language" : "italian",
    "name" : "Richiesta di ammissione",
    "documentation" : "Il cittadino (di solito un genitore) compila il modulo di iscrizione al servizio di richiesta di asilo nido prima di una scadenza specifica. I termini di presentazione delle domande di ammissione ai nidi d’infanzia comunali sono fissati dal 1 settembre al 30 aprile precedenti il periodo di erogazione del servizio (indicativamente da inizio settembre a fine luglio). Il termine finale che cada in giorno festivo o comunque di chiusura degli uffici, è prorogato al primo giorno lavorativo successivo.",
    "＄domain" : "Model.FPMN.Phase"
}
*/
            newChild
                .put("id", UUID.randomUUID().toString())
                .put("designId", newDiagram.getString("id"))
                .put("parentId", newRoot.getString("id"))
                .put("language", language)
                .put("name", "New Phase")
                .put("$domain", "Model.FPMN.Phase");

            newChildShape
                .put("id", UUID.randomUUID().toString())
                .put("modelId", newChild.getString("id"))
                .put("planeId", newPlane.getString("id"))
                .put("label", new JsonObject()
                    .put("bounds", new JsonObject()
                        .put("x", 40.0)
                        .put("y", 80.0)
                        .put("width", 220.0)
                        .put("height", 120.0)
                    )
                )
                .put("bounds", new JsonObject()
                    .put("x", 40.0)
                    .put("y", 80.0)
                    .put("width", 220.0)
                    .put("height", 120.0)
                )
                .put("$domain", "Di.Shape");

            mongodb.save(Domain.Collection.MODELS, newDiagram, diagramSaved -> {
                if (diagramSaved.succeeded()) {
                    mongodb.save(Domain.Collection.DIS, newPlane, planeSaved -> {
                        if (planeSaved.succeeded()) {
                            mongodb.save(Domain.Collection.MODELS, newRoot, rootSaved -> {
                                if (rootSaved.succeeded()) {
                                    mongodb.save(Domain.Collection.DIS, newRootShape, rootShapeSaved -> {
                                        if (rootShapeSaved.succeeded()) {
                                            mongodb.save(Domain.Collection.MODELS, newChild, childSaved -> {
                                                if (childSaved.succeeded()) {
                                                    mongodb
                                                        .save(Domain.Collection.DIS, newChildShape, childShapeSaved -> {
                                                            if (childShapeSaved.succeeded()) {
                                                                new JsonResponse(context)
                                                                    .end(newDiagram.getString("id"));
                                                            } else context.fail(childShapeSaved.cause());
                                                        });

                                                } else context.fail(childSaved.cause());
                                            });

                                        } else context.fail(rootShapeSaved.cause());
                                    });

                                } else context.fail(rootSaved.cause());
                            });

                        } else context.fail(planeSaved.cause());
                    });

                } else context.fail(diagramSaved.cause());
            });
        }
    }

    private void delete(RoutingContext context) {
        if (isAdminFailOtherwise(context)) {
            final String id = context.pathParam("id");
            if (id == null) {
                context.fail(new NullPointerException("no id"));
                return;
            }
            final JsonObject idQuery = new JsonObject().put("id", id);
            final Domain diagramDomain = Domain.ofDefinition(Domain.Definition.DIAGRAM);
            final String diagramCollection = diagramDomain.getCollection();
            final JsonObject diagramQuery = QueryUtils.and(Arrays.asList(diagramDomain.getQuery(), idQuery));
            mongodb.findOneAndDelete(diagramCollection, diagramQuery, deleteDiagram -> {
                if (deleteDiagram.succeeded()) {
                    if (deleteDiagram.result() == null) {
                        context.fail(HttpResponseStatus.NOT_FOUND.code());
                        return;
                    }
                    final JsonArray removed = new JsonArray()
                        .add(new DeleteResult(diagramCollection, 1).toJson());
                    final Counter counter = new Counter(2);
                    mongodb.removeDocuments(Domain.Collection.MODELS,
                        new JsonObject().put("designId", id), removeModels -> {
                            if (removeModels.succeeded()) {
                                removed.add(removeModels.result().toJson());
                            }
                            if (!counter.next()) new JsonResponse(context).end(removed);
                        });
                    mongodb.findOneAndDelete(Domain.Collection.DIS, new JsonObject()
                        .put("modelId", id), deletePlane -> {
                        if (deletePlane.succeeded()) {
                            removed.add(new DeleteResult(Domain.Collection.DIS, 1).toJson());
                            JsonObject plane = deletePlane.result();
                            mongodb.removeDocuments(Domain.Collection.DIS,
                                new JsonObject().put("planeId", plane.getString("id")), removeDIs -> {
                                    if (removeDIs.succeeded()) {
                                        removed.add(removeDIs.result().toJson());
                                    }
                                    if (!counter.next()) new JsonResponse(context).end(removed);
                                });
                        } else if (!counter.next()) new JsonResponse(context).end(removed);
                    });
                } else context.fail(deleteDiagram.cause());
            });
        }
    }

}
