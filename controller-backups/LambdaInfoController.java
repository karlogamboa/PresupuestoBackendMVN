package com.cdc.presupuesto.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Lambda Info", description = "Information about Lambda deployment and API documentation")
public class LambdaInfoController {

    @Value("${lambda.function.url:}")
    private String lambdaFunctionUrl;

    @GetMapping("/lambda-info")
    @Operation(summary = "Get Lambda deployment information", 
               description = "Returns information about the Lambda deployment and available endpoints")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lambda information retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getLambdaInfo() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("status", "running");
        info.put("environment", "lambda");
        info.put("functionUrl", lambdaFunctionUrl.isEmpty() ? "Not configured" : lambdaFunctionUrl);
        
        // Available endpoints
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("health", "/health");
        endpoints.put("userInfo", "/api/userInfo");
        endpoints.put("solicitudes", "/api/solicitudes-presupuesto");
        endpoints.put("usuarios", "/api/usuarios");
        endpoints.put("areas", "/api/areas");
        endpoints.put("departamentos", "/api/departamentos");
        endpoints.put("subdepartamentos", "/api/subdepartamentos");
        endpoints.put("proveedores", "/api/proveedores");
        endpoints.put("categorias", "/api/categorias-gasto");
        endpoints.put("swagger-ui", "/swagger-ui.html");
        endpoints.put("api-docs", "/v3/api-docs");
        
        info.put("endpoints", endpoints);
        
        // Documentation links
        Map<String, String> documentation = new HashMap<>();
        if (!lambdaFunctionUrl.isEmpty()) {
            documentation.put("swagger-ui", lambdaFunctionUrl + "/swagger-ui.html");
            documentation.put("openapi-json", lambdaFunctionUrl + "/v3/api-docs");
            documentation.put("health-check", lambdaFunctionUrl + "/health");
        } else {
            documentation.put("swagger-ui", "Configure LAMBDA_FUNCTION_URL environment variable");
            documentation.put("openapi-json", "Configure LAMBDA_FUNCTION_URL environment variable");
            documentation.put("health-check", "/health");
        }
        
        info.put("documentation", documentation);
        
        return ResponseEntity.ok(info);
    }

    @GetMapping("/swagger-redirect")
    @Operation(summary = "Redirect to Swagger UI", 
               description = "Redirects to the Swagger UI documentation")
    public ResponseEntity<Map<String, String>> getSwaggerRedirect() {
        Map<String, String> response = new HashMap<>();
        
        if (!lambdaFunctionUrl.isEmpty()) {
            response.put("swaggerUrl", lambdaFunctionUrl + "/swagger-ui.html");
            response.put("message", "Access Swagger UI at the provided URL");
        } else {
            response.put("swaggerUrl", "/swagger-ui.html");
            response.put("message", "Access Swagger UI at /swagger-ui.html");
        }
        
        return ResponseEntity.ok(response);
    }
}
