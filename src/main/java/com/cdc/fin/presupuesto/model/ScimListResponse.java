package com.cdc.fin.presupuesto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ScimListResponse<T> {
    @JsonProperty("schemas")
    private List<String> schemas = java.util.Collections.singletonList("urn:ietf:params:scim:api:messages:2.0:ListResponse");

    @JsonProperty("totalResults")
    private int totalResults;

    @JsonProperty("startIndex")
    private int startIndex;

    @JsonProperty("itemsPerPage")
    private int itemsPerPage;

    @JsonProperty("Resources")
    private List<T> resources;

    public List<String> getSchemas() { return schemas; }
    public void setSchemas(List<String> schemas) { this.schemas = schemas; }

    public int getTotalResults() { return totalResults; }
    public void setTotalResults(int totalResults) { this.totalResults = totalResults; }

    public int getStartIndex() { return startIndex; }
    public void setStartIndex(int startIndex) { this.startIndex = startIndex; }

    public int getItemsPerPage() { return itemsPerPage; }
    public void setItemsPerPage(int itemsPerPage) { this.itemsPerPage = itemsPerPage; }

    public List<T> getResources() { return resources; }
    public void setResources(List<T> resources) { this.resources = resources; }
}
