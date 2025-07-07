package com.cdc.presupuesto.config;

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
 */
@Configuration
@EnableWebSecurity
@Profile("lambda")
public class ApiGatewaySecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())
            
            // Configure session management as stateless
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Allow health check and OpenAPI endpoints
                .requestMatchers(
                    "/health",
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                
                // Allow public endpoints
                .requestMatchers(
                    "/api/exchange-token",
                    "/api/okta-config"
                ).permitAll()
                
                // All other requests require authentication (handled by API Gateway)
                .anyRequest().authenticated()
            )
            
            // Add custom filter to extract user context from API Gateway headers
            .addFilterBefore(new ApiGatewayAuthenticationFilter(), 
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
