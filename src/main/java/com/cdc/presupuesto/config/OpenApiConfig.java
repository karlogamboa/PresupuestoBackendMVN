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

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Value("${spring.application.name:presupuesto-backend}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Presupuesto CDC API")
                        .version("1.0.0")
                        .description("API for Budget Management System CDC - Sistema de Gestión de Presupuestos")
                        .contact(new Contact()
                                .name("CDC Development Team")
                                .email("desarrollo@cdc.com")
                                .url("https://cdc.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://cdc.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Development Server"),
                        new Server()
                                .url("http://3.148.196.75:" + serverPort)
                                .description("Ubuntu Production Server")))
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
