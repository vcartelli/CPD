package it.beng.modeler.model.semantic.organization;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hazelcast.util.MD5Util;
import io.vertx.core.json.Json;
import it.beng.modeler.model.basic.Typed;
import it.beng.modeler.model.semantic.organization.roles.AuthenticationRole;
import it.beng.modeler.model.semantic.organization.roles.AuthorizationRole;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>This class is a member of <strong>modeler-microservice</strong> project.</p>
 *
 * @author vince
 */
public class UserProfile implements Typed {

    private static Map<String, UserProfile> CACHE = new LinkedHashMap<>();

    public static UserProfile get(String username) {
        return CACHE.get(username);
    }

    public static List<UserProfile> list() {
        return CACHE.values().stream().collect(Collectors.toList());
    }

    public static class Name {
        public String familyName;
        public String givenName;

        public Name() {
        }

        public Name(String familyName, String givenName) {
            this.familyName = familyName;
            this.givenName = givenName;
        }
    }

    public static class Image {
        public String url;
        public boolean isDefault;

        public Image() {
        }

        public Image(String url, boolean isDefault) {
            this.url = url;
            this.isDefault = isDefault;
        }
    }

/*
    gender: string;
    name: {
      familyName: string;
      givenName: string;
    };
    displayName: string;
    language: string;
    image: {
        url: string;
        isDefault: boolean;
    };
    authorizationRoles: Role[];
*/

    @JsonProperty(required = true)
    @JsonPropertyDescription("provider of this UserProfile")
    public final String provider = "local";
    @JsonProperty(required = true)
    @JsonPropertyDescription("username of this UserProfile")
    public String username;
    @JsonProperty(required = true)
    @JsonPropertyDescription("the id of Person owner of this UserProfile")
    public String personId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyDescription("the md5 encrypted password of this UserProfile")
    public String password;
    @JsonPropertyDescription("the gender of this UserProfile")
    public String gender;
    @JsonProperty(required = true)
    @JsonPropertyDescription("the name of this UserProfile")
    public Name name;
    @JsonProperty(required = true)
    @JsonPropertyDescription("the display name of this UserProfile")
    public String displayName;
    @JsonProperty(required = true, defaultValue = "en")
    @JsonPropertyDescription("the display name of this UserProfile")
    public String language = "en";
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyDescription("the display name of this UserProfile")
    public Image image;
    @JsonProperty(required = true)
    @JsonPropertyDescription("the authentication role associated to this UserProfile")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public AuthenticationRole authenticationRole;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyDescription("the authorization roles (key: diagramId) associated to this UserProfile")
    public Map<String, Set<AuthorizationRole>> authorizationRoles;
//    // TODO: load the email from google and append it to the loggedInUser object
//    // NOTE: the email could be retrieved from Person...
//    @JsonProperty(required = true)
//    @JsonPropertyDescription("the e-mail of this UserProfile")
//    public String eMail;

    protected UserProfile() {
    }

    public UserProfile(String username, String personId, String password, String gender, Name name, String displayName, String language, Image image, AuthenticationRole authenticationRole, Map<String, Set<AuthorizationRole>> authorizationRoles) {
        this.username = username;
        this.personId = personId;
        this.password = password;
        this.gender = gender;
        this.name = name;
        this.displayName = displayName;
        this.language = language;
        this.image = image;
        this.authenticationRole = authenticationRole;
        this.authorizationRoles = authorizationRoles;
        CACHE.put(username, this);
    }

    static {
        new UserProfile(
            "administrator",
            "3e3e2dbc-10e4-4953-a852-8f90920760b7",
            MD5Util.toMD5String("simpatico"),
            "male",
            new Name("Simpatico", "Administrator"),
            "Simpatico Administrator",
            "en",
            null,
            AuthenticationRole.ADMINISTRATOR,
            new LinkedHashMap<String, Set<AuthorizationRole>>() {
                {
                    put("*", new LinkedHashSet<>(Arrays.asList(AuthorizationRole.COLLABORATOR)));
                    put("*", new LinkedHashSet<>(Arrays.asList(AuthorizationRole.EDITOR)));
                    put("*", new LinkedHashSet<>(Arrays.asList(AuthorizationRole.REVIEWER)));
                    put("*", new LinkedHashSet<>(Arrays.asList(AuthorizationRole.OWNER)));
                }
            }
        );
        new UserProfile(
            "citizen",
            "3e3e2dbc-10e4-4953-a852-8f90920760b7",
            MD5Util.toMD5String("simpatico"),
            "male",
            new Name("Simpatico", "Citizen"),
            "Simpatico Citizen",
            "en",
            null,
            AuthenticationRole.CITIZEN,
            new LinkedHashMap<String, Set<AuthorizationRole>>() {
                {
                    put("*", new LinkedHashSet<>(Arrays.asList(AuthorizationRole.OBSERVER)));
                }
            }
        );
        new UserProfile(
            "civil servant 1",
            "b4b30fab-7cca-4ee4-a0a1-07550bec363f",
            MD5Util.toMD5String("simpatico"),
            "male",
            new Name("Simpatico Civil", "Servant #1"),
            "Simpatico Civil Servant #1",
            "en",
            null,
            AuthenticationRole.CIVIL_SERVANT,
            new LinkedHashMap<String, Set<AuthorizationRole>>() {
                {
                    put("43467de2-9f42-477f-9f00-13b70f53ce24", new LinkedHashSet<>(Arrays.asList(AuthorizationRole.EDITOR, AuthorizationRole.REVIEWER)));
                    put("62c02032-0b13-436c-9a96-00a7e479801b", new LinkedHashSet<>(Arrays.asList(AuthorizationRole.COLLABORATOR)));
                }
            }
        );
        new UserProfile(
            "civil servant 2",
            "317f15b6-9a69-4c0f-ad97-b226e5f6029e",
            MD5Util.toMD5String("simpatico"),
            "male",
            new Name("Civil", "Servant #2"),
            "Civil Servant #2",
            "en",
            null,
            AuthenticationRole.CIVIL_SERVANT,
            new LinkedHashMap<String, Set<AuthorizationRole>>() {
                {
                    put("43467de2-9f42-477f-9f00-13b70f53ce24", new LinkedHashSet<>(Arrays.asList(AuthorizationRole.OWNER)));
                    put("62c02032-0b13-436c-9a96-00a7e479801b", new LinkedHashSet<>(Arrays.asList(AuthorizationRole.EDITOR, AuthorizationRole.REVIEWER)));
                }
            }
        );
    }

}
