package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.model.CategoriaGasto;
import com.cdc.presupuesto.service.CategoriaGastoService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/categorias-gasto")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class CategoriaGastoController {
    private static final Logger logger = LoggerFactory.getLogger(CategoriaGastoController.class);

    @Autowired
    private CategoriaGastoService categoriaGastoService;

    @GetMapping
    public ResponseEntity<List<CategoriaGasto>> getAllCategorias(@AuthenticationPrincipal Jwt jwt) {
        List<CategoriaGasto> categorias = categoriaGastoService.getAllCategorias();
        return ResponseEntity.ok(categorias);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaGasto> getCategoriaById(@PathVariable String id,
                                                          @AuthenticationPrincipal Jwt jwt) {
        CategoriaGasto categoria = categoriaGastoService.getCategoriaById(id);
        return categoria != null ? ResponseEntity.ok(categoria) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<CategoriaGasto> createCategoria(@RequestBody CategoriaGasto categoria,
                                                         @AuthenticationPrincipal Jwt jwt) {
        if (categoria.getId() == null || categoria.getId().isEmpty()) {
            categoria.setId(UUID.randomUUID().toString());
        }
        CategoriaGasto created = categoriaGastoService.saveCategoria(categoria);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaGasto> updateCategoria(@PathVariable String id, 
                                                         @RequestBody CategoriaGasto categoria,
                                                         @AuthenticationPrincipal Jwt jwt) {
        categoria.setId(id);
        CategoriaGasto updated = categoriaGastoService.saveCategoria(categoria);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategoria(@PathVariable String id,
                                                @AuthenticationPrincipal Jwt jwt) {
        categoriaGastoService.deleteCategoria(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import-csv")
    public ResponseEntity<Map<String, Object>> importCategoriasFromCSV(
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
            Map<String, Object> result = categoriaGastoService.importCategoriasFromCSV(file, replaceAll);
            
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
