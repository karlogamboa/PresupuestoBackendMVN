package com.cdc.fin.presupuesto.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ScimUser {
    // No debe tener @JsonIgnore ni @JsonProperty("otroNombre") en el campo id
    @JsonProperty("id")
    private String id;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("name")
    private Name name;

    @JsonProperty("emails")
    private List<Email> emails;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("roles")
    private List<String> roles;

    // SCIM requiere el campo "schemas" en el JSON
    @JsonProperty("schemas")
    private List<String> schemas = java.util.Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:User");

    public static class Name {
        @JsonProperty("givenName")
        private String givenName;
        @JsonProperty("familyName")
        private String familyName;

        public String getGivenName() { return givenName; }
        public void setGivenName(String givenName) { this.givenName = givenName; }
        public String getFamilyName() { return familyName; }
        public void setFamilyName(String familyName) { this.familyName = familyName; }
    }

    public static class Email {
        @JsonProperty("value")
        private String value;
        @JsonProperty("primary")
        private Boolean primary;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public Boolean getPrimary() { return primary; }
        public void setPrimary(Boolean primary) { this.primary = primary; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Name getName() { return name; }
    public void setName(Name name) { this.name = name; }
    public List<Email> getEmails() { return emails; }
    public void setEmails(List<Email> emails) { this.emails = emails; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public List<String> getSchemas() { return schemas; }
    public void setSchemas(List<String> schemas) { this.schemas = schemas; }
}
