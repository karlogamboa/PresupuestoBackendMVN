package com.cdc.fin.presupuesto.controller;

import com.cdc.fin.presupuesto.model.ScimUser;
import com.cdc.fin.presupuesto.util.UserAuthUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/user")
public class ApiUserController {
    private static final Logger logger = LoggerFactory.getLogger(ApiUserController.class);

    // @Autowired
    // private ScimUserService scimUserService;
    @Autowired
    private UserAuthUtils userAuthUtils;

    // @GetMapping
    // public ResponseEntity<ScimUser> getCurrentUser(Authentication authentication) {
    //     // Puedes obtener el username del JWT así:
    //     String username = authentication.getName();
    //     ScimUser user = scimUserService.findByUserName(username);
    //     if (user != null) {
    //         return ResponseEntity.ok(user);
    //     } else {
    //         return ResponseEntity.notFound().build();
    //     }
    // }

    public ApiUserController(UserAuthUtils userAuthUtils) {
        this.userAuthUtils = userAuthUtils;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
            }

            // Verifica si el JWT está expirado (usando detalles del authentication si se propaga la excepción)
            if (authentication.getDetails() instanceof Exception ex &&
                ex.getClass().getSimpleName().equalsIgnoreCase("ExpiredJwtException")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "JWT expired"));
            }

            Map<String, Object> result = new HashMap<>();
            logger.info("[SAML] authentication: {}", authentication);

            // Robust principal extraction
            String email = null;
            Object principal = authentication.getPrincipal();
            if (principal instanceof String str) {
                email = str;
            } else if (principal instanceof Map<?, ?> claims && claims.containsKey("email")) {
                email = String.valueOf(claims.get("email"));
            } else {
                email = null;
            }

            ScimUser scimUser = (email != null) ? userAuthUtils.getScimUserByEmail(email) : null;

            if (scimUser != null) {
                result.put("email", email);
                result.put("userName", scimUser.getUserName());
                result.put("employeeNumber", scimUser.getEmployeeNumber());
                result.put("department", scimUser.getDepartment());
                result.put("displayName", scimUser.getDisplayName());
                result.put("firstName", scimUser.getFirstName());
                result.put("lastName", scimUser.getLastName());
                result.put("userType", scimUser.getUserType());
                result.put("active", scimUser.getActive() != null ? scimUser.getActive() : true);
                result.put("group", scimUser.getGroup() != null ? scimUser.getGroup() : List.of());
                result.put("id", scimUser.getId());
                scimUser.syncNameFromFirstLast();
                result.put("name", scimUser.getName() != null ? scimUser.getName() : new ScimUser.Name());
                result.put("schemas", scimUser.getSchemas() != null ? scimUser.getSchemas() : List.of("urn:ietf:params:scim:schemas:core:2.0:User"));
                if (scimUser.getEmails() != null && !scimUser.getEmails().isEmpty()) {
                    result.put("emails", scimUser.getEmails());
                } else if (email != null) {
                    result.put("emails", List.of(new ScimUser.Email(email, true, "work")));
                }
            } else {
                result.put("email", email);
                result.put("userName", null);
                result.put("employeeNumber", null);
                result.put("department", null);
            }

            // ...existing code for logging and extracting other details...
            if (authentication != null) {
                logger.info("[SAML] principal: {}", email);
                logger.info("[SAML] details: {}", authentication.getDetails());
                result.put("principal", email);
                result.put("authorities", authentication.getAuthorities());

                if (authentication instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token) {
                    Object details = token.getDetails();
                    if (details instanceof String emailToken) {
                        result.put("email_from_token", emailToken);
                    }
                }

                if (principal instanceof Map<?, ?> claims) {
                    for (Map.Entry<?, ?> entry : claims.entrySet()) {
                        result.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                if (authentication.getDetails() instanceof Map<?, ?> details) {
                    for (Map.Entry<?, ?> entry : details.entrySet()) {
                        result.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
            }
            // Detecta si el JWT tiene el claim "authType" y lo agrega al resultado
            if (principal instanceof Map<?, ?> claims && claims.containsKey("authType")) {
                result.put("authType", claims.get("authType"));
            }
            logger.info("[SAML] result: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            logger.error("Error in /api/user endpoint: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", ex.getMessage()));
        }
    }
}
