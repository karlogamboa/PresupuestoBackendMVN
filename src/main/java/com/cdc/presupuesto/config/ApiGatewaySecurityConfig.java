package com.cdc.presupuesto.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("lambda") // Only active when running as Lambda
public class ApiGatewaySecurityConfig {
    // Existing security configuration code
}