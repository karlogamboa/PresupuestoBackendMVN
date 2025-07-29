package com.cdc.fin.presupuesto.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.cdc.fin.presupuesto.util.UserAuthUtils;
import com.cdc.fin.presupuesto.model.ScimUser;

@RestController
public class SamlUserController {
    private static final Logger logger = LoggerFactory.getLogger(SamlUserController.class);

    @Value("${okta.slo.url:https://trial-4567848.okta.com/logout}")
    private String oktaLogoutUrl;

    private final UserAuthUtils userAuthUtils;

    public SamlUserController(UserAuthUtils userAuthUtils) {
        this.userAuthUtils = userAuthUtils;
    }

    @GetMapping("/saml/user")
    public Map<String, Object> samlUser(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        logger.info("[SAML] authentication: {}", authentication);

        String email = authentication.getPrincipal().toString();
        ScimUser scimUser = userAuthUtils.getScimUserByEmail(email);

        if (scimUser != null) {
            result.put("email", email);
            result.put("userName", scimUser.getName());
            result.put("employeeNumber", scimUser.getEmployeeNumber());
            result.put("department", scimUser.getDepartment());
        } else {
            result.put("email", email);
            result.put("userName", null);
            result.put("employeeNumber", null);
            result.put("department", null);
        }

        // ...existing code for logging and extracting other details...
        if (authentication != null) {
            logger.info("[SAML] principal: {}",email);
            logger.info("[SAML] details: {}", authentication.getDetails());
            result.put("principal", email);
            result.put("authorities", authentication.getAuthorities());

            if (authentication instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token) {
                Object details = token.getDetails();
                if (details instanceof String emailToken) {
                    result.put("email_from_token", emailToken);
                }
            }

            if (authentication.getPrincipal() instanceof Map) {
                Map<?, ?> claims = (Map<?, ?>) authentication.getPrincipal();
                for (Map.Entry<?, ?> entry : claims.entrySet()) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
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
