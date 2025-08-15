
package com.cdc.fin.presupuesto.service;

import org.springframework.stereotype.Service;

@Service
public class UserInfoService {
    public String getNombreProcesadorActual() {
        // Intenta obtener el nombre del usuario autenticado desde el JWT
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            // El nombre de usuario (subject) está en getName()
            return auth.getName();
        }
        return "Procesador";
    }

    public String getPuestoProcesadorActual() {
        // Intenta obtener el puesto desde los claims del JWT (si existe)
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() != null) {
            // Si el puesto se guarda como un claim, podría estar en details o en authorities
            // Aquí solo retornamos el email como ejemplo, ajusta según tu claim real
            return auth.getDetails().toString();
        }
        return "Puesto";
    }
    public String getCorreoProcesadorActual() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() != null) {
            // Si el correo se guarda como un claim, podría estar en details
            return auth.getDetails().toString(); // Ajusta si tienes un objeto custom
        }
        return "aprobador@tudominio.com";
    }
}
