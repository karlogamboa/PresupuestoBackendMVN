package com.cdc.fin.presupuesto.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.view.RedirectView;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import com.cdc.fin.presupuesto.config.SecurityConfig;

import java.util.*;

@Controller
@RequestMapping("/login/saml2/sso")
public class Saml2Controller {
    private static final Logger logger = LoggerFactory.getLogger(Saml2Controller.class);

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${frontend.redirect-url}")
    private String frontendRedirectUrl;

    @Autowired
    private SecurityConfig securityConfig;

    @PostMapping("/okta-saml")
    public RedirectView saml2Sso(@RequestParam("SAMLResponse") String samlResponse,
                                 @RequestParam(value = "RelayState", required = false) String relayState) {
        logger.debug("Received SAMLResponse for /login/saml2/sso/okta-saml");
        Map<String, Object> samlClaims = new HashMap<>();
        List<String> groupList = new ArrayList<>();
        String username = null;

        try {
            Assertion assertion = securityConfig.parseSamlResponseIgnoringInResponseTo(samlResponse);
            logger.debug("Parsed SAML assertion: " + assertion.getID());
            if (assertion.getAttributeStatements() != null) {
                for (org.opensaml.saml.saml2.core.AttributeStatement attrStmt : assertion.getAttributeStatements()) {
                    for (org.opensaml.saml.saml2.core.Attribute attr : attrStmt.getAttributes()) {
                        List<Object> values = new ArrayList<>();
                        for (org.opensaml.core.xml.XMLObject valueObj : attr.getAttributeValues()) {
                            values.add(valueObj.getDOM().getTextContent());
                        }
                        if (!values.isEmpty()) {
                            samlClaims.put(attr.getName(), values.size() == 1 ? values.get(0) : values);
                            if ("group".equalsIgnoreCase(attr.getName())) {
                                if (values.size() == 1) {
                                    groupList.add(values.get(0).toString());
                                } else {
                                    for (Object v : values) groupList.add(v.toString());
                                }
                            }
                            if ("email".equalsIgnoreCase(attr.getName())) {
                                username = values.get(0).toString();
                            }
                        }
                    }
                }
            }
            if (groupList.isEmpty()) {
                groupList.add("user");
            }
            if (username == null) {
                username = "unknown";
            }
        } catch (Exception e) {
            logger.warn("Error parsing SAML attributes: " + e.getMessage());
            username = "unknown";
            groupList.add("user");
        }

        String jwt = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .claim("group", groupList)
                .addClaims(samlClaims)
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                .compact();

        String redirectUrl = frontendRedirectUrl + "?token=" + jwt;
        logger.debug("Redirecting to frontend with JWT: " + redirectUrl);
        RedirectView redirectView = new RedirectView(redirectUrl);
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }
}
