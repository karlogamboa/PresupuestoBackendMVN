package com.cdc.presupuesto.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cdc.presupuesto.service.ScimUserService;
import com.cdc.presupuesto.service.ScimGroupService;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.annotation.PostConstruct;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

@RestController
@RequestMapping("/scim/v2")
public class ScimController {

    private static final Logger logger = LoggerFactory.getLogger(ScimController.class);

    private final ScimUserService scimUserService;
    private final ScimGroupService scimGroupService;

    public ScimController(ScimUserService scimUserService, ScimGroupService scimGroupService) {
        this.scimUserService = scimUserService;
        this.scimGroupService = scimGroupService;
    }

    @Value("${scim.token:scim-2025-qa-BEARER-2f8c1e7a4b7d4e8c9a1f6b3c2d5e7f8a}")
    private String scimToken;

    /**
     * Valida el token Bearer SCIM en el header Authorization
     */
    private boolean isScimAuthorized(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return false;
        String token = authHeader.substring(7);
        return scimToken.equals(token);
    }

    // --- Users endpoints ---

    @GetMapping("/Users")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<String> listUsers(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestParam(value = "startIndex", required = false, defaultValue = "1") int startIndex,
        @RequestParam(value = "count", required = false, defaultValue = "100") int count
    ) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\":\"Forbidden\"}");
        }
        logger.info("SCIM listUsers called: startIndex={}, count={}", startIndex, count);
        String response = scimUserService.listUsers();
        // Verifica que cada usuario en la respuesta tenga el atributo "id" en el JSON.
        // Si usas un ObjectMapper, asegúrate que el modelo User tenga la propiedad "id" y que se serialice correctamente.
        return ResponseEntity.ok(response);
    }

    @PostMapping("/Users")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<String> createUser(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestBody String userJson) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\":\"Forbidden\"}");
        }
        try {
            String response = scimUserService.createUser(userJson);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creando usuario SCIM", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Invalid user data\"}");
        }
    }

    @GetMapping("/Users/{id}")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<String> getUser(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @PathVariable String id) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\":\"Forbidden\"}");
        }
        try {
            String userJson = scimUserService.getUser(id);
            if (userJson == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\":\"User not found\"}");
            }
            return ResponseEntity.ok(userJson);
        } catch (Exception e) {
            logger.error("Error consultando usuario SCIM", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Internal error\"}");
        }
    }

    @PutMapping("/Users/{id}")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<String> replaceUser(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @PathVariable String id,
        @RequestBody String userJson) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\":\"Forbidden\"}");
        }
        try {
            String result = scimUserService.replaceUser(id, userJson);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error reemplazando usuario SCIM", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Invalid user data\"}");
        }
    }

    @PatchMapping("/Users/{id}")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<String> patchUser(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @PathVariable String id,
        @RequestBody String patchJson) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\":\"Forbidden\"}");
        }
        try {
            String result = scimUserService.patchUser(id, patchJson);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error modificando usuario SCIM", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Invalid patch data\"}");
        }
    }

    @DeleteMapping("/Users/{id}")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<Void> deleteUser(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @PathVariable String id) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            scimUserService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error eliminando usuario SCIM", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- Groups endpoints ---

    @GetMapping("/Groups")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<String> listGroups(
        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\":\"Forbidden\"}");
        }
        String response = scimGroupService.listGroups();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/Groups")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<String> createGroup(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestBody String groupJson) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\":\"Forbidden\"}");
        }
        try {
            String response = scimGroupService.createGroup(groupJson);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creando grupo SCIM", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Invalid group data\"}");
        }
    }

    @GetMapping("/Groups/{id}")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<String> getGroup(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @PathVariable String id) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\":\"Forbidden\"}");
        }
        String groupJson = scimGroupService.getGroup(id);
        if (groupJson == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\":\"Group not found\"}");
        }
        return ResponseEntity.ok(groupJson);
    }

    @PutMapping("/Groups/{id}")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<String> replaceGroup(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @PathVariable String id,
        @RequestBody String groupJson) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\":\"Forbidden\"}");
        }
        try {
            String result = scimGroupService.replaceGroup(id, groupJson);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error reemplazando grupo SCIM", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Invalid group data\"}");
        }
    }

    @PatchMapping("/Groups/{id}")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<String> patchGroup(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @PathVariable String id,
        @RequestBody String patchJson) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\":\"Forbidden\"}");
        }
        try {
            String result = scimGroupService.patchGroup(id, patchJson);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error modificando grupo SCIM", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Invalid patch data\"}");
        }
    }

    @DeleteMapping("/Groups/{id}")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<Void> deleteGroup(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @PathVariable String id) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            scimGroupService.deleteGroup(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error eliminando grupo SCIM", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Todos los endpoints SCIM requeridos están implementados.
}