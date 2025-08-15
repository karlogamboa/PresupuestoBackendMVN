package com.cdc.fin.presupuesto.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;

@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @Autowired
    private Environment environment;

    @Value("${netsuite.consumerKey}")
    private String consumerKey;
    @Value("${netsuite.consumerSecret}")
    private String consumerSecret;
    @Value("${netsuite.accessToken}")
    private String accessToken;
    @Value("${netsuite.tokenSecret}")
    private String tokenSecret;
    @Value("${netsuite.realm}")
    private String realm;
    @Value("${netsuite.baseUrl}")
    private String baseUrl;

    @GetMapping(value = "/health", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> health() {
        log.info("Health check requested");
        Map<String, Object> status = Map.of(
                "status", "UP",
                "service", "Presupuesto Backend",
                "timestamp", Instant.now().toEpochMilli()
        );

        // Lista de todas las propiedades relevantes
        String[] propertyKeys = {
            // Netsuite
            "netsuite.consumerKey",
            "netsuite.consumerSecret",
            "netsuite.accessToken",
            "netsuite.tokenSecret",
            "netsuite.realm",
            "netsuite.baseUrl",
            // CORS
            "cors.allowed-origins",
            "cors.allowed-methods",
            "cors.allowed-headers",
            "cors.allow-credentials",
            // Email
            "email.from",
            "email.charset",
            // Frontend
            "frontend.redirect-url",
            // SAML2
            "spring.security.saml2.relyingparty.registration.okta-saml.assertingparty.sso-url",
            "spring.security.saml2.relyingparty.registration.okta-saml.assertingparty.entity-id",
            "spring.security.saml2.relyingparty.registration.okta-saml.assertingparty.metadata-uri",
            "spring.security.saml2.relyingparty.registration.okta-saml.assertion-consumer-service.location",
            "spring.security.saml2.relyingparty.registration.okta-saml.entity-id",
            "spring.security.saml2.relyingparty.registration.okta-saml.authentication-uri",
            "okta.slo.url",
            // Security
            "security.hsts.exclude-paths",
            "security.jwt.expiration-ms",
            "security.auth.enabled",
            "security.jwt.secret",
            // AWS
            "aws.parameterstore.enabled",
            "aws.region",
            // SCIM
            "scim.enabled",
            "scim.base-path",
            "scim.token",
            // DynamoDB
            "aws.dynamodb.table.prefix",
            "aws.dynamodb.table.proveedores",
            "aws.dynamodb.table.categoriasGasto",
            "aws.dynamodb.table.solicitudes",
            "aws.dynamodb.table.departamentos",
            "aws.dynamodb.table.scim-users",
            "aws.dynamodb.table.scim-groups",
            // Spring
            "spring.web.encoding.enabled",
            "spring.main.lazy-initialization",
            "spring.autoconfigure.exclude",
            "spring.datasource.url",
            "spring.sql.init.mode",
            // API
            "api.stage"
        };

        Map<String, String> allParams = new LinkedHashMap<>();
        Map<String, String> allOrigins = new LinkedHashMap<>();
        Set<String> found = new HashSet<>();
        boolean foundAwsParameter = false;

        for (String key : propertyKeys) {
            String value = environment.getProperty(key);
            allParams.put(key, value);
            String origin = getPropertyOrigin(key);
            allOrigins.put(key, origin);
            if ("AWS Parameter Store".equals(origin)) {
                foundAwsParameter = true;
            }
            found.add(key);
        }

        // Opcional: agrega cualquier otra propiedad presente en el Environment que no esté listada
        if (environment instanceof ConfigurableEnvironment env) {
            for (PropertySource<?> ps : env.getPropertySources()) {
                if (ps.getSource() instanceof java.util.Map<?,?> map) {
                    for (Object k : map.keySet()) {
                        String key = String.valueOf(k);
                        if (!found.contains(key)) {
                            String value = environment.getProperty(key);
                            allParams.put(key, value);
                            String origin = getPropertyOrigin(key);
                            allOrigins.put(key, origin);
                            if ("AWS Parameter Store".equals(origin)) {
                                foundAwsParameter = true;
                            }
                            found.add(key);
                        }
                    }
                }
            }
        }

        // Construye el HTML
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Health Check</title>");
        html.append("<style>table{border-collapse:collapse;}th,td{border:1px solid #ccc;padding:4px 8px;}th{background:#eee;}</style>");
        html.append("</head><body>");
        html.append("<h2>Presupuesto Backend - Health Check</h2>");
        html.append("<ul>");
        html.append("<li><b>Status:</b> ").append(status.get("status")).append("</li>");
        html.append("<li><b>Service:</b> ").append(status.get("service")).append("</li>");
        html.append("<li><b>Timestamp:</b> ").append(status.get("timestamp")).append("</li>");
        html.append("<li><b>AWS Parameter Store activo:</b> ").append(foundAwsParameter ? "Sí" : "No").append("</li>");
        html.append("</ul>");

        html.append("<table>");
        html.append("<tr><th>Propiedad</th><th>Origen</th><th>Valor</th><th>AWS</th></tr>");
        for (String key : allParams.keySet()) {
            String origin = allOrigins.get(key);
            boolean isAws = "AWS Parameter Store".equals(origin);
            html.append("<tr>");
            html.append("<td>").append(key).append("</td>");
            html.append("<td>").append(origin).append("</td>");
            html.append("<td>").append(allParams.get(key) == null ? "<i>null</i>" : allParams.get(key)).append("</td>");
            html.append("<td>").append(isAws ? "<b>OK</b>" : "").append("</td>");
            html.append("</tr>");
        }
        html.append("</table>");
        html.append("</body></html>");

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html.toString());
    }

    private String getPropertyOrigin(String propertyName) {
        if (environment instanceof ConfigurableEnvironment env) {
            for (PropertySource<?> ps : env.getPropertySources()) {
                if (ps.containsProperty(propertyName)) {
                    String name = ps.getName().toLowerCase();
                    // Detecta AWS Parameter Store
                    if (name.contains("awsparam") || name.contains("ssm") || name.contains("aws-secretsmanager") || name.contains("aws")) {
                        return "AWS Parameter Store";
                    }
                    // Detecta archivos de propiedades por perfil (simplificado)
                    if (name.contains("applicationconfig") || name.contains("applicationproperties")) {
                        if (name.contains("application-qa.properties")) {
                            return "properties (qa.properties)";
                        }
                        if (name.contains("application.properties")) {
                            return "properties (properties)";
                        }
                        return "properties";
                    }
                    if (name.contains("systemenvironment")) {
                        return "systemEnvironment";
                    }
                    if (name.contains("systemproperties")) {
                        return "systemProperties";
                    }
                    return name;
                }
            }
        }
        return "unknown";
    }
}