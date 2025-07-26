package com.cdc.fin.presupuesto.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ScimGroup {
    @JsonProperty("id")
    private String id;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("members")
    private List<Member> members;

    @JsonProperty("schemas")
    private List<String> schemas = java.util.Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:Group");

    public static class Member {
        @JsonProperty("value")
        private String value;
        @JsonProperty("display")
        private String display;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getDisplay() { return display; }
        public void setDisplay(String display) { this.display = display; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public List<Member> getMembers() { return members; }
    public void setMembers(List<Member> members) { this.members = members; }
    public List<String> getSchemas() { return schemas; }
    public void setSchemas(List<String> schemas) { this.schemas = schemas; }
}
