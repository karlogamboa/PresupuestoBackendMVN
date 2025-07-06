package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.model.Solicitante;
import com.cdc.presupuesto.service.SolicitanteService;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/solicitantes")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class SolicitanteController {
    private static final Logger logger = LoggerFactory.getLogger(SolicitanteController.class);

    @Autowired
    private SolicitanteService solicitanteService;

    @GetMapping
    public ResponseEntity<List<Solicitante>> getAllSolicitantes(@AuthenticationPrincipal Jwt jwt) {
        List<Solicitante> solicitantes = solicitanteService.getAllSolicitantes();
        return ResponseEntity.ok(solicitantes);
    }

    @GetMapping("/{numEmpleado}")
    public ResponseEntity<Solicitante> getSolicitanteByNumEmpleado(@PathVariable String numEmpleado,
                                                                   @AuthenticationPrincipal Jwt jwt) {
        Solicitante solicitante = solicitanteService.getSolicitanteByNumEmpleado(numEmpleado);
        return solicitante != null ? ResponseEntity.ok(solicitante) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Solicitante> createSolicitante(@RequestBody Solicitante solicitante,
                                                        @AuthenticationPrincipal Jwt jwt) {
        Solicitante created = solicitanteService.saveSolicitante(solicitante);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{numEmpleado}")
    public ResponseEntity<Solicitante> updateSolicitante(@PathVariable String numEmpleado, 
                                                        @RequestBody Solicitante solicitante,
                                                        @AuthenticationPrincipal Jwt jwt) {
        solicitante.setNumEmpleado(numEmpleado);
        Solicitante updated = solicitanteService.saveSolicitante(solicitante);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{numEmpleado}")
    public ResponseEntity<Void> deleteSolicitante(@PathVariable String numEmpleado,
                                                 @AuthenticationPrincipal Jwt jwt) {
        solicitanteService.deleteSolicitante(numEmpleado);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import-csv")
    public ResponseEntity<Map<String, Object>> importSolicitantesFromCSV(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "replaceAll", defaultValue = "true") boolean replaceAll,
            @AuthenticationPrincipal Jwt jwt) {
        
        logger.info("CSV import request received from user: {}", jwt.getSubject());
        
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
            Map<String, Object> result = solicitanteService.importSolicitantesFromCSV(file, replaceAll);
            
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
