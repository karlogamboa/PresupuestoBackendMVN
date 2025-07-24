package com.cdc.presupuesto.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración para AWS Lambda + API Gateway + SAML2.
 * Solo SAML2 para autenticación. Sin API Gateway Authorizer.
 */
@Configuration
@EnableWebSecurity
public class Saml2SecurityConfig {

    @Value("${stage:/dev}")
    private String stage;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/health", "/actuator/**").permitAll()
                .requestMatchers("/scim/**").permitAll()
                .requestMatchers(stage + "/saml2/**").permitAll()
                .requestMatchers("/saml2/**").permitAll()
                .requestMatchers("/api/logout").permitAll()
                .requestMatchers("/login").permitAll() 
                .anyRequest().authenticated()
            )
            .saml2Login(saml2 -> {}) // Configuración base de SAML2
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/saml2/**", "/scim/v2/**")
            )
            .logout(logout -> logout
                .logoutUrl("/api/logout")
                .permitAll()
            );

        // Puedes agregar CSRF, CORS, etc. según necesidades para Lambda/API Gateway

        return http.build();
    }
}