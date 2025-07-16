package com.cdc.presupuesto.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Utility class to extract user information from Spring Security (SAML2)
 * AWS Lambda + API Gateway + SAML2 (sin API Gateway Authorizer)
 */
public class UserAuthUtils {

    /**
     * Get the current authenticated user ID (username)
     * @return User ID or "anonymous" if not authenticated
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return authentication.getName() != null ? authentication.getName() : "anonymous";
    }

    /**
     * Get the current authenticated user email (if available)
     * @return User email or null if not available
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            // Si el UserDetails tiene email, obténlo aquí (puede requerir implementación personalizada)
            // return userDetails.getEmail();
            return null;
        }
        return null;
    }

    /**
     * Get the current authenticated user name
     * @return User name or null if not available
     */
    public static String getCurrentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
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

    /**
     * Nota: Si se utiliza SAML2, el logout debe gestionarse mediante el flujo SAML2 estándar.
     * El endpoint personalizado /api/logout no tiene sentido en este contexto.
     */
}
