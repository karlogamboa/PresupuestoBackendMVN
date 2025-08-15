package com.cdc.fin.presupuesto.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonProperty("group")
    private List<String> group;

    // SCIM requiere el campo "schemas" en el JSON
    @JsonProperty("schemas")
    private List<String> schemas = java.util.Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:User");

 

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Name {
        @JsonProperty("givenName")
        private String givenName;
        @JsonProperty("familyName")
        private String familyName;
        @JsonProperty("middleName")
        private String middleName; // <-- Add this property
        @JsonProperty("honorificPrefix")
        private String honorificPrefix;
        @JsonProperty("honorificSuffix")
        private String honorificSuffix;

        public String getGivenName() { return givenName; }
        public void setGivenName(String givenName) { this.givenName = givenName; }

        public String getFamilyName() { return familyName; }
        public void setFamilyName(String familyName) { this.familyName = familyName; }

        public String getMiddleName() { return middleName; }
        public void setMiddleName(String middleName) { this.middleName = middleName; }

        public String getHonorificPrefix() { return honorificPrefix; }
        public void setHonorificPrefix(String honorificPrefix) { this.honorificPrefix = honorificPrefix; }

        public String getHonorificSuffix() { return honorificSuffix; }
        public void setHonorificSuffix(String honorificSuffix) { this.honorificSuffix = honorificSuffix; }
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
    public List<String> getGroup() { return group; }
    public void setGroup(List<String> group) { this.group = group; }
    public List<String> getSchemas() { return schemas; }
    public void setSchemas(List<String> schemas) { this.schemas = schemas; }

    // SAML attributes
    @JsonProperty("email")
    private String email;
    @JsonProperty("firstName")
    private String firstName;
    @JsonProperty("lastName")
    private String lastName;
    @JsonProperty("employeeNumber")
    private String employeeNumber;
    @JsonProperty("userType")
    private String userType;
    @JsonProperty("department")
    private String department;
    @JsonProperty("displayName")
    private String displayName;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
 
    public void syncNameFromFirstLast() {
        if (this.name == null) this.name = new Name();
        if (this.firstName != null) this.name.setGivenName(this.firstName);
        if (this.lastName != null) this.name.setFamilyName(this.lastName);
    }

    public void syncFirstLastFromName() {
        if (this.name != null) {
            if (this.name.getGivenName() != null) this.firstName = this.name.getGivenName();
            if (this.name.getFamilyName() != null) this.lastName = this.name.getFamilyName();
        }
    }
}