package com.cdc.fin.presupuesto.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class SamlUserController {
    private static final Logger logger = LoggerFactory.getLogger(SamlUserController.class);

    @GetMapping("/saml/user")
    public Object samlUser(Authentication authentication) {
        logger.info("SAML /saml/user called. Authentication: {}", authentication);
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        logger.info("Principal class: {}", principal != null ? principal.getClass().getName() : "null");
        if (principal instanceof Saml2AuthenticatedPrincipal samlPrincipal) {
            Map<String, ?> attributes = samlPrincipal.getAttributes();
            logger.info("SAML principal name: {}", samlPrincipal.getName());
            logger.info("SAML attributes: {}", attributes);
            return Map.of(
                "name", samlPrincipal.getName(),
                "attributes", attributes
            );
        }
        logger.info("Authentication is not SAML2AuthenticatedPrincipal, returning raw authentication.");
        return authentication;
    }
}
