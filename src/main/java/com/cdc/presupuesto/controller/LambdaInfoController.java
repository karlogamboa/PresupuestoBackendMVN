package com.cdc.presupuesto.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LambdaInfoController {

    @Value("${lambda.function.url:}")
    private String lambdaFunctionUrl;

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @GetMapping("/lambda-info")
    public ResponseEntity<Map<String, Object>> getLambdaInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("status", "running");
        info.put("environment", "lambda + SAML2");
        info.put("functionUrl", lambdaFunctionUrl.isEmpty() ? "Not configured" : lambdaFunctionUrl);
        
        // Available endpoints
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("health", "/health");
        endpoints.put("solicitudes", "/api/solicitudes-presupuesto");
        endpoints.put("departamentos", "/api/departamentos");
        endpoints.put("proveedores", "/api/proveedores");
        endpoints.put("categorias", "/api/categorias-gasto");
        info.put("endpoints", endpoints);
        
        // Handler/runtime info
        info.put("handler", "com.amazonaws.serverless.proxy.spring.SpringBootProxyHandler::handleRequest");
        info.put("authMethod", "SAML2");
        info.put("runtime", "java21");
        Map<String, Object> runtime = new HashMap<>();
        runtime.put("javaVersion", System.getProperty("java.version"));
        runtime.put("memoryMax", Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");
        runtime.put("activeProfiles", System.getProperty("spring.profiles.active", "default"));
        info.put("runtimeInfo", runtime);


        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", allowedOrigins)
                .header("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Amz-Date,X-Api-Key,X-Amz-Security-Token,x-user-roles,x-user-email")
                .header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS")
                .body(info);
    }

}




