package com.cdc.fin.presupuesto.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import com.cdc.fin.presupuesto.util.OktaSAMLHelper;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class SamlUserController {
    private static final Logger logger = LoggerFactory.getLogger(SamlUserController.class);

    
    @Value("${okta.slo.url:https://trial-4567848.okta.com/logout}")
    private String oktaLogoutUrl;

    @GetMapping("/saml/user")
    public Map<String, Object> samlUser(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        logger.info("[SAML] authentication: {}", authentication);
        if (authentication != null) {
            logger.info("[SAML] principal: {}", authentication.getPrincipal());
            logger.info("[SAML] details: {}", authentication.getDetails());
            result.put("principal", authentication.getPrincipal());
            result.put("authorities", authentication.getAuthorities());

            // Extrae claims del JWT si están disponibles
            if (authentication instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token) {
                Object details = token.getDetails();
                if (details instanceof String email) {
                    result.put("email", email);
                }
            }

            // Si el JWT contiene claims de SAML, extraerlos
            // El JWT claims se pueden obtener del SecurityContext si se implementa un custom principal o details
            // Alternativamente, si usas un filtro que parsea el JWT y pone los claims en details como Map
            // Aquí intentamos extraer los claims del principal si es un Map
            if (authentication.getPrincipal() instanceof Map) {
                Map<?, ?> claims = (Map<?, ?>) authentication.getPrincipal();
                for (Map.Entry<?, ?> entry : claims.entrySet()) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            // Si los claims están en details como Map
            if (authentication.getDetails() instanceof Map) {
                Map<?, ?> details = (Map<?, ?>) authentication.getDetails();
                for (Map.Entry<?, ?> entry : details.entrySet()) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }
        logger.info("[SAML] result: {}", result);
        return result;
    }
       
    @GetMapping("/logout")
    public void samlLogout(HttpServletResponse response) throws IOException {
        // Cambia por tu dominio de Okta
        logger.info("[SAML] Redirigiendo a logout de Okta: {}", oktaLogoutUrl);
        response.sendRedirect(oktaLogoutUrl);
    }

}
