package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RestTemplate restTemplate;
    private final JwtService jwtService;

    @Value("${okta.oauth2.issuer}")
    private String issuer;

    @Value("${okta.oauth2.client-id}")
    private String clientId;

    @Value("${okta.oauth2.client-secret}")
    private String clientSecret;

    @Value("${okta.oauth2.redirect-uri}")
    private String redirectUri;

    public AuthController(RestTemplate restTemplate, JwtService jwtService) {
        this.restTemplate = restTemplate;
        this.jwtService = jwtService;
    }

    @GetMapping("/login")
    public void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Genera un 'state' aleatorio y seguro para prevenir ataques CSRF
        String state = generateState();
        request.getSession().setAttribute("OAUTH2_STATE", state);

        // Construye la URL de autorización de Okta
        String authorizationUrl = issuer + "/v1/authorize" +
                "?client_id=" + clientId +
                "&response_type=code" +
                "&scope=openid profile email" +
                "&redirect_uri=" + redirectUri +
                "&state=" + state;

        // Redirige al usuario al endpoint de autorización de Okta
        response.sendRedirect(authorizationUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<Map<String, String>> callback(@RequestParam("code") String code, @RequestParam("state") String state, HttpSession session) {
        // Valida el parámetro 'state'
        String storedState = (String) session.getAttribute("OAUTH2_STATE");
        if (storedState == null || !storedState.equals(state)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Invalid state parameter"));
        }
        session.removeAttribute("OAUTH2_STATE");

        // Intercambia el código de autorización por tokens
        String tokenUrl = issuer + "/v1/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String authHeader = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        headers.set("Authorization", "Basic " + authHeader);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> responseEntity = restTemplate.postForEntity(tokenUrl, requestEntity, JsonNode.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Failed to exchange code for token"));
        }

        JsonNode responseBody = responseEntity.getBody();
        String idToken = responseBody.get("id_token").asText();

        // Extrae la información del usuario del ID Token (ej. email)
        // NOTA: En producción, deberías validar la firma y las claims del ID Token.
        String email = jwtService.getClaimFromToken(idToken, "email");

        // Genera tu propio JWT de sesión para el frontend
        String sessionJwt = jwtService.generateSessionToken(email);

        return ResponseEntity.ok(Collections.singletonMap("session_token", sessionJwt));
    }

    private String generateState() {
        SecureRandom sr = new SecureRandom();
        byte[] randomBytes = new byte[32];
        sr.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}