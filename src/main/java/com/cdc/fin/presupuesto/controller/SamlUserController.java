package com.cdc.fin.presupuesto.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class SamlUserController {
    private static final Logger logger = LoggerFactory.getLogger(SamlUserController.class);

    @Value("${okta.slo.url}")
    private String oktaLogoutUrl;

       
    @GetMapping("/logout")
    public void samlLogout(HttpServletResponse response) throws IOException {
        // Cambia por tu dominio de Okta
        logger.info("[SAML] Redirigiendo a logout de Okta: {}", oktaLogoutUrl);
        response.sendRedirect(oktaLogoutUrl);
    }

}
