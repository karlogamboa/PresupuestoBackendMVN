package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String ERROR_DESCRIPTION = "error_description";
    private static final String ERROR = "error";

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${okta.oauth2.issuer}")
    private String issuer;

    @Value("${okta.oauth2.client-id}")
    private String clientId;

    @Value("${okta.oauth2.client-secret:}")
    private String clientSecret;

    @Value("${okta.oauth2.audience:api://default}")
    private String audience;

    private final RestTemplate restTemplate = new RestTemplate();
    
    @Autowired
    private UserInfoService userInfoService;

    @PostMapping("/api/userInfo")
    public ResponseEntity<Map<String, Object>> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> userInfo = new HashMap<>();
        // userInfo.put("sub", jwt.getSubject());
        // if (jwt.getClaim("email") != null) userInfo.put("email", jwt.getClaim("email"));
        // if (jwt.getClaim("name") != null) userInfo.put("name", jwt.getClaim("name"));
        // if (jwt.getClaim("preferred_username") != null) userInfo.put("preferred_username", jwt.getClaim("preferred_username"));
        // if (jwt.getClaim("groups") != null) userInfo.put("groups", jwt.getClaim("groups"));
        
        // Agregar información de debug sobre claims disponibles
        logger.debug("Claims disponibles en JWT: {}", jwt.getClaims().keySet());
        
        // Obtener información adicional del usuario desde DynamoDB
        Map<String, Object> dynamoUserInfo = userInfoService.getUserInfo(jwt);
        
        // Agregar toda la información del usuario desde DynamoDB
        userInfo.putAll(dynamoUserInfo);
        
        // Agregar roles como lista
        String userRole = userInfoService.getUserRoles(jwt);
        userInfo.put("role", userRole);
        
        return ResponseEntity.ok(userInfo);
    }

    /**
     * Endpoint de debug para ver todos los claims del JWT
     */
    @PostMapping("/api/debug/jwt-claims")
    public ResponseEntity<Map<String, Object>> getJwtClaims(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("allClaims", jwt.getClaims());
        debugInfo.put("subject", jwt.getSubject());
        debugInfo.put("issuedAt", jwt.getIssuedAt());
        debugInfo.put("expiresAt", jwt.getExpiresAt());
        debugInfo.put("headers", jwt.getHeaders());
        
        logger.info("Debug JWT Claims: {}", jwt.getClaims());
        
        return ResponseEntity.ok(debugInfo);
    }

    /**
     * Endpoint de debug para probar la búsqueda de usuario en DynamoDB
     */
    @PostMapping("/api/debug/user-lookup")
    public ResponseEntity<Map<String, Object>> debugUserLookup(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            // Obtener información del usuario
            Map<String, Object> userInfo = userInfoService.getUserInfo(jwt);
            String userRole = userInfoService.getUserRoles(jwt);
            boolean isAdmin = userInfoService.isAdmin(jwt);
            
            debugInfo.put("userInfoFromDynamoDB", userInfo);
            debugInfo.put("userRoles", List.of(userRole));
            debugInfo.put("isAdmin", isAdmin);
            debugInfo.put("jwtClaims", jwt.getClaims());
            
            logger.info("Debug User Lookup: userInfo={}, roles={}, isAdmin={}", userInfo, userRole, isAdmin);
            
        } catch (Exception e) {
            debugInfo.put("error", e.getMessage());
            logger.error("Error in debug user lookup: {}", e.getMessage(), e);
        }
        
        return ResponseEntity.ok(debugInfo);
    }

    /**
     * Endpoint de debug para probar conectividad con DynamoDB
     */
    @GetMapping("/api/debug/dynamodb-test")
    public ResponseEntity<Map<String, Object>> testDynamoDBConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Probar conexión listando usuarios
            List<com.cdc.presupuesto.model.Usuario> usuarios = 
                userInfoService.getAllUsersForDebug();
            
            result.put("success", true);
            result.put("totalUsers", usuarios.size());
            result.put("users", usuarios);
            result.put("message", "Conexión con DynamoDB exitosa");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "Error conectando con DynamoDB");
            logger.error("Error testing DynamoDB connection: {}", e.getMessage(), e);
        }
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/exchange-token")
    @SuppressWarnings("rawtypes")
    public ResponseEntity<Map<String, Object>> exchangeToken(@RequestBody Map<String, String> request) {
        String authCode = request.get("code");
        String redirectUri = request.get("redirectUri");
        String codeVerifier = request.get("codeVerifier");

        if (authCode == null || authCode.isEmpty()) {
            return buildBadRequest("Authorization code is required");
        }

        try {
            ResponseEntity<Map> tokenResponse = exchangeAuthCodeForTokens(authCode, redirectUri, codeVerifier);
            return buildTokenExchangeResponse(tokenResponse);
        } catch (RestClientException e) {
            logger.error("RestClientException during token exchange: {}", e.getMessage(), e);
            return buildServerError("Token exchange failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during token exchange: {}", e.getMessage(), e);
            return buildServerError("Unexpected error during token exchange: " + e.getMessage());
        }
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> exchangeAuthCodeForTokens(String authCode, String redirectUri, String codeVerifier) {
        String tokenEndpoint = issuer + "/v1/token";
        MultiValueMap<String, String> tokenRequestBody = new LinkedMultiValueMap<>();
        tokenRequestBody.add("grant_type", "authorization_code");
        tokenRequestBody.add("code", authCode);
        tokenRequestBody.add("client_id", clientId);
        tokenRequestBody.add("client_secret", clientSecret);
        tokenRequestBody.add("redirect_uri", redirectUri != null ? redirectUri : "http://localhost:3000/callback");
        if (codeVerifier != null && !codeVerifier.isEmpty()) {
            tokenRequestBody.add("code_verifier", codeVerifier);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");
        headers.set("User-Agent", "PresupuestoBackend/1.0");

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(tokenRequestBody, headers);

        return restTemplate.exchange(
            tokenEndpoint,
            HttpMethod.POST,
            tokenRequest,
            Map.class
        );
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map<String, Object>> buildTokenExchangeResponse(ResponseEntity<Map> tokenResponse) {
        if (tokenResponse.getStatusCode() == HttpStatus.OK && tokenResponse.getBody() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> tokens = tokenResponse.getBody();
            if (tokens != null) {
                Map<String, Object> response = buildTokenResponse(tokens);
                return ResponseEntity.ok(response);
            } else {
                return buildServerError("Empty response from token endpoint");
            }
        } else {
            logger.error("Token exchange failed with status: {}", tokenResponse.getStatusCode());
            if (tokenResponse.getBody() != null) {
                logger.error("Error response: {}", tokenResponse.getBody());
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                ERROR, "invalid_grant",
                ERROR_DESCRIPTION, "Failed to exchange authorization code for tokens. Status: " + tokenResponse.getStatusCode()
            ));
        }
    }
    private Map<String, Object> buildTokenResponse(Map<String, Object> tokens) {
        Map<String, Object> response = new HashMap<>();
        putIfNotNull(response, "access_token", tokens.get("access_token"));
        putIfNotNull(response, "id_token", tokens.get("id_token"));
        putIfNotNull(response, "refresh_token", tokens.get("refresh_token"));
        putIfNotNull(response, "token_type", tokens.get("token_type"));
        putIfNotNull(response, "expires_in", tokens.get("expires_in"));
        putIfNotNull(response, "scope", tokens.get("scope"));
        response.put("issuer", issuer);
        response.put("audience", audience);
        response.put("client_id", clientId);
        response.put("success", true);
        response.put("message", "Token exchange completed successfully");
        return response;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private ResponseEntity<Map<String, Object>> buildBadRequest(String description) {
        return ResponseEntity.badRequest().body(Map.of(
            ERROR, "invalid_request",
            ERROR_DESCRIPTION, description
        ));
    }

    private ResponseEntity<Map<String, Object>> buildServerError(String description) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            ERROR, "server_error",
            ERROR_DESCRIPTION, description
        ));
    }

    @PostMapping("/api/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Logged out successfully"
        ));
    }

    @GetMapping("/api/okta-config")
    public ResponseEntity<Map<String, String>> getOktaConfig() {
        Map<String, String> config = Map.of(
            "issuer", issuer,
            "clientId", clientId
        );

        // Usa el valor de allowedOrigins (de AWS Parameter Store o properties)
        String[] origins = allowedOrigins.split(",");
        String originHeader = origins.length > 0 ? origins[0].trim() : "*";

        return ResponseEntity.ok()
            .header("Access-Control-Allow-Origin", originHeader)
            .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
            .header("Access-Control-Allow-Headers", "*")
            .header("Access-Control-Allow-Credentials", "true")
            .header("Content-Security-Policy", "default-src 'self' https: http: 'unsafe-inline' 'unsafe-eval'")
            .body(config);
    }
}
