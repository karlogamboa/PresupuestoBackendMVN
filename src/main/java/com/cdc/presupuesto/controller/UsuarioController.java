package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.util.UserAuthUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UsuarioController {

    /**
     * Endpoint para obtener los datos SAML del usuario autenticado en la raíz
     */
    @GetMapping("/usuario/saml-info")
    public ResponseEntity<Map<String, Object>> getCurrentUserSamlInfo() {
        String email = UserAuthUtils.getCurrentUserEmail();
        String givenName = UserAuthUtils.getCurrentUserGivenName();
        String familyName = UserAuthUtils.getCurrentUserFamilyName();
        java.util.List<String> roles = UserAuthUtils.getCurrentUserRoles();

        // Evita nulls en Map.of
        String safeEmail = email != null ? email : "";
        String safeGivenName = givenName != null ? givenName : "";
        String safeFamilyName = familyName != null ? familyName : "";
        java.util.List<String> safeRoles = roles != null ? roles : java.util.Collections.emptyList();

        Map<String, Object> extraClaims = Map.of(
            "given_name", safeGivenName,
            "family_name", safeFamilyName
        );

        // Generar el JWT
        String jwt = com.cdc.presupuesto.util.JwtUtils.generateToken(safeEmail, safeRoles, extraClaims);

        Map<String, Object> info = Map.of(
            "email", safeEmail,
            "given_name", safeGivenName,
            "family_name", safeFamilyName,
            "roles", safeRoles,
            "token", jwt != null ? jwt : ""
        );
        return ResponseEntity.ok(info);
    }
}
