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
        @JsonProperty("type")
        private String type;

        public Email() {}

        public Email(String value, Boolean primary, String type) {
            this.value = value;
            this.primary = primary;
            this.type = type;
        }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public Boolean getPrimary() { return primary; }
        public void setPrimary(Boolean primary) { this.primary = primary; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
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

    // SAML attributes
    @JsonProperty("email")
    private String email;
    @JsonProperty("given_name")
    private String given_name;
    @JsonProperty("family_name")
    private String family_name;
    @JsonProperty("employee_number")
    private String employeeNumber;
    @JsonProperty("user_type")
    private String userType;
    @JsonProperty("department")
    private String department;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGiven_name() { return given_name; }
    public void setGiven_name(String given_name) { this.given_name = given_name; }
    public String getFamily_name() { return family_name; }
    public void setFamily_name(String family_name) { this.family_name = family_name; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
