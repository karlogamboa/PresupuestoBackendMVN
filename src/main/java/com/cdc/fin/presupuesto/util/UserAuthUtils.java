package com.cdc.fin.presupuesto.util;

import com.cdc.fin.presupuesto.model.ScimUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;

@Component
public class UserAuthUtils {

    private final DynamoDbClient dynamoDbClient;
    private final String usersTable;

    public UserAuthUtils(DynamoDbClient dynamoDbClient, @Value("${aws.dynamodb.table.prefix}") String tablePrefix) {
        this.dynamoDbClient = dynamoDbClient;
        this.usersTable = tablePrefix + "scim-users";
    }

    public ScimUser getScimUserByEmail(String email) {
        if (dynamoDbClient == null || usersTable == null || usersTable.isEmpty() || email == null) {
            return null;
        }
    QueryRequest queryRequest = QueryRequest.builder()
            .tableName(usersTable)
            .indexName("UserNameIndex") // Debes tener este GSI en DynamoDB
            .keyConditionExpression("userName = :v_user")
            .expressionAttributeValues(Map.of(":v_user", AttributeValue.builder().s(email).build()))
            .build();
        QueryResponse response = dynamoDbClient.query(queryRequest);
        if (response.hasItems() && !response.items().isEmpty()) {
            Map<String, AttributeValue> item = response.items().get(0);
            try {
                ScimUser user = new ScimUser();
                user.setId(item.get("id").s());
                user.setUserName(item.get("userName").s());
                user.setActive(item.containsKey("active") ? item.get("active").bool() : true);
                if (item.containsKey("firstName") || item.containsKey("lastName")) {
                    ScimUser.Name name = new ScimUser.Name();
                    if (item.containsKey("firstName")) {
                        name.setGivenName(item.get("firstName").s());
                        user.setFirstName(item.get("firstName").s()); // SAML mapping
                    }
                    if (item.containsKey("lastName")) {
                        name.setFamilyName(item.get("lastName").s());
                        user.setLastName(item.get("lastName").s()); // SAML mapping
                    }
                    user.setName(name);
                }
                if (item.containsKey("email"))
                    user.setEmails(List.of(new ScimUser.Email(item.get("email").s(), true, "work")));
                if (item.containsKey("employeeNumber"))
                    user.setEmployeeNumber(item.get("employeeNumber").s());
                if (item.containsKey("userType"))
                    user.setUserType(item.get("userType").s());
                if (item.containsKey("department"))
                    user.setDepartment(item.get("department").s());
                if (item.containsKey("displayName"))
                    user.setDisplayName(item.get("displayName").s());
                // SAML/Group attributes
                if (item.containsKey("group")) {
                    // DynamoDB stores as string, split if needed
                    String groupStr = item.get("group").s();
                    user.setGroup(java.util.Arrays.asList(groupStr.split(",")));
                }
                return user;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
