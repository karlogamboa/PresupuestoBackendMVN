package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.model.SolicitudPresupuesto;
import com.cdc.presupuesto.service.SolicitudPresupuestoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/solicitudes-presupuesto")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
@Tag(name = "Solicitudes de Presupuesto", description = "API para gestionar solicitudes de presupuesto")
@SecurityRequirement(name = "JWT Authentication")
public class SolicitudPresupuestoController {

    @Autowired
    private SolicitudPresupuestoService solicitudService;

    @GetMapping
    @Operation(summary = "Obtener solicitudes de presupuesto", 
               description = "Obtiene todas las solicitudes de presupuesto o filtra por número de empleado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de solicitudes obtenida exitosamente",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = SolicitudPresupuesto.class))),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT requerido"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<List<SolicitudPresupuesto>> getSolicitudes(
            @Parameter(description = "Número de empleado para filtrar solicitudes") 
            @RequestParam(required = false) String numeroEmpleado,
            @AuthenticationPrincipal Jwt jwt) {
        List<SolicitudPresupuesto> solicitudes;
        
        if (numeroEmpleado != null && !numeroEmpleado.isEmpty()) {
            solicitudes = solicitudService.findByNumEmpleado(numeroEmpleado);
        } else {
            solicitudes = solicitudService.findAll();
        }
        
        return ResponseEntity.ok(solicitudes);
    }

    @PostMapping
    @Operation(summary = "Crear nueva solicitud de presupuesto", 
               description = "Crea una nueva solicitud de presupuesto y envía notificación por email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solicitud creada exitosamente",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = SolicitudPresupuesto.class))),
        @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado - Token JWT requerido")
    })
    public ResponseEntity<SolicitudPresupuesto> createSolicitud(
            @Parameter(description = "Datos de la nueva solicitud de presupuesto")
            @RequestBody SolicitudPresupuesto solicitud,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userEmail = jwt.getClaim("email");
        String userName = jwt.getClaim("name");
        
        SolicitudPresupuesto newSolicitud = solicitudService.create(solicitud, userEmail, userName);
        return ResponseEntity.ok(newSolicitud);
    }

    @PutMapping("/{id}/{solicitudId}")
    public ResponseEntity<SolicitudPresupuesto> updateSolicitud(
            @PathVariable String id,
            @PathVariable String solicitudId,
            @RequestBody SolicitudPresupuesto solicitud,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userEmail = jwt.getClaim("email");
        String userName = jwt.getClaim("name");
        
        try {
            SolicitudPresupuesto updatedSolicitud = solicitudService.update(id, solicitudId, solicitud, userEmail, userName);
            return ResponseEntity.ok(updatedSolicitud);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/{solicitudId}")
    public ResponseEntity<Void> deleteSolicitud(@PathVariable String id, @PathVariable String solicitudId) {
        solicitudService.deleteById(id, solicitudId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/editar-estatus")
    @Operation(summary = "Editar estatus de solicitud (Solo Admin)", 
               description = "Permite a los administradores cambiar el estatus de una solicitud y enviar notificaciones automáticas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estatus actualizado exitosamente"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Rol de Admin requerido"),
        @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
        @ApiResponse(responseCode = "400", description = "Error al actualizar estatus")
    })
    public ResponseEntity<Map<String, Object>> editarEstatus(
            @Parameter(description = "Datos para cambiar el estatus de la solicitud")
            @RequestBody EditarEstatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        try {
            // Verificar que el usuario tiene rol de Admin
            List<String> groups = jwt.getClaim("groups");
            boolean isAdmin = groups != null && groups.contains("Admin");
            
            if (!isAdmin) {
                return ResponseEntity.status(403).body(Map.of(
                    "status", "error",
                    "message", "Access denied. Admin role required."
                ));
            }

            String userEmail = jwt.getClaim("email");
            String userName = jwt.getClaim("name");
            
            // Actualizar el estatus de la solicitud
            Optional<SolicitudPresupuesto> updatedSolicitud = solicitudService.updateStatus(
                request.getId(),
                request.getSolicitudId(),
                request.getNuevoEstatus(),
                request.getComentarios(),
                userEmail,
                userName
            );
            
            if (updatedSolicitud.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Status updated successfully and notification sent",
                    "solicitud", updatedSolicitud.get()
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to update status: " + e.getMessage()
            ));
        }
    }

    // Clase para el request de editar estatus
    @Schema(description = "Request para cambiar el estatus de una solicitud de presupuesto")
    public static class EditarEstatusRequest {
        @Schema(description = "ID único de la solicitud", example = "uuid-12345", required = true)
        private String id;
        
        @Schema(description = "ID de la solicitud", example = "REQ-1234567890", required = true)
        private String solicitudId;
        
        @Schema(description = "Nuevo estatus de la solicitud", example = "APROBADO", 
                allowableValues = {"PENDIENTE", "APROBADO", "RECHAZADO"}, required = true)
        private String nuevoEstatus;
        
        @Schema(description = "Comentarios adicionales sobre el cambio de estatus", 
                example = "Aprobado después de revisión presupuestaria")
        private String comentarios;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getSolicitudId() { return solicitudId; }
        public void setSolicitudId(String solicitudId) { this.solicitudId = solicitudId; }
        
        public String getNuevoEstatus() { return nuevoEstatus; }
        public void setNuevoEstatus(String nuevoEstatus) { this.nuevoEstatus = nuevoEstatus; }
        
        public String getComentarios() { return comentarios; }
        public void setComentarios(String comentarios) { this.comentarios = comentarios; }
    }
}
