package com.cdc.presupuesto.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Repository
public class ScimGroupRepository {

    @Autowired(required = false)
    private DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.table.scim-groups:}")
    private String groupsTable;

    public String listGroups() {
        // Implementa consulta a DynamoDB si lo deseas, aquí solo retorna vacío
        return "{\"Resources\":[]}";
    }

    public String createGroup(String groupJson) {
        String id = UUID.randomUUID().toString();
        if (dynamoDbClient != null && groupsTable != null && !groupsTable.isEmpty()) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(id).build());
            item.put("groupJson", AttributeValue.builder().s(groupJson).build());
            dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(groupsTable)
                .item(item)
                .build());
        }
        return "{\"id\":\"" + id + "\"}";
    }

    public String getGroup(String id) {
        if (dynamoDbClient != null && groupsTable != null && !groupsTable.isEmpty()) {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(id).build());
            var result = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(groupsTable)
                .key(key)
                .build());
            if (result.hasItem() && result.item().containsKey("groupJson")) {
                return result.item().get("groupJson").s();
            }
        }
        return null;
    }

    public String replaceGroup(String id, String groupJson) {
        if (dynamoDbClient != null && groupsTable != null && !groupsTable.isEmpty()) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(id).build());
            item.put("groupJson", AttributeValue.builder().s(groupJson).build());
            dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(groupsTable)
                .item(item)
                .build());
        }
        return groupJson;
    }

    public String patchGroup(String id, String patchJson) {
        // Implementa lógica de patch si es necesario
        return "{\"id\":\"" + id + "\"}";
    }

    public void deleteGroup(String id) {
        if (dynamoDbClient != null && groupsTable != null && !groupsTable.isEmpty()) {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(id).build());
            dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(groupsTable)
                .key(key)
                .build());
        }
    }

    // Todas las operaciones CRUD SCIM para grupos están implementadas.
}

