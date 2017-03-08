package it.beng.modeler.model.semantic.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.beng.modeler.model.Entity;
import it.beng.modeler.model.semantic.SemanticElement;

import java.util.List;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class Person extends SemanticElement {

    public static List<Person> list() {
        return Entity.list(Person.class);
    }

    private String username;

    @JsonProperty(required = true)
    @JsonPropertyDescription("the first name of this Person")
    public String firstName;
    @JsonProperty(required = true)
    @JsonPropertyDescription("the last name of this Person")
    public String lastName;
    @JsonProperty(required = true)
    @JsonPropertyDescription("the e-mail of this Person")
    public String eMail;

    protected Person() {
    }

    public Person(String id, String ownerId, String name, String documentation, String firstName, String lastName, String eMail, String username) {
        super(id, ownerId, name, documentation);
        this.firstName = firstName;
        this.lastName = lastName;
        this.eMail = eMail;
        this.username = username;
    }

    @JsonProperty(defaultValue = "$ref: eMail")
    @JsonPropertyDescription("the username of this Person")
    public String getUsername() {
        return username != null ? username : eMail;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
