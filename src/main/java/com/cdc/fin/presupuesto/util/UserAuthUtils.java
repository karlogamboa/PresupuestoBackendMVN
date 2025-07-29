package com.cdc.fin.presupuesto.util;

import com.cdc.fin.presupuesto.model.ScimUser;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public UserAuthUtils(DynamoDbClient dynamoDbClient, @Value("${aws.dynamodb.table.scim-users:}") String usersTable) {
        this.dynamoDbClient = dynamoDbClient;
        this.usersTable = usersTable;
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
                if (item.containsKey("given_name") || item.containsKey("family_name")) {
                    ScimUser.Name name = new ScimUser.Name();
                    if (item.containsKey("given_name")) {
                        name.setGivenName(item.get("given_name").s());
                    }
                    if (item.containsKey("family_name")) {
                        name.setFamilyName(item.get("family_name").s());
                    }
                    user.setName(name);
                }
                if (item.containsKey("email"))
                    user.setEmails(List.of(new ScimUser.Email(item.get("email").s(), true, "work")));
                if (item.containsKey("employee_number"))
                    user.setEmployeeNumber(item.get("employee_number").s());
                if (item.containsKey("user_type"))
                    user.setUserType(item.get("user_type").s());
                    if(item.containsKey("department"))
                    user.setDepartment(item.get("department").s());
                return user;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
