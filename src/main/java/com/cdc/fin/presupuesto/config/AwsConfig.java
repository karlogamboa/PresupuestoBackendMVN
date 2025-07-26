package com.cdc.fin.presupuesto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class AwsConfig {

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-2")))
                .build();
    }

    @Bean
    public software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public software.amazon.awssdk.services.ses.SesClient sesClient() {
        return software.amazon.awssdk.services.ses.SesClient.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-2")))
                .build();
    }
}
