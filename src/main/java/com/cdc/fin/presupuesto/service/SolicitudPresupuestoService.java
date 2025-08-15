package com.cdc.fin.presupuesto.service;

import com.cdc.fin.presupuesto.model.SolicitudPresupuesto;
import com.cdc.fin.presupuesto.repository.SolicitudPresupuestoRepository;
import com.cdc.fin.presupuesto.util.UserAuthUtils;
import com.cdc.fin.presupuesto.model.ScimUser;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SolicitudPresupuestoService {
    private static final Logger logger = LoggerFactory.getLogger(SolicitudPresupuestoService.class);

    private final SolicitudPresupuestoRepository repository;
    private final EmailService emailService;

    @Autowired
    private final UserAuthUtils userAuthUtils;

    @Value("${presupuesto.solicitud.prefix:REQ-}")
    private String solicitudPrefix;

    public SolicitudPresupuestoService(
        SolicitudPresupuestoRepository repository,
        EmailService emailService,
        UserAuthUtils userAuthUtils
    ) {
        this.repository = repository;
        this.emailService = emailService;
        this.userAuthUtils = userAuthUtils;
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
        String emailActual = solicitud.getCorreo();
        ScimUser scimUser = userAuthUtils.getScimUserByEmail(emailActual);

        String userEmail = scimUser != null ? scimUser.getEmail() : emailActual;
        String userName = scimUser != null ? scimUser.getUserName() : null;
        // String numeroEmpleado = scimUser != null ? scimUser.getEmployee_number() : null;
        // String departamento = scimUser != null ? scimUser.getUser_type() : null;

        String id = UUID.randomUUID().toString();
        String solicitudId = solicitudPrefix + System.currentTimeMillis();
        
        solicitud.setId(id);
        solicitud.setSolicitudId(solicitudId);
        solicitud.setEstatusConfirmacion("PENDIENTE");
        solicitud.setFechaCreacion(Instant.now());
        solicitud.setFechaActualizacion(Instant.now());
        solicitud.setCreadoPor(userEmail);
        solicitud.setActualizadoPor(userEmail);
        
        SolicitudPresupuesto savedSolicitud = repository.save(solicitud);
        
        return savedSolicitud;
    }

    public void deleteById(String id, String solicitudId) {
        repository.deleteById(id, solicitudId);
    }

    public Optional<SolicitudPresupuesto> updateStatus(String id, String solicitudId, String nuevoEstatus, 
                                                      String comentarios) {
        // Obtener datos del usuario autenticado vía SAML/OKTA
        // String userEmail = UserAuthUtils.getCurrentUserEmail();
        // String userName = UserAuthUtils.getCurrentUserName();
        String userEmail ="";

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
