package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.model.SolicitudPresupuesto;
import com.cdc.presupuesto.repository.SolicitudPresupuestoRepository;
import com.cdc.presupuesto.util.UserAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class SolicitudPresupuestoController {

    private static final Logger logger = LoggerFactory.getLogger(SolicitudPresupuestoController.class);

    @Autowired
    private SolicitudPresupuestoRepository solicitudPresupuestoRepository;



    @GetMapping
    // No Swagger/OpenAPI annotations
    public ResponseEntity<List<SolicitudPresupuesto>> getAllSolicitudes() {
        try {
            List<SolicitudPresupuesto> solicitudes = solicitudPresupuestoRepository.findAll();
            return ResponseEntity.ok(solicitudes);
        } catch (Exception e) {
            logger.error("Error obteniendo solicitudes de presupuesto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudPresupuesto> getSolicitudById(
            @PathVariable String id,
            @RequestParam(required = false) String solicitudId) {
        try {
            // Si no se proporciona solicitudId, usar el mismo ID
            if (solicitudId == null || solicitudId.isEmpty()) {
                solicitudId = id;
            }
            
            Optional<SolicitudPresupuesto> solicitud = solicitudPresupuestoRepository.findById(id, solicitudId);
            
            if (solicitud.isPresent()) {
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
    public ResponseEntity<SolicitudPresupuesto> createSolicitud(
            @RequestBody SolicitudPresupuesto solicitud) {
        try {
            // Obtener información del usuario autenticado
            String userEmail = UserAuthUtils.getCurrentUserEmail();
            String userName = UserAuthUtils.getCurrentUserName();
            String userId = UserAuthUtils.getCurrentUserId();

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
                solicitud.setSolicitante(userName);
            }

            if (solicitud.getNumeroEmpleado() == null || solicitud.getNumeroEmpleado().isEmpty()) {
                solicitud.setNumeroEmpleado(userId);
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
            return ResponseEntity.status(HttpStatus.CREATED).body(savedSolicitud);
        } catch (Exception e) {
            logger.error("Error creando solicitud de presupuesto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SolicitudPresupuesto> updateSolicitud(
            @PathVariable String id,
            @RequestParam(required = false) String solicitudId,
            @RequestBody SolicitudPresupuesto solicitud) {
        try {
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
            return ResponseEntity.ok(updatedSolicitud);
        } catch (Exception e) {
            logger.error("Error actualizando solicitud: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSolicitud(
            @PathVariable String id,
            @RequestParam(required = false) String solicitudId) {
        try {
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
            return ResponseEntity.ok(Map.of("message", "Solicitud eliminada exitosamente"));
        } catch (Exception e) {
            logger.error("Error eliminando solicitud: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/usuario/{numeroEmpleado}")
    public ResponseEntity<List<SolicitudPresupuesto>> getSolicitudesByEmpleado(
            @PathVariable String numeroEmpleado) {
        try {
            List<SolicitudPresupuesto> solicitudes = solicitudPresupuestoRepository.findByNumEmpleado(numeroEmpleado);
            return ResponseEntity.ok(solicitudes);
        } catch (Exception e) {
            logger.error("Error obteniendo solicitudes para empleado {}: {}", numeroEmpleado, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/estatus")
    public ResponseEntity<Map<String, Object>> updateEstatus(
            @PathVariable String id,
            @RequestParam(required = false) String solicitudId,
            @RequestBody Map<String, String> estatusRequest) {
        try {
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
    public ResponseEntity<Map<String, Object>> cambiarEstatus(
            @RequestBody Map<String, Object> request) {
        try {
            String id = null;
            String solicitudId = null;
            String nuevoEstatus = null;
            // Manejar diferentes formatos de payload
            if (request.containsKey("solicitud")) {
                nuevoEstatus = (String) request.get("estatusConfirmacion");
                @SuppressWarnings("unchecked")
                Map<String, Object> solicitudData = (Map<String, Object>) request.get("solicitud");
                if (solicitudData != null) {
                    id = (String) solicitudData.get("id");
                    solicitudId = (String) solicitudData.get("solicitudId");
                }
            } else if (request.containsKey("solicitudId")) {
                solicitudId = (String) request.get("solicitudId");
                nuevoEstatus = (String) request.get("estatus");
                id = solicitudId; // fallback si solo viene uno
            }
            // Validar parámetros requeridos
            if (id == null || id.isEmpty() || solicitudId == null || solicitudId.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "id y solicitudId son requeridos"));
            }
            Optional<SolicitudPresupuesto> existingSolicitud = solicitudPresupuestoRepository.findById(id, solicitudId);
            if (!existingSolicitud.isPresent()) {
                logger.warn("Solicitud no encontrada para ID: {} y solicitudId: {}", id, solicitudId);
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





