package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.util.UserAuthUtils;

import com.cdc.presupuesto.model.Proveedor;
import com.cdc.presupuesto.service.ProveedorService;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;


import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {
    private static final Logger logger = LoggerFactory.getLogger(ProveedorController.class);

    @Autowired
    private ProveedorService proveedorService;

    @GetMapping
    public ResponseEntity<List<Proveedor>> getAllProveedores() {
        List<Proveedor> proveedores = proveedorService.getAllProveedores();
        return ResponseEntity.ok(proveedores);
    }

    @PostMapping("/import-csv")
    public ResponseEntity<Map<String, Object>> importProveedoresFromCSV(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "replaceAll", defaultValue = "true") boolean replaceAll) {
        
        logger.info("CSV import request received from user: {}", UserAuthUtils.getCurrentUserId());
        
        try {
            // Validate file
            if (file.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "File is empty");
                return ResponseEntity.badRequest().body(error);
            }
            
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "File must be a CSV file");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Process CSV
            Map<String, Object> result = proveedorService.importProveedoresFromCSV(file, replaceAll);
            
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (IOException e) {
            logger.error("IO error during CSV import: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error reading file: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (CsvException e) {
            logger.error("CSV parsing error: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error parsing CSV: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            logger.error("Unexpected error during CSV import: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Unexpected error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}






