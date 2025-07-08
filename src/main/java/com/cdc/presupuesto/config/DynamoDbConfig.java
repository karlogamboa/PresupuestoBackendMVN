package com.cdc.presupuesto.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.ses.SesClient;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    @Value("${aws.region:us-east-2}")
    private String region;

    @Value("${aws.dynamodb.endpoint:}")
    private String endpoint;

    @Value("${aws.dynamodb.table.prefix:presupuesto-}")
    private String tablePrefix;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());

        // For local development - use endpoint override if provided
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        // Check if running on Lambda
        String lambdaTaskRoot = System.getenv("LAMBDA_TASK_ROOT");
        if (lambdaTaskRoot != null) {
            // Lambda-specific optimizations can be added here
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    // Getter para que otros componentes puedan acceder al prefijo de tablas
    public String getTablePrefix() {
        return tablePrefix;
    }
}
