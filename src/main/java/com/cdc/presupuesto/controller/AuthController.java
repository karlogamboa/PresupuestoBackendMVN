package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserInfoService userInfoService;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    

    /**
     * Obtiene información del usuario autenticado via API Gateway
     */
    @PostMapping("/api/userInfo")
    public ResponseEntity<Map<String, Object>> getUserInfo() {
        try {
            // Obtener información del usuario desde el contexto de API Gateway
            Map<String, Object> userInfo = userInfoService.getCurrentUserInfo();
            
            logger.debug("Usuario info obtenido para: {}", userInfo.get("email"));
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            logger.error("Error obteniendo información del usuario: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error obteniendo información del usuario"));
        }
    }

    /**
     * Endpoint de debug para ver información de autenticación de API Gateway
     */
    @PostMapping("/api/debug/auth-info")
    public ResponseEntity<Map<String, Object>> getAuthInfo() {
        try {
            Map<String, Object> debugInfo = userInfoService.getDebugInfo();
            
            // Obtener información adicional del contexto de Spring Security
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (auth != null) {
                debugInfo.put("principalName", auth.getName());
                debugInfo.put("principalType", auth.getPrincipal().getClass().getSimpleName());
                debugInfo.put("authorities", auth.getAuthorities().toString());
            }
            
            logger.info("Debug Auth Info - User: {}", debugInfo.get("userEmail"));
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            logger.error("Error obteniendo información de autenticación: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error obteniendo información de autenticación"));
        }
    }

    /**
     * Endpoint de debug para probar el contexto de usuario
     */
    @PostMapping("/api/debug/user-lookup")
    public ResponseEntity<Map<String, Object>> debugUserLookup() {
        try {
            Map<String, Object> debugInfo = userInfoService.getDebugInfo();
            
            logger.info("Debug User Lookup: {}", debugInfo);
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            logger.error("Error in debug user lookup: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint para logout - solo retorna mensaje de éxito
     * (El logout real se maneja en el API Gateway/Authorizer)
     */
    @PostMapping("/api/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Logged out successfully"
        ));
    }

    /**
     * Endpoint de configuración simplificado para API Gateway
     */
    @GetMapping("/api/auth-config")
    public ResponseEntity<Map<String, String>> getAuthConfig() {
        Map<String, String> config = Map.of(
            "authMethod", "API_GATEWAY",
            "version", "1.0"
        );

        // Usar el valor de allowedOrigins para CORS
        String[] origins = allowedOrigins.split(",");
        String originHeader = origins.length > 0 ? origins[0].trim() : "*";

        return ResponseEntity.ok()
            .header("Access-Control-Allow-Origin", originHeader)
            .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
            .header("Access-Control-Allow-Headers", "*")
            .header("Access-Control-Allow-Credentials", "true")
            .body(config);
    }
}





