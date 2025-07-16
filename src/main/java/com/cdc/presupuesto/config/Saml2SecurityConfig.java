package com.cdc.presupuesto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Configuración para AWS Lambda + API Gateway + SAML2.
 * Solo SAML2 para autenticación. Sin API Gateway Authorizer.
 */
@Configuration
@EnableWebSecurity
public class Saml2SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/health", "/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .saml2Login();
        http
            .logout(logout -> logout
                .logoutUrl("/api/logout")
                .permitAll()
            );

        // Puedes agregar CSRF, CORS, etc. según necesidades para Lambda/API Gateway
        return http.build();
    }
}
