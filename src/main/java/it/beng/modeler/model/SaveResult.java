package it.beng.modeler.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.schema.ValidationResult;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class SaveResult<T> {

    public final ValidationResult validation;
    public final T data;

    public SaveResult(ValidationResult validation, T data) {
        this.validation = validation;
        this.data = data;
    }

    public JsonObject toJson() {
        Object data;
        if ((this.data instanceof JsonObject) || (this.data instanceof JsonArray))
            data = this.data;
        else if (this.data instanceof Collection)
            data = new JsonArray(new ArrayList<Object>((Collection<?>) this.data));
        else
            data = JsonObject.mapFrom(this.data);
        return new JsonObject()
                               .put("validation", validation.toJson())
                               .put("data", data);
    }

}
