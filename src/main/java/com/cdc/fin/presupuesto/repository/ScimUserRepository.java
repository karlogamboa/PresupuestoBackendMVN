package com.cdc.fin.presupuesto.repository;

import com.cdc.fin.presupuesto.model.ScimListResponse;
import com.cdc.fin.presupuesto.model.ScimUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class ScimUserRepository {
    // Actualiza atributos SAML en el usuario SCIM en DynamoDB
    public void updateUserWithSamlAttributes(String userName, Map<String, Object> samlAttrs) {
        if (dynamoDbClient == null || usersTable == null || usersTable.isEmpty() || userName == null) {
            return;
        }
        // Buscar usuario por userName
        QueryRequest queryRequest = QueryRequest.builder()
            .tableName(usersTable)
            .indexName("UserNameIndex") // Debes tener este GSI en DynamoDB
            .keyConditionExpression("userName = :v_user")
            .expressionAttributeValues(Map.of(":v_user", AttributeValue.builder().s(userName).build()))
            .build();
        QueryResponse response = dynamoDbClient.query(queryRequest);
        if (response.hasItems() && !response.items().isEmpty()) {
            Map<String, AttributeValue> item = response.items().get(0);
            String userId = item.get("id").s();
            Map<String, AttributeValue> updateValues = new HashMap<>();
            // Puedes mapear los atributos SAML relevantes aquí
            for (Map.Entry<String, Object> entry : samlAttrs.entrySet()) {
                // Ejemplo: guardar como string JSON
                updateValues.put(entry.getKey(), AttributeValue.builder().s(entry.getValue().toString()).build());
            }
            // Actualiza el usuario con los nuevos atributos
            Map<String, AttributeValue> key = Map.of("id", AttributeValue.builder().s(userId).build());
            Map<String, AttributeValueUpdate> updates = new HashMap<>();
            for (Map.Entry<String, AttributeValue> entry : updateValues.entrySet()) {
                updates.put(entry.getKey(), AttributeValueUpdate.builder().value(entry.getValue()).action(AttributeAction.PUT).build());
            }
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                .tableName(usersTable)
                .key(key)
                .attributeUpdates(updates)
                .build());
        }
    }

    @Autowired(required = false)
    private DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.table.scim-users:}")
    private String usersTable;

    // Crear usuario (POST /Users)
    public ScimUser createUser(ScimUser user) throws JsonProcessingException {
        String id = java.util.UUID.randomUUID().toString();
        user.setId(id);
        ensureScimCompliance(user);

        if (dynamoDbClient != null && usersTable != null && !usersTable.isEmpty()) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(id).build());
            item.put("userName", AttributeValue.builder().s(user.getUserName()).build());
            item.put("active", AttributeValue.builder().bool(user.getActive() != null ? user.getActive() : true).build());
            if (user.getName() != null)
                item.put("name", AttributeValue.builder().s(new ObjectMapper().writeValueAsString(user.getName())).build());
            // ...otros campos relevantes...

            dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(usersTable)
                .item(item)
                .build());
        }
        return user;
    }

    // Leer usuario por ID (GET /Users/{id})
    public ScimUser getUser(String id) throws JsonProcessingException {
        if (dynamoDbClient == null || usersTable == null || usersTable.isEmpty()) {
            return null;
        }
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());
        var result = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(usersTable)
                .key(key)
                .build());

        if (result.hasItem()) {
            Map<String, AttributeValue> item = result.item();
            ScimUser user = new ScimUser();
            user.setId(item.get("id").s());
            user.setUserName(item.get("userName").s());
            user.setActive(item.containsKey("active") ? item.get("active").bool() : true);
            if (item.containsKey("name"))
                user.setName(new ObjectMapper().readValue(item.get("name").s(), ScimUser.Name.class));
            ensureScimCompliance(user);
            return user;
        }
        return null;
    }

    // Actualizar usuario (PUT /Users/{id})
    public ScimUser replaceUser(String id, ScimUser user) throws JsonProcessingException {
        user.setId(id);
        ensureScimCompliance(user);

        if (dynamoDbClient != null && usersTable != null && !usersTable.isEmpty()) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(id).build());
            item.put("userName", AttributeValue.builder().s(user.getUserName()).build());
            item.put("active", AttributeValue.builder().bool(user.getActive() != null ? user.getActive() : true).build());
            if (user.getName() != null)
                item.put("name", AttributeValue.builder().s(new ObjectMapper().writeValueAsString(user.getName())).build());
            // ...otros campos relevantes...

            dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(usersTable)
                .item(item)
                .build());
        }
        return user;
    }

    // Desactivar/reactivar usuario (PATCH /Users/{id})
    public ScimUser patchUser(String id, ScimUser patch) throws JsonProcessingException {
        ScimUser user = getUser(id);
        if (user != null) {
            if (patch.getActive() != null) user.setActive(patch.getActive());
            if (patch.getUserName() != null) user.setUserName(patch.getUserName());
            if (patch.getName() != null) user.setName(patch.getName());
            if (patch.getEmails() != null) user.setEmails(patch.getEmails());
            if (patch.getRoles() != null) user.setRoles(patch.getRoles());
            ensureScimCompliance(user);
            return replaceUser(id, user);
        }
        return null;
    }

    // Buscar usuarios por userName (GET /Users?filter=userName eq "...")
    public ScimListResponse<ScimUser> listUsers(String filter) throws JsonProcessingException {
        Pattern USER_NAME_FILTER_PATTERN = Pattern.compile("userName\\s+eq\\s+\"([^\"]+)\"");
        Matcher matcher = (filter != null) ? USER_NAME_FILTER_PATTERN.matcher(filter) : null;

        List<ScimUser> filteredUsers = new ArrayList<>();
        if (matcher != null && matcher.find()) {
            String userNameToFilter = matcher.group(1);
            QueryRequest queryRequest = QueryRequest.builder()
                .tableName(usersTable)
                .indexName("UserNameIndex") // Debes crear este GSI en DynamoDB
                .keyConditionExpression("userName = :v_user")
                .expressionAttributeValues(Map.of(":v_user", AttributeValue.builder().s(userNameToFilter).build()))
                .build();
            QueryResponse response = dynamoDbClient.query(queryRequest);
            ObjectMapper mapper = new ObjectMapper();
            for (Map<String, AttributeValue> item : response.items()) {
                ScimUser user = new ScimUser();
                AttributeValue idAttr = item.get("id");
                if (idAttr != null) user.setId(idAttr.s());
                AttributeValue userNameAttr = item.get("userName");
                if (userNameAttr != null) user.setUserName(userNameAttr.s());
                user.setActive(item.containsKey("active") ? item.get("active").bool() : true);
                AttributeValue nameAttr = item.get("name");
                if (nameAttr != null)
                    user.setName(mapper.readValue(nameAttr.s(), ScimUser.Name.class));
                ensureScimCompliance(user);
                filteredUsers.add(user);
            }
        } else {
            // Si no hay filtro, puedes hacer un scan y devolver todos los usuarios
            filteredUsers = listUsers().getResources();
        }

        ScimListResponse<ScimUser> response = new ScimListResponse<>();
        response.setTotalResults(filteredUsers.size());
        response.setItemsPerPage(filteredUsers.size());
        response.setStartIndex(1);
        response.setResources(filteredUsers);

        return response;
    }

    // Listar todos los usuarios (GET /Users)
    public ScimListResponse<ScimUser> listUsers() {
        List<ScimUser> userResources = new ArrayList<>();

        if (dynamoDbClient != null && usersTable != null && !usersTable.isEmpty()) {
            ScanRequest scanRequest = ScanRequest.builder().tableName(usersTable).build();
            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
            ObjectMapper mapper = new ObjectMapper();

            for (Map<String, AttributeValue> item : scanResponse.items()) {
                try {
                    ScimUser user = new ScimUser();
                    AttributeValue idAttr = item.get("id");
                    if (idAttr != null) user.setId(idAttr.s());
                    AttributeValue userNameAttr = item.get("userName");
                    if (userNameAttr != null) user.setUserName(userNameAttr.s());
                    user.setActive(item.containsKey("active") ? item.get("active").bool() : true);
                    AttributeValue nameAttr = item.get("name");
                    if (nameAttr != null)
                        user.setName(mapper.readValue(nameAttr.s(), ScimUser.Name.class));
                    ensureScimCompliance(user);
                    userResources.add(user);
                } catch (Exception e) {
                    System.err.println("Error procesando el registro de usuario: " + item);
                    e.printStackTrace();
                }
            }
        }
        ScimListResponse<ScimUser> response = new ScimListResponse<>();
        response.setTotalResults(userResources.size());
        response.setItemsPerPage(userResources.size());
        response.setStartIndex(1);
        response.setResources(userResources);
        return response;
    }

    // Eliminar usuario por ID (DELETE /Users/{id})
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

    private void ensureScimCompliance(ScimUser user) {
        if (user.getSchemas() == null || user.getSchemas().isEmpty()) {
            user.setSchemas(Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:User"));
        }
    }
}
