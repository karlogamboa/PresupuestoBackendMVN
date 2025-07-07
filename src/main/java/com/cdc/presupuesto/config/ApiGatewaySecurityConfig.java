package com.cdc.presupuesto.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for API Gateway Authorizer
 * When using API Gateway, authentication is handled at the gateway level
 * The application receives pre-authorized requests with user context
 * 
 * For Lambda testing: set security.auth.enabled=false to disable authentication
 */
@Configuration
@EnableWebSecurity
@Profile("lambda")
public class ApiGatewaySecurityConfig {

    @Value("${security.auth.enabled:true}")
    private boolean authEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())
            
            // Configure session management as stateless
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            
        if (authEnabled) {
            // Authentication ENABLED - normal Lambda production mode
            http.authorizeHttpRequests(authz -> authz
                // Allow health check and OpenAPI endpoints
                .requestMatchers(
                    "/health",
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                
                // Allow public endpoints for testing/health check
                .requestMatchers(
                    "/api/debug/**"
                ).permitAll()
                
                // All other requests require authentication (handled by API Gateway)
                .anyRequest().authenticated()
            )
            
            // Add custom filter to extract user context from API Gateway headers
            .addFilterBefore(new ApiGatewayAuthenticationFilter(), 
                UsernamePasswordAuthenticationFilter.class);
        } else {
            // Authentication DISABLED - Lambda testing mode (NO AUTHENTICATION)
            http.authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            );
        }

        return http.build();
    }
}
