package com.cdc.fin.presupuesto.controller;

import com.cdc.fin.presupuesto.model.ScimUser;
import com.cdc.fin.presupuesto.util.UserAuthUtils;

import java.util.HashMap;
import java.util.Map;

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

        String email = authentication.getPrincipal().toString();
        ScimUser scimUser = userAuthUtils.getScimUserByEmail(email);

        if (scimUser != null) {
            result.put("email", email);
            result.put("userName", scimUser.getUserName());
            result.put("employeeNumber", scimUser.getEmployeeNumber());
            result.put("department", scimUser.getDepartment());
        } else {
            result.put("email", email);
            result.put("userName", null);
            result.put("employeeNumber", null);
            result.put("department", null);
        }

        // ...existing code for logging and extracting other details...
        if (authentication != null) {
            logger.info("[SAML] principal: {}",email);
            logger.info("[SAML] details: {}", authentication.getDetails());
            result.put("principal", email);
            result.put("authorities", authentication.getAuthorities());

            if (authentication instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token) {
                Object details = token.getDetails();
                if (details instanceof String emailToken) {
                    result.put("email_from_token", emailToken);
                }
            }

            if (authentication.getPrincipal() instanceof Map) {
                Map<?, ?> claims = (Map<?, ?>) authentication.getPrincipal();
                for (Map.Entry<?, ?> entry : claims.entrySet()) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            if (authentication.getDetails() instanceof Map) {
                Map<?, ?> details = (Map<?, ?>) authentication.getDetails();
                for (Map.Entry<?, ?> entry : details.entrySet()) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }
        // Detecta si el JWT tiene el claim "authType" y lo agrega al resultado
        if (authentication.getPrincipal() instanceof Map) {
            Map<?, ?> claims = (Map<?, ?>) authentication.getPrincipal();
            if (claims.containsKey("authType")) {
                result.put("authType", claims.get("authType"));
            }
        }
        logger.info("[SAML] result: {}", result);
        return ResponseEntity.ok(result);
    }
}
