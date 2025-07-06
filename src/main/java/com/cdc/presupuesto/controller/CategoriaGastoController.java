package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.model.CategoriaGasto;
import com.cdc.presupuesto.service.CategoriaGastoService;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/categorias-gasto")
public class CategoriaGastoController {
    private static final Logger logger = LoggerFactory.getLogger(CategoriaGastoController.class);

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Autowired
    private CategoriaGastoService categoriaGastoService;

    @GetMapping
    public ResponseEntity<List<CategoriaGasto>> getAllCategorias(@AuthenticationPrincipal Jwt jwt) {
        List<CategoriaGasto> categorias = categoriaGastoService.getAllCategorias();
        return ResponseEntity.ok(categorias);
    }

    @PostMapping("/import-csv")
    public ResponseEntity<Map<String, Object>> importCategoriasFromCSV(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "replaceAll", defaultValue = "true") boolean replaceAll,
            @AuthenticationPrincipal Jwt jwt) {

        logger.info("CSV import request received from user: {}", jwt.getSubject());

        ResponseEntity<Map<String, Object>> response;
        try {
            // Validate file
            if (file.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "File is empty");
                response = ResponseEntity.badRequest().body(error);
            } else {
                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "File must be a CSV file");
                    response = ResponseEntity.badRequest().body(error);
                } else {
                    // Process CSV
                    Map<String, Object> result = categoriaGastoService.importCategoriasFromCSV(file, replaceAll);
                    if (Boolean.TRUE.equals(result.get("success"))) {
                        response = ResponseEntity.ok(result);
                    } else {
                        response = ResponseEntity.badRequest().body(result);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("IO error during CSV import: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error reading file: " + e.getMessage());
            response = ResponseEntity.badRequest().body(error);
        } catch (CsvException e) {
            logger.error("CSV parsing error: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error parsing CSV: " + e.getMessage());
            response = ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            logger.error("Unexpected error during CSV import: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Unexpected error: " + e.getMessage());
            response = ResponseEntity.internalServerError().body(error);
        }

        // Agrega el header CORS dinámicamente
        String[] origins = allowedOrigins.split(",");
        String originHeader = origins.length > 0 ? origins[0].trim() : "*";
        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .header("Access-Control-Allow-Origin", originHeader)
                .header("Access-Control-Allow-Credentials", "true")
                .body(response.getBody());
    }
}
        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .header("Access-Control-Allow-Origin", originHeader)
                .header("Access-Control-Allow-Credentials", "true")
                .body(response.getBody());
    }
}
