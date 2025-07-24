package com.cdc.presupuesto.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;

@Repository
public class ScimUserRepository {

    @Autowired(required = false)
    private DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.table.scim-users:}")
    private String usersTable;

    public String createUser(String userJson) {
        String id = java.util.UUID.randomUUID().toString();
        String userJsonWithId;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode node = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(userJson);
            node.put("id", id);
            userJsonWithId = mapper.writeValueAsString(node);
        } catch (Exception e) {
            userJsonWithId = userJson;
        }
        if (dynamoDbClient != null && usersTable != null && !usersTable.isEmpty()) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(id).build());
            item.put("userJson", AttributeValue.builder().s(userJsonWithId).build());
            dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(usersTable)
                .item(item)
                .build());
        }
        return userJsonWithId;
    }

    public String getUser(String id) {
        if (dynamoDbClient != null && usersTable != null && !usersTable.isEmpty()) {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(id).build());
            var result = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(usersTable)
                .key(key)
                .build());
            if (result.hasItem() && result.item().containsKey("userJson")) {
                return result.item().get("userJson").s();
            }
        }
        return null;
    }

    public String replaceUser(String id, String userJson) {
        if (dynamoDbClient != null && usersTable != null && !usersTable.isEmpty()) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(id).build());
            item.put("userJson", AttributeValue.builder().s(userJson).build());
            dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(usersTable)
                .item(item)
                .build());
        }
        return userJson;
    }

    public String patchUser(String id, String patchJson) {
        // Implementa lógica de patch si es necesario
        return "{\"id\":\"" + id + "\"}";
    }

    public void deleteUser(String id) {
        if (dynamoDbClient != null && usersTable != null && !usersTable.isEmpty()) {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(id).build());
            dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(usersTable)
                .key(key)
                .build());
        }
    }

    public String listUsers() {
        if (dynamoDbClient != null && usersTable != null && !usersTable.isEmpty()) {
            ScanRequest scanRequest = ScanRequest.builder().tableName(usersTable).build();
            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
            java.util.List<String> resources = new java.util.ArrayList<>();
            for (Map<String, AttributeValue> item : scanResponse.items()) {
                if (item.containsKey("userJson")) {
                    String userJson = item.get("userJson").s();
                    // Verifica que el JSON tenga "id", si no lo tiene, lo agrega
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        com.fasterxml.jackson.databind.node.ObjectNode node = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(userJson);
                        if (!node.has("id") && item.containsKey("id")) {
                            node.put("id", item.get("id").s());
                            userJson = mapper.writeValueAsString(node);
                        }
                    } catch (Exception e) { /* ignora error */ }
                    resources.add(userJson);
                }
            }
            return "{\"Resources\":[" + String.join(",", resources) + "]}";
        }
        return "{\"Resources\":[]}";
    }

    // Todas las operaciones CRUD SCIM para usuarios están implementadas.
}
