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
import java.util.Arrays;

@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Value("${spring.application.name:presupuesto-backend}")
    private String applicationName;
    
    @Value("${lambda.function.url:}")
    private String lambdaFunctionUrl;
    
    @Value("${openapi.development.server.url:http://localhost}")
    private String developmentServerUrl;
    
    @Value("${openapi.production.server.url:}")
    private String productionServerUrl;
    
    @Value("${openapi.cloudfront.url:}")
    private String cloudfrontUrl;
    
    @Value("${security.auth.enabled:true}")
    private boolean authEnabled;
    
    @Autowired
    private Environment environment;

    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = new ArrayList<>();
        
        // Add development server
        servers.add(new Server()
                .url(developmentServerUrl + ":" + serverPort)
                .description("Development Server"));
        
        // Add production server if configured
        if (productionServerUrl != null && !productionServerUrl.isEmpty()) {
            servers.add(new Server()
                    .url(productionServerUrl + ":" + serverPort)
                    .description("Production Server"));
        }
        
        // Add Lambda function URL if available
        if (lambdaFunctionUrl != null && !lambdaFunctionUrl.isEmpty()) {
            servers.add(new Server()
                    .url(lambdaFunctionUrl)
                    .description("AWS Lambda Function"));
        }
        
        // Add CloudFront distribution if configured
        if (cloudfrontUrl != null && !cloudfrontUrl.isEmpty()) {
            servers.add(new Server()
                    .url(cloudfrontUrl)
                    .description("CloudFront Distribution"));
        }
        
        // Determine if we should show JWT security in documentation
        boolean isLambdaProfile = Arrays.asList(environment.getActiveProfiles()).contains("lambda");
        boolean showJwtSecurity = authEnabled && isLambdaProfile;
        
        String apiDescription = "API for Budget Management System CDC - Sistema de Gestión de Presupuestos\n\n" +
                               "Esta API permite gestionar solicitudes de presupuesto, usuarios, áreas, departamentos y más.\n" +
                               "Disponible tanto en EC2 como en AWS Lambda para máxima flexibilidad.\n\n";
        
        if (!authEnabled) {
            apiDescription += "⚠️ **MODO DE PRUEBA**: Autenticación deshabilitada. Todas las APIs son de acceso libre.\n\n";
        } else if (!isLambdaProfile) {
            apiDescription += "🔓 **MODO DESARROLLO**: Sin autenticación requerida en desarrollo local.\n\n";
        } else {
            apiDescription += "🔐 **MODO PRODUCCIÓN**: Autenticación requerida vía API Gateway Authorizer.\n\n";
        }
        
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Presupuesto CDC API")
                        .version("1.0.0")
                        .description(apiDescription)
                        .contact(new Contact()
                                .name("CDC Development Team")
                                .email("desarrollo@cdc.com")
                                .url("https://cdc.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://cdc.com/license")))
                .servers(servers);
        
        // Only add JWT security scheme if authentication is enabled and we're in lambda profile
        if (showJwtSecurity) {
            openAPI.addSecurityItem(new SecurityRequirement()
                        .addList("JWT Authentication"))
                    .components(new Components()
                        .addSecuritySchemes("JWT Authentication", new SecurityScheme()
                                .type(Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(In.HEADER)
                                .name("Authorization")
                                .description("JWT token obtained from API Gateway Authorizer")));
        }
        
        return openAPI;
    }
}
