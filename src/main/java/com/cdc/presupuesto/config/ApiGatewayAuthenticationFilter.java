package com.cdc.presupuesto.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Authentication filter for API Gateway Authorizer
 * Extracts user information from headers set by API Gateway
 */
public class ApiGatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "x-user-id";
    private static final String USER_EMAIL_HEADER = "x-user-email";
    private static final String USER_ROLES_HEADER = "x-user-roles";
    private static final String USER_NAME_HEADER = "x-user-name";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                    @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        // Extract user information from API Gateway headers
        String userId = request.getHeader(USER_ID_HEADER);
        String userEmail = request.getHeader(USER_EMAIL_HEADER);
        String userRoles = request.getHeader(USER_ROLES_HEADER);
        String userName = request.getHeader(USER_NAME_HEADER);

        // If user ID is present, create authentication
        if (userId != null && !userId.trim().isEmpty()) {
            
            // Parse roles from comma-separated string
            Collection<SimpleGrantedAuthority> authorities = parseRoles(userRoles);
            
            // Create principal with user information
            ApiGatewayUserPrincipal principal = new ApiGatewayUserPrincipal(
                userId, userName != null ? userName : userEmail, userEmail);
            
            // Create authentication token
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
            
            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private Collection<SimpleGrantedAuthority> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    /**
     * User principal for API Gateway authenticated users
     */
    public static class ApiGatewayUserPrincipal {
        private final String id;
        private final String name;
        private final String email;

        public ApiGatewayUserPrincipal(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        @Override
        public String toString() {
            return name != null ? name : email != null ? email : id;
        }
    }
}
