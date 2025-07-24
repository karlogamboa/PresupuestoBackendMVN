package com.cdc.presupuesto.service;

import com.cdc.presupuesto.model.SolicitudPresupuesto;
import com.cdc.presupuesto.repository.SolicitudPresupuestoRepository;
import com.cdc.presupuesto.util.UserAuthUtils;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SolicitudPresupuestoService {
    private static final Logger logger = LoggerFactory.getLogger(SolicitudPresupuestoService.class);

    private final SolicitudPresupuestoRepository repository;
    private final EmailService emailService;

    public SolicitudPresupuestoService(SolicitudPresupuestoRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }
    
    /**
     * Custom exception for not found SolicitudPresupuesto.
     */
    class SolicitudNotFoundException extends RuntimeException {
        public SolicitudNotFoundException(String message) {
            super(message);
        }
    }

    public List<SolicitudPresupuesto> findAll() {
        return repository.findAll();
    }

    public Optional<SolicitudPresupuesto> findById(String id, String solicitudId) {
        return repository.findById(id, solicitudId);
    }

    public List<SolicitudPresupuesto> findByNumEmpleado(String numEmpleado) {
        return repository.findByNumEmpleado(numEmpleado);
    }

    public SolicitudPresupuesto create(SolicitudPresupuesto solicitud) {
        // Obtener datos del usuario autenticado vía SAML/OKTA
        String userEmail = UserAuthUtils.getCurrentUserEmail();
        String userName = UserAuthUtils.getCurrentUserName();
        // String numeroEmpleado = UserAuthUtils.getCurrentUserEmployeeNumber(); // Implementar si es necesario
        // String departamento = UserAuthUtils.getCurrentUserDepartment(); // Implementar si es necesario

        String id = UUID.randomUUID().toString();
        String solicitudId = "REQ-" + System.currentTimeMillis();
        
        solicitud.setId(id);
        solicitud.setSolicitudId(solicitudId);
        solicitud.setEstatusConfirmacion("PENDIENTE");
        solicitud.setFechaCreacion(Instant.now());
        solicitud.setFechaActualizacion(Instant.now());
        solicitud.setCreadoPor(userEmail);
        solicitud.setActualizadoPor(userEmail);
        
        SolicitudPresupuesto savedSolicitud = repository.save(solicitud);
        
        // Enviar notificación por email (opcional - no fallar si hay error)
        try {
            // Aquí podrías obtener el email del aprobador desde una configuración o base de datos
            String approverEmail = "aprobador@cdc.com"; // Esto debería venir de configuración
            emailService.sendBudgetRequestNotification(
                userEmail,
                approverEmail,
                solicitudId,
                userName,
                solicitud.getMontoSubtotal()
            );
        } catch (Exception e) {
            // Log el error pero no fallar la creación de la solicitud
            logger.error("Error sending email notification: {}", e.getMessage(), e);
        }
        
        return savedSolicitud;
    }

    public void deleteById(String id, String solicitudId) {
        repository.deleteById(id, solicitudId);
    }

    public Optional<SolicitudPresupuesto> updateStatus(String id, String solicitudId, String nuevoEstatus, 
                                                      String comentarios) {
        // Obtener datos del usuario autenticado vía SAML/OKTA
        String userEmail = UserAuthUtils.getCurrentUserEmail();
        String userName = UserAuthUtils.getCurrentUserName();

        Optional<SolicitudPresupuesto> existingSolicitud = repository.findById(id, solicitudId);
        
        if (existingSolicitud.isPresent()) {
            SolicitudPresupuesto existing = existingSolicitud.get();
            String oldStatus = existing.getEstatusConfirmacion();
            
            // Validar que el nuevo estatus sea válido
            if (!isValidStatus(nuevoEstatus)) {
                throw new IllegalArgumentException("Invalid status: " + nuevoEstatus);
            }
            
            // Actualizar el estatus y comentarios
            existing.setEstatusConfirmacion(nuevoEstatus);
            existing.setComentarios(comentarios);
            existing.setFechaActualizacion(Instant.now());
            existing.setActualizadoPor(userEmail);
            
            SolicitudPresupuesto updatedSolicitud = repository.save(existing);
            
            // Enviar notificación automática si el estatus cambió
            if (!nuevoEstatus.equals(oldStatus)) {
                try {
                    // Enviar notificación al solicitante original
                    String requesterEmail = existing.getCreadoPor();
                    emailService.sendBudgetStatusNotification(
                        userEmail,
                        requesterEmail,
                        solicitudId,
                        nuevoEstatus,
                        comentarios
                    );
                    
                    // Debug log eliminado
                } catch (Exception e) {
                    // Log el error pero no fallar la actualización
                    logger.error("Error sending status notification: {}", e.getMessage(), e);
                }
            }
            
            return Optional.of(updatedSolicitud);
        } else {
            return Optional.empty();
        }
    }
    
    private boolean isValidStatus(String status) {
        return status != null && (
            status.equals("PENDIENTE") || 
            status.equals("APROBADO") || 
            status.equals("RECHAZADO")
        );
    }
}
