package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.model.SolicitudPresupuesto;
import com.cdc.presupuesto.repository.SolicitudPresupuestoRepository;
import com.cdc.presupuesto.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/solicitudes-presupuesto")
@Tag(name = "Solicitudes de Presupuesto", description = "API para gestionar solicitudes de presupuesto")
@SecurityRequirement(name = "JWT Authentication")
public class SolicitudPresupuestoController {

    private static final Logger logger = LoggerFactory.getLogger(SolicitudPresupuestoController.class);

    @Autowired
    private SolicitudPresupuestoRepository solicitudPresupuestoRepository;

    @Autowired
    private UserInfoService userInfoService;

    @GetMapping
    @Operation(summary = "Obtener todas las solicitudes de presupuesto", 
               description = "Obtiene la lista de todas las solicitudes de presupuesto")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de solicitudes obtenida exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<SolicitudPresupuesto>> getAllSolicitudes(@AuthenticationPrincipal Jwt jwt) {
        try {
            logger.info("Obteniendo todas las solicitudes de presupuesto");
            List<SolicitudPresupuesto> solicitudes = solicitudPresupuestoRepository.findAll();
            logger.info("Se encontraron {} solicitudes", solicitudes.size());
            return ResponseEntity.ok(solicitudes);
        } catch (Exception e) {
            logger.error("Error obteniendo solicitudes de presupuesto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener solicitud por ID", 
               description = "Obtiene una solicitud de presupuesto específica por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solicitud encontrada"),
        @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<SolicitudPresupuesto> getSolicitudById(
            @PathVariable String id,
            @RequestParam(required = false) String solicitudId,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            logger.info("Obteniendo solicitud con ID: {}", id);
            
            // Si no se proporciona solicitudId, usar el mismo ID
            if (solicitudId == null || solicitudId.isEmpty()) {
                solicitudId = id;
            }
            
            Optional<SolicitudPresupuesto> solicitud = solicitudPresupuestoRepository.findById(id, solicitudId);
            
            if (solicitud.isPresent()) {
                logger.info("Solicitud encontrada: {}", solicitud.get().getId());
                return ResponseEntity.ok(solicitud.get());
            } else {
                logger.warn("Solicitud no encontrada para ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error obteniendo solicitud por ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    @Operation(summary = "Crear nueva solicitud de presupuesto", 
               description = "Crea una nueva solicitud de presupuesto en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Solicitud creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<SolicitudPresupuesto> createSolicitud(
            @Parameter(description = "Datos de la nueva solicitud")
            @RequestBody SolicitudPresupuesto solicitud,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            logger.info("Creando nueva solicitud de presupuesto");
            
            // Obtener información del usuario autenticado
            Map<String, Object> userInfo = userInfoService.getUserInfo(jwt);
            String userEmail = jwt.getClaim("email");
            
            // Generar ID único si no existe
            if (solicitud.getId() == null || solicitud.getId().isEmpty()) {
                solicitud.setId(UUID.randomUUID().toString());
            }
            
            // Generar solicitudId si no existe
            if (solicitud.getSolicitudId() == null || solicitud.getSolicitudId().isEmpty()) {
                solicitud.setSolicitudId("REQ-" + System.currentTimeMillis());
            }
            
            // Establecer información del usuario si no está presente
            if (solicitud.getCorreo() == null || solicitud.getCorreo().isEmpty()) {
                solicitud.setCorreo(userEmail);
            }
            
            if (solicitud.getSolicitante() == null || solicitud.getSolicitante().isEmpty()) {
                solicitud.setSolicitante(userInfo.get("nombre") != null ? userInfo.get("nombre").toString() : "");
            }
            
            if (solicitud.getNumeroEmpleado() == null || solicitud.getNumeroEmpleado().isEmpty()) {
                solicitud.setNumeroEmpleado(userInfo.get("numeroEmpleado") != null ? 
                    userInfo.get("numeroEmpleado").toString() : "");
            }
            
            // Establecer fechas
            String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            solicitud.setFecha(fechaActual);
            solicitud.setFechaCreacion(Instant.now());
            solicitud.setFechaActualizacion(Instant.now());
            
            // Establecer estatus por defecto si no existe
            if (solicitud.getEstatusConfirmacion() == null || solicitud.getEstatusConfirmacion().isEmpty()) {
                solicitud.setEstatusConfirmacion("Pendiente");
            }
            
            // Guardar la solicitud
            SolicitudPresupuesto savedSolicitud = solicitudPresupuestoRepository.save(solicitud);
            logger.info("Solicitud creada exitosamente con ID: {}", savedSolicitud.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedSolicitud);
        } catch (Exception e) {
            logger.error("Error creando solicitud de presupuesto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar solicitud de presupuesto", 
               description = "Actualiza una solicitud de presupuesto existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solicitud actualizada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
        @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<SolicitudPresupuesto> updateSolicitud(
            @PathVariable String id,
            @RequestParam(required = false) String solicitudId,
            @Parameter(description = "Datos actualizados de la solicitud")
            @RequestBody SolicitudPresupuesto solicitud,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            logger.info("Actualizando solicitud con ID: {}", id);
            
            // Si no se proporciona solicitudId, usar el mismo ID
            if (solicitudId == null || solicitudId.isEmpty()) {
                solicitudId = id;
            }
            
            // Verificar que la solicitud existe
            Optional<SolicitudPresupuesto> existingSolicitud = solicitudPresupuestoRepository.findById(id, solicitudId);
            if (!existingSolicitud.isPresent()) {
                logger.warn("Solicitud no encontrada para ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            // Mantener los IDs originales
            solicitud.setId(id);
            solicitud.setSolicitudId(solicitudId);
            
            // Mantener fecha de creación original
            solicitud.setFechaCreacion(existingSolicitud.get().getFechaCreacion());
            
            // Actualizar fecha de modificación
            solicitud.setFechaActualizacion(Instant.now());
            
            // Guardar la solicitud actualizada
            SolicitudPresupuesto updatedSolicitud = solicitudPresupuestoRepository.save(solicitud);
            logger.info("Solicitud actualizada exitosamente: {}", updatedSolicitud.getId());
            
            return ResponseEntity.ok(updatedSolicitud);
        } catch (Exception e) {
            logger.error("Error actualizando solicitud: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar solicitud de presupuesto", 
               description = "Elimina una solicitud de presupuesto del sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solicitud eliminada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Map<String, String>> deleteSolicitud(
            @PathVariable String id,
            @RequestParam(required = false) String solicitudId,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            logger.info("Eliminando solicitud con ID: {}", id);
            
            // Si no se proporciona solicitudId, usar el mismo ID
            if (solicitudId == null || solicitudId.isEmpty()) {
                solicitudId = id;
            }
            
            // Verificar que la solicitud existe
            Optional<SolicitudPresupuesto> existingSolicitud = solicitudPresupuestoRepository.findById(id, solicitudId);
            if (!existingSolicitud.isPresent()) {
                logger.warn("Solicitud no encontrada para ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            // Eliminar la solicitud
            solicitudPresupuestoRepository.deleteById(id, solicitudId);
            logger.info("Solicitud eliminada exitosamente: {}", id);
            
            return ResponseEntity.ok(Map.of("message", "Solicitud eliminada exitosamente"));
        } catch (Exception e) {
            logger.error("Error eliminando solicitud: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/usuario/{numeroEmpleado}")
    @Operation(summary = "Obtener solicitudes por número de empleado", 
               description = "Obtiene todas las solicitudes de un empleado específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de solicitudes del empleado"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<SolicitudPresupuesto>> getSolicitudesByEmpleado(
            @PathVariable String numeroEmpleado,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            logger.info("Obteniendo solicitudes para empleado: {}", numeroEmpleado);
            List<SolicitudPresupuesto> solicitudes = solicitudPresupuestoRepository.findByNumEmpleado(numeroEmpleado);
            logger.info("Se encontraron {} solicitudes para el empleado {}", solicitudes.size(), numeroEmpleado);
            return ResponseEntity.ok(solicitudes);
        } catch (Exception e) {
            logger.error("Error obteniendo solicitudes para empleado {}: {}", numeroEmpleado, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/estatus")
    @Operation(summary = "Actualizar estatus de solicitud", 
               description = "Actualiza únicamente el estatus de una solicitud (solo Admin)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estatus actualizado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Solo Admin"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Map<String, Object>> updateEstatus(
            @PathVariable String id,
            @RequestParam(required = false) String solicitudId,
            @RequestBody Map<String, String> estatusRequest,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            logger.info("Actualizando estatus de solicitud con ID: {}", id);
            
            // Verificar que el usuario es Admin
            if (!userInfoService.isAdmin(jwt)) {
                logger.warn("Usuario no autorizado para cambiar estatus de solicitud");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "No autorizado - Solo Admin"));
            }
            
            // Si no se proporciona solicitudId, usar el mismo ID
            if (solicitudId == null || solicitudId.isEmpty()) {
                solicitudId = id;
            }
            
            // Verificar que la solicitud existe
            Optional<SolicitudPresupuesto> existingSolicitud = solicitudPresupuestoRepository.findById(id, solicitudId);
            if (!existingSolicitud.isPresent()) {
                logger.warn("Solicitud no encontrada para ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            String nuevoEstatus = estatusRequest.get("estatus");
            if (nuevoEstatus == null || nuevoEstatus.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Estatus es requerido"));
            }
            
            // Actualizar solo el estatus
            SolicitudPresupuesto solicitud = existingSolicitud.get();
            solicitud.setEstatusConfirmacion(nuevoEstatus);
            solicitud.setFechaActualizacion(Instant.now());
            
            // Guardar la solicitud actualizada
            SolicitudPresupuesto updatedSolicitud = solicitudPresupuestoRepository.save(solicitud);
            logger.info("Estatus actualizado exitosamente para solicitud: {}", updatedSolicitud.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Estatus actualizado exitosamente",
                "solicitud", updatedSolicitud
            ));
        } catch (Exception e) {
            logger.error("Error actualizando estatus de solicitud: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error interno del servidor"));
        }
    }
    
    @PutMapping("/cambiar-estatus")
    @Operation(summary = "Cambiar estatus de solicitud", 
               description = "Cambia el estatus de una solicitud específica (solo Admin)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estatus actualizado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Solo Admin"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Map<String, Object>> cambiarEstatus(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String solicitudId = null;
            String nuevoEstatus = null;
            
            // Manejar diferentes formatos de payload
            if (request.containsKey("solicitudId")) {
                // Formato simple: {"solicitudId": "...", "estatus": "..."}
                solicitudId = (String) request.get("solicitudId");
                nuevoEstatus = (String) request.get("estatus");
            } else if (request.containsKey("solicitud")) {
                // Formato complejo: {"estatusConfirmacion": "...", "solicitud": {...}}
                nuevoEstatus = (String) request.get("estatusConfirmacion");
                @SuppressWarnings("unchecked")
                Map<String, Object> solicitudData = (Map<String, Object>) request.get("solicitud");
                if (solicitudData != null) {
                    solicitudId = (String) solicitudData.get("id");
                    if (solicitudId == null) {
                        solicitudId = (String) solicitudData.get("solicitudId");
                    }
                }
            }
            
            logger.info("Cambiando estatus de solicitud ID: {} a estatus: {}", solicitudId, nuevoEstatus);
            
            // Verificar que el usuario es Admin
            if (!userInfoService.isAdmin(jwt)) {
                logger.warn("Usuario no autorizado para cambiar estatus de solicitud");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "No autorizado - Solo Admin"));
            }
            
            // Validar parámetros requeridos
            if (solicitudId == null || solicitudId.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "solicitudId es requerido"));
            }
            
            if (nuevoEstatus == null || nuevoEstatus.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "estatus es requerido"));
            }
            
            // Buscar la solicitud (usando el mismo ID para ambos campos)
            Optional<SolicitudPresupuesto> existingSolicitud = solicitudPresupuestoRepository.findById(solicitudId, solicitudId);
            if (!existingSolicitud.isPresent()) {
                logger.warn("Solicitud no encontrada para ID: {}", solicitudId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Solicitud no encontrada"));
            }
            
            // Actualizar el estatus
            SolicitudPresupuesto solicitud = existingSolicitud.get();
            String estatusAnterior = solicitud.getEstatusConfirmacion();
            solicitud.setEstatusConfirmacion(nuevoEstatus);
            solicitud.setFechaActualizacion(Instant.now());
            
            // Guardar la solicitud actualizada
            SolicitudPresupuesto updatedSolicitud = solicitudPresupuestoRepository.save(solicitud);
            logger.info("Estatus actualizado exitosamente para solicitud: {} de '{}' a '{}'", 
                       updatedSolicitud.getId(), estatusAnterior, nuevoEstatus);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Estatus actualizado exitosamente",
                "solicitudId", updatedSolicitud.getId(),
                "estatusAnterior", estatusAnterior,
                "nuevoEstatus", nuevoEstatus,
                "solicitud", updatedSolicitud
            ));
        } catch (Exception e) {
            logger.error("Error cambiando estatus de solicitud: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error interno del servidor: " + e.getMessage()));
        }
    }
}
