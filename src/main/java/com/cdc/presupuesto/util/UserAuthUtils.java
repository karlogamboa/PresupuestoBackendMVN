package com.cdc.presupuesto.util;

import com.cdc.presupuesto.config.ApiGatewayAuthenticationFilter.ApiGatewayUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class to extract user information from API Gateway authentication
 * Replaces JWT-based user extraction
 */
public class UserAuthUtils {
    
    /**
     * Get the current authenticated user ID
     * @return User ID from API Gateway context or "anonymous" if not authenticated
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        
        if (authentication.getPrincipal() instanceof ApiGatewayUserPrincipal principal) {
            return principal.getId();
        }
        
        return authentication.getName() != null ? authentication.getName() : "anonymous";
    }
    
    /**
     * Get the current authenticated user email
     * @return User email from API Gateway context or null if not available
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        if (authentication.getPrincipal() instanceof ApiGatewayUserPrincipal principal) {
            return principal.getEmail();
        }
        
        return null;
    }
    
    /**
     * Get the current authenticated user name
     * @return User name from API Gateway context or null if not available
     */
    public static String getCurrentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        if (authentication.getPrincipal() instanceof ApiGatewayUserPrincipal principal) {
            return principal.getName();
        }
        
        return authentication.getName();
    }
    
    /**
     * Check if current user is authenticated
     * @return true if user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
    
    /**
     * Check if current user has a specific role
     * @param role Role to check (without ROLE_ prefix)
     * @return true if user has the role, false otherwise
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> 
                    authority.getAuthority().equals("ROLE_" + role.toUpperCase()) ||
                    authority.getAuthority().equals(role.toUpperCase()));
    }
}
