package com.cdc.presupuesto.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.ArrayList;

@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Value("${spring.application.name:presupuesto-backend}")
    private String applicationName;
    
    @Value("${lambda.function.url:}")
    private String lambdaFunctionUrl;
    
    @Autowired
    private Environment environment;

    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = new ArrayList<>();
        
        // Add development server
        servers.add(new Server()
                .url("http://localhost:" + serverPort)
                .description("Development Server"));
        
        // Add production server
        servers.add(new Server()
                .url("https://3.148.196.75:" + serverPort)
                .description("Ubuntu Production Server"));
        
        // Add Lambda function URL if available
        if (lambdaFunctionUrl != null && !lambdaFunctionUrl.isEmpty()) {
            servers.add(new Server()
                    .url(lambdaFunctionUrl)
                    .description("AWS Lambda Function"));
        }
        
        // Add CloudFront distribution
        servers.add(new Server()
                .url("https://d3i4aa04ftrk87.cloudfront.net")
                .description("CloudFront Distribution"));
        
        return new OpenAPI()
                .info(new Info()
                        .title("Presupuesto CDC API")
                        .version("1.0.0")
                        .description("API for Budget Management System CDC - Sistema de Gestión de Presupuestos\n\n" +
                                   "Esta API permite gestionar solicitudes de presupuesto, usuarios, áreas, departamentos y más.\n" +
                                   "Disponible tanto en EC2 como en AWS Lambda para máxima flexibilidad.")
                        .contact(new Contact()
                                .name("CDC Development Team")
                                .email("desarrollo@cdc.com")
                                .url("https://cdc.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://cdc.com/license")))
                .servers(servers)
                .addSecurityItem(new SecurityRequirement()
                        .addList("JWT Authentication"))
                .components(new Components()
                        .addSecuritySchemes("JWT Authentication", new SecurityScheme()
                                .type(Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(In.HEADER)
                                .name("Authorization")
                                .description("JWT token obtained from Okta authentication")));
    }
}
