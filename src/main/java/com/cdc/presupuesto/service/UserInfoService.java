package com.cdc.presupuesto.service;

import com.cdc.presupuesto.util.UserAuthUtils;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.HashMap;
import com.cdc.presupuesto.model.Solicitante;

/**
 * Servicio para manejo de información de usuarios
 * Simplificado para usar solo información del API Gateway Authorizer
 */
@Service
public class UserInfoService {

    private static final Logger logger = LoggerFactory.getLogger(UserInfoService.class);

    @Autowired
    private SolicitanteService solicitanteService;

    /**
     * Obtiene la información del usuario desde el contexto de API Gateway
     * @return Información del usuario del contexto de autenticación
     */
    public Map<String, Object> getCurrentUserInfo() {
        try {
            String userEmail = UserAuthUtils.getCurrentUserEmail();
            String userId = UserAuthUtils.getCurrentUserId();
            String userName = UserAuthUtils.getCurrentUserName();
            
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", userId != null ? userId : "unknown");
            userInfo.put("nombre", userName != null ? userName : "");
            userInfo.put("email", userEmail);
            logger.debug("Obteniendo información del usuario: {}", userEmail);
            // Buscar solicitante por correo electrónico y setear numeroEmpleado si existe
            Solicitante solicitante = solicitanteService.getSolicitanteByCorreoElectronico(userEmail);
            if (solicitante != null) {
                userInfo.put("numeroEmpleado", solicitante.getNumEmpleado());
                userInfo.put("isAdmin",solicitante.isAprobadorGastos());

            }
            
            // Obtener roles del contexto de autenticación
            if (UserAuthUtils.hasRole("ADMIN") || UserAuthUtils.hasRole("ADMINISTRATOR")) {
                userInfo.put("roles", "ADMIN");
            } else {
                userInfo.put("roles", "USER");
            }

            logger.debug("Usuario info obtenido del API Gateway para: {}", userEmail);
            return userInfo;
            
        } catch (Exception e) {
            logger.error("Error al obtener información del usuario: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener información del usuario", e);
        }
    }

    /**
     * Obtiene el email del usuario actual del contexto de autenticación
     * @return Email del usuario o null si no está disponible
     */
    public String getCurrentUserEmail() {
        return UserAuthUtils.getCurrentUserEmail();
    }

    /**
     * Obtiene el nombre de usuario del contexto de autenticación
     * @return Nombre del usuario o null si no está disponible
     */
    public String getCurrentUserName() {
        return UserAuthUtils.getCurrentUserName();
    }

    /**
     * Verifica si el usuario actual tiene rol de administrador
     * @return true si es administrador, false en caso contrario
     */
    public boolean isCurrentUserAdmin() {
        return UserAuthUtils.hasRole("ADMIN") || UserAuthUtils.hasRole("ADMINISTRATOR");
    }

    /**
     * Obtiene el ID del usuario actual
     * @return ID del usuario
     */
    public String getCurrentUserId() {
        return UserAuthUtils.getCurrentUserId();
    }

    /**
     * Crea información básica de usuario cuando no hay email disponible
     */
    private Map<String, Object> createEmptyUserInfo(String userId, String userName) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", userId != null ? userId : "unknown");
        userInfo.put("nombre", userName != null ? userName : "");
        userInfo.put("email", "");
        userInfo.put("roles", "USER");
        userInfo.put("isAdmin", false);
        return userInfo;
    }

    /**
     * Obtiene información de debug del contexto de autenticación
     * @return Información de debug
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            debugInfo.put("userId", UserAuthUtils.getCurrentUserId());
            debugInfo.put("userEmail", UserAuthUtils.getCurrentUserEmail());
            debugInfo.put("userName", UserAuthUtils.getCurrentUserName());
            debugInfo.put("isAdmin", isCurrentUserAdmin());
            debugInfo.put("authenticationSource", "API Gateway");
            
            return debugInfo;
        } catch (Exception e) {
            logger.error("Error obteniendo información de debug: {}", e.getMessage(), e);
            debugInfo.put("error", e.getMessage());
            return debugInfo;
        }
    }
}
