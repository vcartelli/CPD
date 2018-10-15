package it.beng.modeler.model;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class ModelTools {

    // public static BiMap<String, String> JSON_ENTITY_TO_MONGO_DB = HashBiMap.create(
    //     new LinkedHashMap<String, String>() {{
    //         put("id", "_id");
    //     }}
    // );

    // private final Vertx vertx;
    // private final MongoDB mongodb;
    // private final SchemaTools schemaTools;
    // private final boolean debugLog;

    // public ModelTools(Vertx vertx, MongoDB mongodb, SchemaTools schemaTools, boolean debugLog) {
    //     this.vertx = vertx;
    //     this.mongodb = mongodb;
    //     this.schemaTools = schemaTools;
    //     this.debugLog = debugLog;
    // }

    // public String getDiagramRootId(JsonObject entity) {
    //     if ("diagram.Root".equals(entity.getString("class")))
    //         return entity.getString("id");
    //     else
    //         return entity.getString("rootId");
    // }

    // public void getEntity(String id, String collection, Handler<AsyncResult<JsonObject>> handler) {
    //     JsonObject query = new JsonObject().put("id", id);
    //     mongodb.findOne(collection, query, new JsonObject(), findOne -> {
    //         if (findOne.succeeded()) {
    //             handler.handle(Future.succeededFuture(findOne.result()));
    //         } else {
    //             handler.handle(Future.failedFuture(findOne.cause()));
    //         }
    //     });
    // }

    // public void saveEntity(JsonObject entity, String collection, Handler<AsyncResult<SaveResult>> handler) {
    //     schemaTools.validate(schemaTools.absRef(entity.getString("class")), entity, validate -> {
    //         if (validate.succeeded()) {
    //             if (validate.result().isValid()) {
    //                 mongodb.save(collection, entity, save -> {
    //                     if (save.succeeded()) {
    //                         handler.handle(Future.succeededFuture(new SaveResult<>(validate.result(), save.result())));
    //                     } else
    //                         handler.handle(Future.failedFuture(save.cause()));
    //                 });
    //             } else
    //                 handler.handle(Future.succeededFuture(new SaveResult<>(validate.result(), entity)));
    //         } else
    //             handler.handle(Future.failedFuture(validate.cause()));
    //     });
    // }

    // public void deleteEntity(String id, String collection, Handler<AsyncResult<DeleteResult>> handler) {
    //     mongodb.removeDocument(collection, new JsonObject().put("id", id), removeDocument -> {
    //         if (removeDocument.succeeded()) {
    //             handler.handle(Future.succeededFuture(removeDocument.result()));
    //         } else {
    //             handler.handle(Future.failedFuture(removeDocument.cause()));
    //         }
    //     });
    // }

    // public void getDiagramEntity(String id, Handler<AsyncResult<JsonObject>> handler) {
    //     getEntity(id, "diagram", handler);
    // }

    // public void saveDiagramEntity(JsonObject entity, Handler<AsyncResult<SaveResult>> handler) {
    //     saveEntity(entity, "diagram", handler);
    // }

    // public void deleteDiagramEntity(String id, Handler<AsyncResult<DeleteResult>> handler) {
    //     deleteEntity(id, "diagram", handler);
    // }

    // public void getModelEntity(String id, Handler<AsyncResult<JsonObject>> handler) {
    //     getEntity(id, "model", handler);
    // }

    // public void saveModelEntity(JsonObject entity, Handler<AsyncResult<SaveResult>> handler) {
    //     saveEntity(entity, "model", handler);
    // }

    // public void deleteModelEntity(String id, Handler<AsyncResult<DeleteResult>> handler) {
    //     deleteEntity(id, "model", handler);
    // }

    // public void getDiagramElements(String rootId, Handler<AsyncResult<List<JsonObject>>> handler) {
    //     JsonObject query = new JsonObject().put("$or",
    //             new JsonArray().add(new JsonObject().put("id", rootId)).add(new JsonObject().put("rootId", rootId)));
    //     mongodb.find("diagram", query, find -> {
    //         if (find.succeeded()) {
    //             handler.handle(Future.succeededFuture(find.result()));
    //         } else {
    //             handler.handle(Future.failedFuture(find.cause()));
    //         }
    //     });
    // }

    // public void getDiagramModels(String rootId, Handler<AsyncResult<JsonArray>> handler) {
    //     MongoDB.Command command = mongodb.command("getDiagramModels", new HashMap<String, String>() {
    //         {
    //             put("rootId", rootId);
    //         }
    //     });
    //     mongodb.runCommand("aggregate", command, runCommand -> {
    //         if (runCommand.succeeded()) {
    //             handler.handle(Future.succeededFuture(runCommand.result().getJsonArray("result")));
    //         } else {
    //             handler.handle(Future.failedFuture(runCommand.cause()));
    //         }
    //     });
    // }

}
