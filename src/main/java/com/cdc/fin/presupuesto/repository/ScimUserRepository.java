package com.cdc.fin.presupuesto.repository;

import com.cdc.fin.presupuesto.model.ScimListResponse;
import com.cdc.fin.presupuesto.model.ScimUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class ScimUserRepository {
    private static final Logger logger = LoggerFactory.getLogger(ScimUserRepository.class);
    // Actualiza atributos SAML en el usuario SCIM en DynamoDB
    public void updateUserWithSamlAttributes(String userName, Map<String, Object> samlAttrs) {
        logger.info("updateUserWithSamlAttributes called for userName: {}, samlAttrs: {}", userName, samlAttrs);
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
                if ("employeeNumber".equals(entry.getKey()) || "department".equals(entry.getKey())) {
                    updateValues.put(entry.getKey(), AttributeValue.builder().s(entry.getValue().toString()).build());
                }
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
        logger.info("updateUserWithSamlAttributes finished for userId: {}", response.hasItems() && !response.items().isEmpty() ? response.items().get(0).get("id").s() : null);
    }

    @Autowired(required = false)
    private DynamoDbClient dynamoDbClient;

    private String usersTable;

    @Autowired
    public ScimUserRepository(@Value("${aws.dynamodb.table.prefix}") String tablePrefix) {
        this.usersTable = tablePrefix + "scim-users";
    }

    // Crear usuario (POST /Users)
    public ScimUser createUser(ScimUser user) throws JsonProcessingException {
        logger.info("createUser called for user: {}", user);
        user.setUserType(getUserRoleType(user)); // <-- Guarda el tipo de usuario
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
            if (user.getEmails() != null && !user.getEmails().isEmpty())
                item.put("email", AttributeValue.builder().s(user.getEmails().get(0).getValue()).build());
            if (user.getFirstName() != null)
                item.put("firstName", AttributeValue.builder().s(user.getFirstName()).build());
            if (user.getLastName() != null)
                item.put("lastName", AttributeValue.builder().s(user.getLastName()).build());
            // Use snake_case for DynamoDB attributes
            if (user.getEmployeeNumber() != null)
                item.put("employeeNumber", AttributeValue.builder().s(user.getEmployeeNumber()).build());
            if (user.getDepartment() != null)
                item.put("department", AttributeValue.builder().s(user.getDepartment()).build());
            if (user.getUserType() != null)
                item.put("userType", AttributeValue.builder().s(user.getUserType()).build());
            if (user.getDisplayName() != null)
                item.put("displayName", AttributeValue.builder().s(user.getDisplayName()).build());
            logger.info("createUser DynamoDB item: {}", item);
            dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(usersTable)
                .item(item)
                .build());
        }
        logger.info("createUser finished for userId: {}", user.getId());
        return user;
    }

    // Leer usuario por ID (GET /Users/{id})
    public ScimUser getUser(String id) throws JsonProcessingException {
        logger.info("getUser called for id: {}", id);
        if (dynamoDbClient == null || usersTable == null || usersTable.isEmpty()) {
            return null;
        }
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());
        var result = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(usersTable)
                .key(key)
                .build());

        ScimUser user = null;
        if (result.hasItem()) {
            Map<String, AttributeValue> item = result.item();
            user = new ScimUser();
            user.setId(item.get("id").s());
            user.setUserName(item.get("userName").s());
            user.setActive(item.containsKey("active") ? item.get("active").bool() : true);
            if (item.containsKey("name"))
                user.setName(new ObjectMapper().readValue(item.get("name").s(), ScimUser.Name.class));
            // Leer atributos SAML/enterprise
            if (item.containsKey("email"))
                user.setEmails(List.of(new ScimUser.Email(item.get("email").s(), true, "work")));
            if (item.containsKey("firstName"))
                user.setFirstName(item.get("firstName").s());
            if (item.containsKey("lastName"))
                user.setLastName(item.get("lastName").s());
            if (item.containsKey("employeeNumber"))
                user.setEmployeeNumber(item.get("employeeNumber").s());
            if (item.containsKey("department"))
                user.setDepartment(item.get("department").s());
            if (item.containsKey("userType"))
                user.setUserType(item.get("userType").s());
            if (item.containsKey("displayName"))
                user.setDisplayName(item.get("displayName").s());
            if (item.containsKey("group"))
                user.setGroup(List.of(item.get("group").s()));
            ensureScimCompliance(user);
            user.setUserType(getUserRoleType(user));
        }
        logger.info("getUser finished for id: {}, found: {}", id, result.hasItem());
        return result.hasItem() ? user : null;
    }

    // Actualizar usuario (PUT /Users/{id})
    public ScimUser replaceUser(String id, ScimUser user) throws JsonProcessingException {
        logger.info("replaceUser called for id: {}, user: {}", id, user);
        logger.debug("[SCIM][replaceUser] Entrando a replaceUser con id={}, user={}", id, user);
        user.setUserType(getUserRoleType(user));
        user.setId(id);
        ensureScimCompliance(user);

        if (dynamoDbClient != null && usersTable != null && !usersTable.isEmpty()) {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(id).build());
            item.put("userName", AttributeValue.builder().s(user.getUserName()).build());
            item.put("active", AttributeValue.builder().bool(user.getActive() != null ? user.getActive() : true).build());
            if (user.getName() != null)
                item.put("name", AttributeValue.builder().s(new ObjectMapper().writeValueAsString(user.getName())).build());
            if (user.getEmails() != null && !user.getEmails().isEmpty())
                item.put("email", AttributeValue.builder().s(user.getEmails().get(0).getValue()).build());
            if (user.getFirstName() != null)
                item.put("firstName", AttributeValue.builder().s(user.getFirstName()).build());
            if (user.getLastName() != null)
                item.put("lastName", AttributeValue.builder().s(user.getLastName()).build());
            // Use snake_case for DynamoDB attributes
            if (user.getEmployeeNumber() != null)
                item.put("employeeNumber", AttributeValue.builder().s(user.getEmployeeNumber()).build());
            if (user.getDepartment() != null)
                item.put("department", AttributeValue.builder().s(user.getDepartment()).build());
            if (user.getUserType() != null)
                item.put("userType", AttributeValue.builder().s(user.getUserType()).build());
            if (user.getDisplayName() != null)
                item.put("displayName", AttributeValue.builder().s(user.getDisplayName()).build());
            logger.info("replaceUser DynamoDB item: {}", item);
            logger.debug("[SCIM][replaceUser] DynamoDB putItem: {}", item);
            dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(usersTable)
                .item(item)
                .build());
        } else {
            logger.debug("[SCIM][replaceUser] DynamoDB client o tabla no configurados: dynamoDbClient={}, usersTable={}", dynamoDbClient, usersTable);
        }
        logger.info("replaceUser finished for id: {}", id);
        logger.debug("[SCIM][replaceUser] Finalizando replaceUser para id={}", id);
        return user;
    }

    // Desactivar/reactivar usuario (PATCH /Users/{id})
    public ScimUser patchUser(String id, ScimUser patch) throws JsonProcessingException {
        logger.info("patchUser called for id: {}, patch: {}", id, patch);
        logger.debug("[SCIM][patchUser] Entrando a patchUser con id={}, patch={}", id, patch);
        ScimUser user = getUser(id);
        logger.debug("[SCIM][patchUser] Usuario obtenido de getUser: {}", user);
        if (user != null) {
            if (patch.getActive() != null) user.setActive(patch.getActive());
            if (patch.getUserName() != null) user.setUserName(patch.getUserName());
            if (patch.getName() != null) user.setName(patch.getName());
            if (patch.getEmails() != null) user.setEmails(patch.getEmails());
            if (patch.getGroup() != null) user.setGroup(patch.getGroup());
            // Campos faltantes SAML/enterprise
            if (patch.getEmployeeNumber() != null) user.setEmployeeNumber(patch.getEmployeeNumber());
            if (patch.getUserType() != null) user.setUserType(patch.getUserType());
            if (patch.getDepartment() != null) user.setDepartment(patch.getDepartment());
            if (patch.getDisplayName() != null) user.setDisplayName(patch.getDisplayName());
            ensureScimCompliance(user);
            logger.info("patchUser finished for id: {}", id);
            logger.debug("[SCIM][patchUser] Usuario después de aplicar patch: {}", user);
            return replaceUser(id, user);
        }
        logger.debug("[SCIM][patchUser] Usuario no encontrado para id={}", id);
        return null;
    }

    // Buscar usuarios por userName (GET /Users?filter=userName eq "...")
    public ScimListResponse<ScimUser> listUsers(String filter) throws JsonProcessingException {
        logger.info("listUsers called with filter: {}", filter);
        Pattern USER_NAME_FILTER_PATTERN = Pattern.compile("userName\\s+eq\\s+\"([^\"]+)\"");
        Matcher matcher = (filter != null) ? USER_NAME_FILTER_PATTERN.matcher(filter) : null;

        List<ScimUser> filteredUsers = new ArrayList<>();
        if (matcher != null && matcher.find()) {
            String userNameToFilter = matcher.group(1);
            QueryRequest queryRequest = QueryRequest.builder()
                .tableName(usersTable)
                .indexName("UserNameIndex") // GSI must have userName as partition key
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
                // Leer atributos SAML/enterprise
                if (item.containsKey("email"))
                    user.setEmails(List.of(new ScimUser.Email(item.get("email").s(), true, "work")));
                if (item.containsKey("firstName"))
                    user.setFirstName(item.get("firstName").s());
                if (item.containsKey("lastName"))
                    user.setLastName(item.get("lastName").s());
                if (item.containsKey("employeeNumber"))
                    user.setEmployeeNumber(item.get("employeeNumber").s());
                if (item.containsKey("userType"))
                    user.setUserType(item.get("userType").s());
                if (item.containsKey("department"))
                    user.setDepartment(item.get("department").s());
                if (item.containsKey("displayName"))
                    user.setDisplayName(item.get("displayName").s());
                if (item.containsKey("group"))
                    user.setGroup(List.of(item.get("group").s()));
                ensureScimCompliance(user);
                // Establece el tipo de usuario según roles
                user.setUserType(getUserRoleType(user));
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

        logger.info("listUsers finished, totalResults: {}", response.getTotalResults());
        return response;
    }

    // Listar todos los usuarios (GET /Users)
    public ScimListResponse<ScimUser> listUsers() {
        logger.info("listUsers (all) called");
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
                    // Leer atributos SAML/enterprise
                    if (item.containsKey("email"))
                        user.setEmails(List.of(new ScimUser.Email(item.get("email").s(), true, "work")));
                    if (item.containsKey("firstName"))
                        user.setFirstName(item.get("firstName").s());
                    if (item.containsKey("lastName"))
                        user.setLastName(item.get("lastName").s());
                    if (item.containsKey("employeeNumber"))
                        user.setEmployeeNumber(item.get("employeeNumber").s());
                    if (item.containsKey("userType"))
                        user.setUserType(item.get("userType").s());
                    if (item.containsKey("department"))
                        user.setDepartment(item.get("department").s());
                    if (item.containsKey("displayName"))
                        user.setDisplayName(item.get("displayName").s());
                    if (item.containsKey("group"))
                        user.setGroup(List.of(item.get("group").s()));
                    ensureScimCompliance(user);
                    // Establece el tipo de usuario según roles
                    user.setUserType(getUserRoleType(user));
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
        logger.info("listUsers (all) finished, totalResults: {}", response.getTotalResults());
        return response;
    }

    // Eliminar usuario por ID (DELETE /Users/{id})
    public void deleteUser(String id) {
        logger.info("deleteUser called for id: {}", id);
        if (dynamoDbClient != null && usersTable != null && !usersTable.isEmpty()) {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(id).build());
            dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                .tableName(usersTable)
                .key(key)
                .build());
        }
        logger.info("deleteUser finished for id: {}", id);
    }

    private void ensureScimCompliance(ScimUser user) {
        logger.info("ensureScimCompliance called for user: {}", user);
        if (user.getSchemas() == null || user.getSchemas().isEmpty()) {
            user.setSchemas(Collections.singletonList("urn:ietf:params:scim:schemas:core:2.0:User"));
        }
        logger.info("ensureScimCompliance finished for user: {}", user.getId());
    }

    // Crear usuario (POST /Users)
    public ScimUser createUserFromJson(String body) throws JsonProcessingException {
        logger.info("createUserFromJson called with body: {}", body);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(body);
        logger.debug("[SCIM][createUserFromJson] JSON recibido: {}", root);

        ScimUser user = mapper.treeToValue(root, ScimUser.class);
        logger.debug("[SCIM][createUserFromJson] Usuario parseado: {}", user);

        // Verifica campos complejos
        if (root.has("name")) logger.debug("[SCIM][createUserFromJson] Campo 'name' recibido: {}", root.get("name"));
        if (root.has("emails")) logger.debug("[SCIM][createUserFromJson] Campo 'emails' recibido: {}", root.get("emails"));
        if (root.has("phoneNumbers")) logger.debug("[SCIM][createUserFromJson] Campo 'phoneNumbers' recibido: {}", root.get("phoneNumbers"));
        if (root.has("addresses")) logger.debug("[SCIM][createUserFromJson] Campo 'addresses' recibido: {}", root.get("addresses"));
        if (root.has("groups")) logger.debug("[SCIM][createUserFromJson] Campo 'groups' recibido: {}", root.get("groups"));
        if (root.has("group")) logger.debug("[SCIM][createUserFromJson] Campo 'group' recibido: {}", root.get("group"));

        // Corrige el parseo de grupos si viene como array
        if (root.has("groups") && root.get("groups").isArray()) {
            List<String> groupList = new ArrayList<>();
            for (JsonNode g : root.get("groups")) {
                if (g.has("value")) {
                    groupList.add(g.get("value").asText());
                } else if (g.isTextual()) {
                    groupList.add(g.asText());
                }
            }
            if (!groupList.isEmpty()) {
                user.setGroup(groupList);
                logger.debug("[SCIM][createUserFromJson] Asignando grupos desde 'groups': {}", groupList);
            }
        } else if (root.has("group") && root.get("group").isArray()) {
            List<String> groupList = new ArrayList<>();
            for (JsonNode g : root.get("group")) {
                if (g.isTextual()) groupList.add(g.asText());
            }
            if (!groupList.isEmpty()) {
                user.setGroup(groupList);
                logger.debug("[SCIM][createUserFromJson] Asignando grupos desde 'group': {}", groupList);
            }
        }

        // Extraer employeeNumber y department del objeto raíz si existen
        if (root.has("employeeNumber")) {
            user.setEmployeeNumber(root.get("employeeNumber").asText());
            logger.info("employeeNumber (root): {}", root.get("employeeNumber").asText());
        }
        if (root.has("department")) {
            user.setDepartment(root.get("department").asText());
            logger.info("department (root): {}", root.get("department").asText());
        }

        // Extraer de la extensión enterprise si existen
        JsonNode enterpriseNode = root.get("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User");
        if (enterpriseNode != null) {
            logger.debug("[SCIM][createUserFromJson] Extension enterprise recibida: {}", enterpriseNode);
            if (enterpriseNode.has("employeeNumber")) {
                user.setEmployeeNumber(enterpriseNode.get("employeeNumber").asText());
                logger.info("employeeNumber (enterprise): {}", enterpriseNode.get("employeeNumber").asText());
            }
            if (enterpriseNode.has("department")) {
                user.setDepartment(enterpriseNode.get("department").asText());
                logger.info("department (enterprise): {}", enterpriseNode.get("department").asText());
            }
            if (enterpriseNode.has("manager")) {
                logger.debug("[SCIM][createUserFromJson] Manager recibido: {}", enterpriseNode.get("manager"));
                // Si tienes un campo manager en ScimUser, asígnalo aquí
            }
        } else {
            logger.debug("[SCIM][createUserFromJson] No se encontró extensión enterprise en el JSON");
        }

        logger.info("createUserFromJson finished for userId: {}, employeeNumber: {}, department: {}",
            user.getId(), user.getEmployeeNumber(), user.getDepartment());
        logger.debug("[SCIM][createUserFromJson] Usuario final: {}", user);
        return createUser(user);
    }

    public ScimUser replaceUserFromJson(String id, String body) throws JsonProcessingException {
        logger.info("replaceUserFromJson called with id: {}, body: {}", id, body);
        logger.debug("[SCIM][replaceUserFromJson] JSON recibido: {}", body);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(body);

        ScimUser user = mapper.treeToValue(root, ScimUser.class);
        logger.debug("[SCIM][replaceUserFromJson] Usuario parseado: {}", user);

        // Verifica campos complejos
        if (root.has("name")) logger.debug("[SCIM][replaceUserFromJson] Campo 'name' recibido: {}", root.get("name"));
        if (root.has("emails")) logger.debug("[SCIM][replaceUserFromJson] Campo 'emails' recibido: {}", root.get("emails"));
        if (root.has("phoneNumbers")) logger.debug("[SCIM][replaceUserFromJson] Campo 'phoneNumbers' recibido: {}", root.get("phoneNumbers"));
        if (root.has("addresses")) logger.debug("[SCIM][replaceUserFromJson] Campo 'addresses' recibido: {}", root.get("addresses"));
        if (root.has("groups")) logger.debug("[SCIM][replaceUserFromJson] Campo 'groups' recibido: {}", root.get("groups"));
        if (root.has("group")) logger.debug("[SCIM][replaceUserFromJson] Campo 'group' recibido: {}", root.get("group"));

        // Corrige el parseo de grupos si viene como array
        if (root.has("groups") && root.get("groups").isArray()) {
            List<String> groupList = new ArrayList<>();
            for (JsonNode g : root.get("groups")) {
                if (g.has("value")) {
                    groupList.add(g.get("value").asText());
                } else if (g.isTextual()) {
                    groupList.add(g.asText());
                }
            }
            if (!groupList.isEmpty()) {
                user.setGroup(groupList);
                logger.debug("[SCIM][replaceUserFromJson] Asignando grupos desde 'groups': {}", groupList);
            }
        } else if (root.has("group") && root.get("group").isArray()) {
            List<String> groupList = new ArrayList<>();
            for (JsonNode g : root.get("group")) {
                if (g.isTextual()) groupList.add(g.asText());
            }
            if (!groupList.isEmpty()) {
                user.setGroup(groupList);
                logger.debug("[SCIM][replaceUserFromJson] Asignando grupos desde 'group': {}", groupList);
            }
        }

        // Extraer employeeNumber y department del objeto raíz si existen
        if (root.has("employeeNumber")) {
            user.setEmployeeNumber(root.get("employeeNumber").asText());
            logger.info("employeeNumber (root): {}", root.get("employeeNumber").asText());
        }
        if (root.has("department")) {
            user.setDepartment(root.get("department").asText());
            logger.info("department (root): {}", root.get("department").asText());
        }

        // Extraer de la extensión enterprise si existen
        JsonNode enterpriseNode = root.get("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User");
        if (enterpriseNode != null) {
            logger.debug("[SCIM][replaceUserFromJson] Extension enterprise recibida: {}", enterpriseNode);
            if (enterpriseNode.has("employeeNumber")) {
                user.setEmployeeNumber(enterpriseNode.get("employeeNumber").asText());
                logger.info("employeeNumber (enterprise): {}", enterpriseNode.get("employeeNumber").asText());
            }
            if (enterpriseNode.has("department")) {
                user.setDepartment(enterpriseNode.get("department").asText());
                logger.info("department (enterprise): {}", enterpriseNode.get("department").asText());
            }
            if (enterpriseNode.has("manager")) {
                logger.debug("[SCIM][replaceUserFromJson] Manager recibido: {}", enterpriseNode.get("manager"));
                // Si tienes un campo manager en ScimUser, asígnalo aquí
            }
        } else {
            logger.debug("[SCIM][replaceUserFromJson] No se encontró extensión enterprise en el JSON");
        }

        logger.info("replaceUserFromJson finished for id: {}, employeeNumber: {}, department: {}",
            id, user.getEmployeeNumber(), user.getDepartment());
        logger.debug("[SCIM][replaceUserFromJson] Usuario final: {}", user);
        return replaceUser(id, user);
    }

    /**
     * Retorna el tipo de usuario según el rol principal en la lista de roles.
     * Si contiene "ADMIN" (case-insensitive), retorna "ADMIN".
     * Si contiene "USER" (case-insensitive), retorna "USER".
     * Si no contiene ninguno, retorna "UNKNOWN".
     */
    public String getUserRoleType(ScimUser user) {
        if (user.getGroup() != null) {
            // Prioriza ADMIN sobre USER si ambos están presentes
            boolean isAdmin = false;
            boolean isUser = false;
            for (String group : user.getGroup()) {
                if (group == null) continue;
                String g = group.trim().toUpperCase();
                if (g.equals("CDC_APP_PRES_ADMIN") || g.contains("ADMIN") || g.equals("PRESUPUESTO_ADMIN")) {
                    isAdmin = true;
                }
                if (g.equals("CDC_APP_PRES_USER") || g.contains("USER") || g.equals("PRESUPUESTO_USER")) {
                    isUser = true;
                }
            }
            if (isAdmin) return "ADMIN";
            if (isUser) return "USER";
        }
        return "UNKNOWN";
    }
}