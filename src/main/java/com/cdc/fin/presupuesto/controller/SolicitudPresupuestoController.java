package com.cdc.fin.presupuesto.controller;

import com.cdc.fin.presupuesto.model.SolicitudPresupuesto;
import com.cdc.fin.presupuesto.repository.SolicitudPresupuestoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cdc.fin.presupuesto.util.UserAuthUtils;

import jakarta.servlet.http.HttpServletResponse;

import com.cdc.fin.presupuesto.model.ScimUser;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cdc.fin.presupuesto.service.UserInfoService;
import com.cdc.fin.presupuesto.service.EmailService;
import com.cdc.fin.presupuesto.service.ProcesarSolicitudesService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/solicitudes-presupuesto")
public class SolicitudPresupuestoController {

    private static final Logger logger = LoggerFactory.getLogger(SolicitudPresupuestoController.class);

    private final SolicitudPresupuestoRepository solicitudPresupuestoRepository;
    private final ProcesarSolicitudesService procesarSolicitudesService;
    private final UserAuthUtils userAuthUtils;
    
    private final EmailService emailService;


    @Autowired
    public SolicitudPresupuestoController(ProcesarSolicitudesService procesarSolicitudesService,
                                          SolicitudPresupuestoRepository solicitudPresupuestoRepository,
                                          UserAuthUtils userAuthUtils,
                                          EmailService emailService) {

        this.procesarSolicitudesService = procesarSolicitudesService;
        this.solicitudPresupuestoRepository = solicitudPresupuestoRepository;
        this.userAuthUtils = userAuthUtils;
        this.emailService = emailService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllSolicitudes(
            @RequestParam Map<String, String> params) {
        try {
            int page = params.containsKey("page") ? Integer.parseInt(params.get("page")) : 0;
            int size = params.containsKey("size") ? Integer.parseInt(params.get("size")) : 20;

            // Validar que page no sea menor que cero
            if (page < 0) {
                page = 0;
            }

            // Elimina page y size del mapa de filtros
            params.remove("page");
            params.remove("size");

            // Llama al servicio/repositorio con filtros dinámicos (sin paginación JPA)
            List<SolicitudPresupuesto> solicitudes = solicitudPresupuestoRepository.findByDynamicFilters(params);

            // Simula paginación en memoria si es necesario
            int fromIndex = Math.min(page * size, solicitudes.size());
            int toIndex = Math.min(fromIndex + size, solicitudes.size());
            List<SolicitudPresupuesto> pageContent = solicitudes.subList(fromIndex, toIndex);

            int totalPages = (int) Math.ceil((double) solicitudes.size() / size);

            return ResponseEntity.ok(Map.of(
                "content", pageContent,
                "totalElements", solicitudes.size(),
                "totalPages", totalPages,
                "page", page,
                "size", size
            ));
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
            String email = solicitud.getCorreo();
            ScimUser scimUser = userAuthUtils.getScimUserByEmail(email);

            String userEmail = scimUser != null ? scimUser.getEmail() : email;
            String userName = scimUser != null ? scimUser.getUserName() : null;
            String numeroEmpleado = scimUser != null ? scimUser.getEmployeeNumber() : null;
            String departamento = scimUser != null ? scimUser.getDepartment() : null;

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
                solicitud.setNumeroEmpleado(numeroEmpleado);
            }
            if (solicitud.getDepartamento() == null || solicitud.getDepartamento().isEmpty()) {
                solicitud.setDepartamento(departamento);
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

            // --- ENVÍO DE CORREOS NUEVA SOLICITUD ---
            try {
                // 5A. Correo al aprobador
                emailService.sendNuevaSolicitudAprobadorEmail(savedSolicitud);     
        
                // 5B. Correo al solicitante
                String correoSolicitante = savedSolicitud.getCorreo();
                if (correoSolicitante != null && !correoSolicitante.isEmpty()) {
                    emailService.sendNuevaSolicitudSolicitanteEmail(correoSolicitante, savedSolicitud);
                } else {
                    logger.warn("No se envió correo al solicitante porque el correo es nulo o vacío");
                }
            } catch (Exception ex) {
                logger.error("No se pudo enviar correo de nueva solicitud: {}", ex.getMessage(), ex);
            }
            // --- FIN ENVÍO DE CORREOS ---

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

    
    @GetMapping("/procesar")
    public ResponseEntity<byte[]> exportarExcel() {
        try {
            byte[] excelBytes = procesarSolicitudesService.procesarYExportarExcel();
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"solicitudes.xlsx\"")
                .body(excelBytes);
        } catch (Exception ex) {
            logger.error("Error exportando Excel", ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Replace Lambda-specific POST endpoint with standard Spring Boot endpoint
    @PostMapping("/procesar")
    public ResponseEntity<byte[]> procesarSolicitudesPost() {
        logger.info("POST /api/solicitudes-presupuesto/procesar endpoint invoked (App Runner)");
        try {
            logger.info("Llamando a procesarYExportarExcel...");
            byte[] excelBytes = procesarSolicitudesService.procesarYExportarExcel();
            logger.info("Procesamiento y exportación completado. Bytes generados: {}", excelBytes.length);

            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"solicitudes.xlsx\"")
                .body(excelBytes);
        } catch (Exception ex) {
            logger.error("Error en procesarYExportarExcel: {}", ex.getMessage(), ex);
            // Devuelve un error 500 con un cuerpo JSON como bytes
            String errorJson = "{\"success\":false,\"message\":\"Error procesando solicitudes\"}";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "application/json")
                .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @PutMapping("/cambiar-estatus")
    public ResponseEntity<Map<String, Object>> cambiarEstatus(
            @RequestBody Map<String, Object> request) {
        try {
            String id = null;
            String solicitudId = null;
            String nuevoEstatus = null;
            String userLogueado = request.get("userLogueado") != null ? request.get("userLogueado").toString() : "";
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

            // --- ENVÍO DE CORREO SEGÚN CAMBIO DE ESTATUS ---
            try {
                String motivoRechazo = request.get("motivoRechazo") != null ? request.get("motivoRechazo").toString() : "";
                String correoSolicitante = solicitud.getCorreo();
                // Obtener el displayName del usuario logueado (aprobador)
                String nombreAprobador = "";
                try {
                    // Obtener el correo del usuario autenticado desde el header (API Gateway Authorizer)
                    ScimUser scimUserAprobador = userAuthUtils.getScimUserByEmail(userLogueado);
                    if (scimUserAprobador != null && scimUserAprobador.getDisplayName() != null && !scimUserAprobador.getDisplayName().isEmpty()) {
                        nombreAprobador = scimUserAprobador.getDisplayName();
                    } else {
                        nombreAprobador = userLogueado;
                    }
                } catch (Exception ex) {
                    logger.warn("No se pudo obtener el nombre del aprobador, usando correo. Error: {}", ex.getMessage());
                    nombreAprobador = "";
                }
                emailService.sendNotificacionCambioEstatus(
                    estatusAnterior,
                    nuevoEstatus,
                    correoSolicitante,
                    solicitud,
                    motivoRechazo,
                    nombreAprobador,
                    ""
                );
            } catch (Exception ex) {
                logger.warn("No se pudo enviar correo de notificación de estatus: {}", ex.getMessage());
            }
            // --- FIN ENVÍO DE CORREO ---

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