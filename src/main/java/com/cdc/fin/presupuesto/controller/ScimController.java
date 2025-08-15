package com.cdc.fin.presupuesto.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cdc.fin.presupuesto.service.ScimUserService;
import com.cdc.fin.presupuesto.service.ScimGroupService;
import org.springframework.security.access.prepost.PreAuthorize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import com.cdc.fin.presupuesto.model.ScimUser;
import com.cdc.fin.presupuesto.model.ScimListResponse;
import com.cdc.fin.presupuesto.model.ScimGroup;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.core.JsonProcessingException;

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

    @Value("${scim.token}")
    private String scimToken;

    private boolean isScimAuthorized(String authHeader) {
        // Valida el token Bearer SCIM
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return false;
        String token = authHeader.substring(7);
        return scimToken.equals(token);
    }

    // Endpoints SCIM para Users y Groups
    // SCIM root endpoint (for Okta discovery)
    @GetMapping
    public ResponseEntity<String> scimRoot() {
        // Puedes agregar información adicional como en HomeController del ejemplo Okta
        return ResponseEntity.ok("{\"message\":\"SCIM v2 root endpoint. Use /scim/v2/Users or /scim/v2/Groups.\",\"status\":\"OK\"}");
    }

    // --- Users endpoints ---
    /**
     * Leer usuario por ID (GET /Users/{id}) - SCIM
     */
    @GetMapping("/Users/{id}")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<ScimUser> getUser(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @PathVariable String id) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            ScimUser user = scimUserService.getUser(id);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(user); // HttpStatus.OK
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Crear usuario (POST /Users) - SCIM
     */
    @PostMapping("/Users")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<ScimUser> createUser(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestBody String body // <-- Recibe el body como String
    ) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            ScimUser response = scimUserService.createUserFromJson(body); // <-- Usa el método correcto
            logger.info("SCIM response to Okta (createUser): {}", new ObjectMapper().writeValueAsString(response));
            return ResponseEntity.status(HttpStatus.CREATED).body(response); // HttpStatus.CREATED
        } catch (Exception e) {
            logger.error("Error creating SCIM user", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    /**
     * Buscar usuarios por userName (GET /Users?filter=...) - SCIM
     */
    @GetMapping(value = "/Users", produces = "application/scim+json")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<ScimListResponse<ScimUser>> listUsers(
        @RequestParam(required = false) String filter,
        @RequestParam(defaultValue = "1") int startIndex,
        @RequestParam(defaultValue = "100") int count,
        @RequestParam(required = false) String sortBy
    ) throws JsonProcessingException {
        ScimListResponse<ScimUser> responseBody = scimUserService.listUsers(filter);
        return ResponseEntity.ok()
            .contentType(new MediaType("application", "scim+json"))
            .body(responseBody);
    }

    /**
     * Actualizar usuario (PUT /Users/{id}) - SCIM
     */
    @PutMapping("/Users/{id}")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<ScimUser> replaceUser(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @PathVariable String id,
        @RequestBody String body // <-- Recibe el body como String
    ) {
        logger.debug("[SCIM][ScimController] PUT /Users/{} body={}", id, body);
        if (!isScimAuthorized(authHeader)) {
            logger.debug("[SCIM][ScimController] No autorizado para editar usuario id={}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            ScimUser result = scimUserService.replaceUserFromJson(id, body); // <-- Usa el método correcto
            logger.debug("[SCIM][ScimController] Resultado de replaceUserFromJson: {}", result);
            return ResponseEntity.ok(result); // HttpStatus.OK
        } catch (Exception e) {
            logger.error("Error actualizando usuario SCIM", e);
            logger.debug("[SCIM][ScimController] Error en replaceUser: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Desactivar/reactivar usuario (PATCH /Users/{id}) - SCIM
     */
    @PatchMapping("/Users/{id}")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<ScimUser> patchUser(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @PathVariable String id,
        @RequestBody ScimUser patch) {
        logger.debug("[SCIM][ScimController] PATCH /Users/{} patch={}", id, patch);
        if (!isScimAuthorized(authHeader)) {
            logger.debug("[SCIM][ScimController] No autorizado para patch usuario id={}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            ScimUser result = scimUserService.patchUser(id, patch);
            logger.info("SCIM response to Okta (patchUser): {}", new ObjectMapper().writeValueAsString(result));
            logger.debug("[SCIM][ScimController] Resultado de patchUser: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error modificando usuario SCIM", e);
            logger.debug("[SCIM][ScimController] Error en patchUser: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Eliminar usuario (DELETE /Users/{id}) - SCIM
     */
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
            return ResponseEntity.noContent().build(); // HttpStatus.NO_CONTENT
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- Groups endpoints ---

    @GetMapping("/Groups")
    @PreAuthorize("hasAuthority('ROLE_SCIM')")
    public ResponseEntity<String> listGroups(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestParam(value = "startIndex", required = false, defaultValue = "1") int startIndex,
        @RequestParam(value = "count", required = false, defaultValue = "100") int count,
        @RequestParam(value = "filter", required = false) String filter
    ) {
        if (!isScimAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"message\":\"Forbidden\"}");
        }
        logger.info("SCIM listGroups called: startIndex={}, count={}, filter={}", startIndex, count, filter);
        ScimListResponse<ScimGroup> response = scimGroupService.listGroups(); // Puedes implementar filtrado y paginación si lo requieres

        // Puedes agregar meta y schemas SCIM aquí si Okta lo requiere
        // Ejemplo: {"schemas":["urn:ietf:params:scim:api:messages:2.0:ListResponse"],"totalResults":N,"startIndex":startIndex,"itemsPerPage":count,"Resources":[...]}
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(response);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            logger.error("Error serializando respuesta de grupos SCIM", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Serialization error\"}");
        }
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
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error eliminando grupo SCIM", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}