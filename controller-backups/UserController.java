package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.config.ApiGatewayAuthenticationFilter.ApiGatewayUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for user information and authentication status
 * Replaces the previous Okta-based user info endpoint
 */
@RestController
@RequestMapping("/api")
public class UserController {

    /**
     * Get current authenticated user information
     * @return User information from API Gateway context
     */
    @GetMapping("/userInfo")
    public ResponseEntity<Map<String, Object>> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                .body(Map.of("error", "User not authenticated"));
        }

        Map<String, Object> userInfo = new HashMap<>();
        
        if (authentication.getPrincipal() instanceof ApiGatewayUserPrincipal principal) {
            userInfo.put("sub", principal.getId());
            userInfo.put("email", principal.getEmail());
            userInfo.put("name", principal.getName());
            userInfo.put("preferred_username", principal.getName());
        } else {
            userInfo.put("sub", authentication.getName());
            userInfo.put("name", authentication.getName());
        }
        
        // Add roles/authorities
        userInfo.put("roles", authentication.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .collect(Collectors.toList()));
        
        userInfo.put("authenticated", true);
        
        return ResponseEntity.ok(userInfo);
    }

    /**
     * Simple health check endpoint
     */
    @GetMapping("/auth/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> status = new HashMap<>();
        status.put("authenticated", authentication != null && authentication.isAuthenticated());
        status.put("authType", "API_GATEWAY");
        
        if (authentication != null && authentication.isAuthenticated()) {
            status.put("principal", authentication.getName());
            status.put("authorities", authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList()));
        }
        
        return ResponseEntity.ok(status);
    }
}
