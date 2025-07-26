package com.cdc.fin.presupuesto.repository;

import com.cdc.fin.presupuesto.model.ScimListResponse;
import com.cdc.fin.presupuesto.model.ScimUser;
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
import java.util.stream.Collectors;

@Repository
public class ScimUserRepository {

    @Autowired(required = false)
    private DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.table.scim-users:}")
    private String usersTable;

    public ScimUser createUser(ScimUser user) {
        String id = java.util.UUID.randomUUID().toString();
        user.setId(id);
        if (user.getSchemas() == null || user.getSchemas().isEmpty()) {
            user.setSchemas(Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:User"));
        }
        String userJson;
        try {
            ObjectMapper mapper = new ObjectMapper();
            userJson = mapper.writeValueAsString(user);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando usuario", e);
        }
        if (dynamoDbClient != null && usersTable != null && !usersTable.isEmpty()) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(id).build());
            item.put("userJson", AttributeValue.builder().s(userJson).build());
            dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(usersTable)
                .item(item)
                .build());
        }
        return user;
    }

    /**
     * Obtiene un solo usuario y lo devuelve como un objeto ScimUser.
     * La serialización a JSON la hará el controlador.
     */
    public ScimUser getUser(String id) {
        if (dynamoDbClient == null || usersTable == null || usersTable.isEmpty()) {
            return null;
        }

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());
        var result = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(usersTable)
                .key(key)
                .build());

        if (result.hasItem() && result.item().containsKey("userJson")) {
            String userJson = result.item().get("userJson").s();
            try {
                ObjectMapper mapper = new ObjectMapper();
                ScimUser user = mapper.readValue(userJson, ScimUser.class);

                // La fuente de verdad para el ID es la clave primaria de DynamoDB.
                user.setId(id);

                // Asegura que el campo "schemas" esté presente.
                if (user.getSchemas() == null || user.getSchemas().isEmpty()) {
                    user.setSchemas(Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:User"));
                }
                return user;
            } catch (Exception e) {
                // ¡NUNCA ignores los errores! Esto te mostrará qué está fallando.
                System.err.println("Error al deserializar el usuario con ID: " + id);
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public ScimUser replaceUser(String id, ScimUser user) {
        user.setId(id);
        if (user.getSchemas() == null || user.getSchemas().isEmpty()) {
            user.setSchemas(Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:User"));
        }
        String userJson;
        try {
            ObjectMapper mapper = new ObjectMapper();
            userJson = mapper.writeValueAsString(user);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando usuario", e);
        }
        if (dynamoDbClient != null && usersTable != null && !usersTable.isEmpty()) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(id).build());
            item.put("userJson", AttributeValue.builder().s(userJson).build());
            dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(usersTable)
                .item(item)
                .build());
        }
        return user;
    }

    public ScimUser patchUser(String id, ScimUser patch) {
        ScimUser user = getUser(id);
        if (user != null) {
            // Aplica solo los cambios del patch (ejemplo: active, userName, emails, roles)
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

    /**
     * Devuelve un objeto ScimListResponse que contiene la lista de usuarios.
     * El framework se encargará de convertir esto a JSON.
     */
    public ScimListResponse<ScimUser> listUsers() {
        List<ScimUser> userResources = new ArrayList<>();

        if (dynamoDbClient != null && usersTable != null && !usersTable.isEmpty()) {
            ScanRequest scanRequest = ScanRequest.builder().tableName(usersTable).build();
            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
            ObjectMapper mapper = new ObjectMapper();

            for (Map<String, AttributeValue> item : scanResponse.items()) {
                if (item.containsKey("userJson") && item.containsKey("id")) {
                    String userJson = item.get("userJson").s();
                    String userId = item.get("id").s();
                    try {
                        ScimUser user = mapper.readValue(userJson, ScimUser.class);

                        // Asigna explícitamente el ID desde la clave de DynamoDB
                        user.setId(userId);

                        // Asegura que el campo "schemas" esté presente
                        if (user.getSchemas() == null || user.getSchemas().isEmpty()) {
                           user.setSchemas(Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:User"));
                        }
                        userResources.add(user);

                    } catch (Exception e) {
                        // Loguea el error para poder depurarlo
                        System.err.println("Error procesando el registro de usuario: " + userJson);
                        e.printStackTrace();
                    }
                }
            }
        }

        // Construye el objeto de respuesta final
        ScimListResponse<ScimUser> response = new ScimListResponse<ScimUser>();
        response.setTotalResults(userResources.size());
        response.setItemsPerPage(userResources.size());
        response.setStartIndex(1);
        response.setResources(userResources); // Asumiendo que ScimListResponse tiene un campo List<ScimUser> Resources

        return response;
    }

    /**
     * Lista usuarios. Si se provee un filtro por 'userName', busca un usuario específico
     * realizando el filtrado en la aplicación después de leer de la BD.
     */
    public ScimListResponse<ScimUser> listUsers(String filter) {
        System.out.println("SCIM listUsers - Filtro recibido: " + filter);

        List<ScimUser> allUsers = new ArrayList<>();

        if (dynamoDbClient != null && usersTable != null && !usersTable.isEmpty()) {
            ScanRequest scanRequest = ScanRequest.builder().tableName(usersTable).build();
            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
            ObjectMapper mapper = new ObjectMapper();

            for (Map<String, AttributeValue> item : scanResponse.items()) {
                if (item.containsKey("userJson") && item.containsKey("id")) {
                    String userJson = item.get("userJson").s();
                    String userId = item.get("id").s();
                    try {
                        ScimUser user = mapper.readValue(userJson, ScimUser.class);
                        user.setId(userId);
                        ensureScimCompliance(user);
                        allUsers.add(user);
                    } catch (Exception e) {
                        System.err.println("Error procesando el registro de usuario: " + userJson);
                        e.printStackTrace();
                    }
                }
            }
        }

        List<ScimUser> filteredUsers;
        Pattern USER_NAME_FILTER_PATTERN = Pattern.compile("userName\\s+eq\\s+\"([^\"]+)\"");
        Matcher matcher = (filter != null) ? USER_NAME_FILTER_PATTERN.matcher(filter) : null;

        if (matcher != null && matcher.find()) {
            String userNameToFilter = matcher.group(1);
            System.out.println("SCIM listUsers - Buscando userName: '" + userNameToFilter + "'");
            filteredUsers = allUsers.stream()
                    .filter(user -> user.getUserName() != null && user.getUserName().equals(userNameToFilter))
                    .collect(Collectors.toList());
            System.out.println("SCIM listUsers - Usuarios encontrados después del filtro: " + filteredUsers.size());
            for (ScimUser u : filteredUsers) {
                System.out.println("SCIM listUsers - Usuario encontrado: id=" + u.getId() + ", userName=" + u.getUserName());
            }
        } else {
            System.out.println("SCIM listUsers - No se encontró un filtro de userName válido, devolviendo todos los usuarios.");
            filteredUsers = allUsers;
        }

        ScimListResponse<ScimUser> response = new ScimListResponse<>();
        response.setTotalResults(filteredUsers.size());
        response.setItemsPerPage(filteredUsers.size());
        response.setStartIndex(1);
        response.setResources(filteredUsers);

        return response;
    }

    private void ensureScimCompliance(ScimUser user) {
        if (user.getSchemas() == null || user.getSchemas().isEmpty()) {
           user.setSchemas(Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:User"));
        }
    }
}
