package com.cdc.presupuesto.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.beans.factory.annotation.Value;

@RestController
public class LoginController {

    @Value("${stage:/dev}")
    private String stage;

    @GetMapping("/login")
    public ResponseEntity<String> login(@RequestParam(required = false) String error) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        StringBuilder info = new StringBuilder();

        if (error != null) {
            info.append("Authentication failed. Reason: ").append(error).append("\n");
        }

        HttpHeaders headers = new HttpHeaders();

        if (auth instanceof Saml2Authentication samlAuth && auth.isAuthenticated()) {
            info.append("Authenticated user: ").append(samlAuth.getName()).append("\n");
            info.append("SAML info:\n");
            info.append("  Principal: ").append(samlAuth.getPrincipal()).append("\n");
            info.append("  Authorities: ").append(samlAuth.getAuthorities()).append("\n");
            info.append("  Okta SAML attributes: ").append(samlAuth.getPrincipal()).append("\n");

            // Si la autenticación requiere redirección SAML, ajusta el header Location
            String location = getSamlRedirectLocation(samlAuth);
            if (location != null && location.startsWith("/saml2/authenticate/okta")) {
                headers.set("Location", stage + location);
                return ResponseEntity.status(302).headers(headers).body("Redirecting to SAML authentication...");
            }
        } else {
            info.append("Login endpoint. If you see this, authentication is required or failed.");
            // Si hay error, redirige a la URL con stage
            if (error != null) {
                String errorLocation = stage + "/login?error=" + error;
                headers.set("Location", errorLocation);
                return ResponseEntity.status(302).headers(headers).body("Redirecting to login with error...");
            }
        }

        // Ejemplo de generación de enlace Okta con stage
        String oktaUrl = stage + "/saml2/authenticate/okta";
        info.append("<a href=\"").append(oktaUrl).append("\">okta</a>\n");

        return ResponseEntity.ok(info.toString());
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginPost(@RequestParam(required = false) String error) {
        return login(error);
    }

    // Obtiene la URL de redirección SAML si aplica (simulación)
    private String getSamlRedirectLocation(Saml2Authentication samlAuth) {
        // Aquí deberías obtener la URL de redirección real del flujo SAML, por ejemplo:
        // return samlAuth.getRedirectUrl();
        // Simulación: retorna la ruta relativa si aplica
        return null;
    }
}