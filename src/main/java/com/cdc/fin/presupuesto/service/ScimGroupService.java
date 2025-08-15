package com.cdc.fin.presupuesto.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import com.cdc.fin.presupuesto.repository.ScimGroupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import com.cdc.fin.presupuesto.model.ScimGroup;
import com.cdc.fin.presupuesto.model.ScimListResponse;

@Service
public class ScimGroupService {

    @Autowired(required = false)
    private DynamoDbClient dynamoDbClient;

    @Autowired
    private ScimGroupRepository groupRepository;

    private String groupsTable;

    @Autowired
    public void setGroupsTable(@Value("${aws.dynamodb.table.prefix}") String tablePrefix) {
        this.groupsTable = tablePrefix + "scim-groups";
    }

    public ScimListResponse<ScimGroup> listGroups() {
        List<ScimGroup> groupResources = new ArrayList<>();

        if (dynamoDbClient != null && groupsTable != null && !groupsTable.isEmpty()) {
            ScanRequest scanRequest = ScanRequest.builder().tableName(groupsTable).build();
            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
            ObjectMapper mapper = new ObjectMapper();

            for (Map<String, AttributeValue> item : scanResponse.items()) {
                if (item.containsKey("groupJson") && item.containsKey("id")) {
                    String groupJson = item.get("groupJson").s();
                    String groupId = item.get("id").s();
                    try {
                        ScimGroup group = mapper.readValue(groupJson, ScimGroup.class);
                        group.setId(groupId);
                        // Asegura que el campo "schemas" esté presente
                        if (group.getSchemas() == null || group.getSchemas().isEmpty()) {
                            group.setSchemas(Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:Group"));
                        }
                        groupResources.add(group);
                    } catch (Exception e) {
                        System.err.println("Error procesando el registro de grupo: " + groupJson);
                        e.printStackTrace();
                    }
                }
            }
        }

        ScimListResponse<ScimGroup> response = new ScimListResponse<>();
        response.setSchemas(Collections.singletonList("urn:ietf:params:scim:api:messages:2.0:ListResponse"));
        response.setTotalResults(groupResources.size());
        response.setItemsPerPage(groupResources.size());
        response.setStartIndex(1);
        response.setResources(groupResources);

        return response;
    }

    public String createGroup(String groupJson) {
        return groupRepository.createGroup(groupJson);
    }

    public String getGroup(String id) {
        return groupRepository.getGroup(id);
    }

    public String replaceGroup(String id, String groupJson) {
        return groupRepository.replaceGroup(id, groupJson);
    }

    public String patchGroup(String id, String patchJson) {
        return groupRepository.patchGroup(id, patchJson);
    }

    public void deleteGroup(String id) {
        groupRepository.deleteGroup(id);
    }

    // Todos los métodos SCIM para grupos están implementados.
}
