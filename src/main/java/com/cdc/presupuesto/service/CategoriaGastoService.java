package com.cdc.presupuesto.service;

import com.cdc.presupuesto.model.CategoriaGasto;
import com.cdc.presupuesto.repository.CategoriaGastoRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Service
public class CategoriaGastoService {
    private static final Logger logger = LoggerFactory.getLogger(CategoriaGastoService.class);

    @Autowired
    private CategoriaGastoRepository categoriaGastoRepository;

    public List<CategoriaGasto> getAllCategorias() {
        return categoriaGastoRepository.findAll();
    }

    public CategoriaGasto getCategoriaById(String id) {
        return categoriaGastoRepository.findById(id).orElse(null);
    }

    public CategoriaGasto saveCategoria(CategoriaGasto categoria) {
        return categoriaGastoRepository.save(categoria);
    }

    public void deleteCategoria(String id) {
        categoriaGastoRepository.deleteById(id);
    }

    public Map<String, Object> importCategoriasFromCSV(MultipartFile file, boolean replaceAll) throws IOException, CsvException {
        logger.info("Starting CSV import for categorias gasto. Replace all: {}", replaceAll);
        
        List<CategoriaGasto> categorias = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                .withSkipLines(1) // Skip header
                .build()) {
            
            List<String[]> records = csvReader.readAll();
            
            for (int i = 0; i < records.size(); i++) {
                String[] record = records.get(i);
                int rowNumber = i + 2; // +2 because we skip header and arrays are 0-indexed
                
                try {
                    if (record.length < 5) {
                        errors.add("Row " + rowNumber + ": Insufficient columns (expected at least 5, got " + record.length + ")");
                        errorCount++;
                        continue;
                    }
                    
                    // Validar que el nombre no esté vacío primero
                    String nombre = record[0].trim();
                    if (nombre.isEmpty()) {
                        errors.add("Row " + rowNumber + ": Nombre is required");
                        errorCount++;
                        continue;
                    }
                    
                    CategoriaGasto categoria = new CategoriaGasto();
                    
                    // Generar ID automáticamente a partir del nombre
                    String id = generateCategoriaId(nombre);
                    categoria.setId(id);
                    
                    // Mapear correctamente las columnas del CSV:
                    // Columna 0: Nombre
                    // Columna 1: Descripción  
                    // Columna 2: Cuenta de gastos
                    // Columna 3: Cuenta
                    // Columna 4: Saldo
                    categoria.setNombre(nombre);
                    categoria.setDescripcion(record[1].trim());
                    categoria.setCuentaDeGastos(record[2].trim());
                    categoria.setCuenta(record[3].trim());
                    
                    // Parse saldo - manejar formatos como "$3,307,877.56" o "$-"
                    try {
                        String saldoStr = record[4].trim();
                        Double saldoValue = parseSaldoValue(saldoStr);
                        categoria.setSaldo(saldoValue);
                    } catch (Exception e) {
                        logger.warn("Row {}: Could not parse saldo '{}', setting to 0.0", rowNumber, record[4]);
                        categoria.setSaldo(0.0);
                    }
                    
                    // Validate required fields
                    if (categoria.getNombre().isEmpty()) {
                        errors.add("Row " + rowNumber + ": Nombre is required");
                        errorCount++;
                        continue;
                    }
                    
                    logger.debug("Created categoria: ID={}, Nombre={}, Saldo={}", 
                            categoria.getId(), categoria.getNombre(), categoria.getSaldo());
                    
                    categorias.add(categoria);
                    successCount++;
                    
                } catch (Exception e) {
                    errors.add("Row " + rowNumber + ": Error processing record - " + e.getMessage());
                    errorCount++;
                    logger.error("Error processing CSV row {}: {}", rowNumber, e.getMessage());
                }
            }
            
            // If replace all is true, delete all existing categorias first
            if (replaceAll && !categorias.isEmpty()) {
                logger.info("Deleting all existing categorias gasto before import");
                categoriaGastoRepository.deleteAll();
            }
            
            // Save all valid categorias
            if (!categorias.isEmpty()) {
                logger.info("Saving {} categorias gasto to database", categorias.size());
                categoriaGastoRepository.saveAll(categorias);
            }
            
        } catch (IOException e) {
            logger.error("Error reading CSV file: {}", e.getMessage());
            throw new IOException("Error reading CSV file: " + e.getMessage(), e);
        } catch (CsvException e) {
            logger.error("Error parsing CSV file: {}", e.getMessage());
            throw new CsvException("Error parsing CSV file: " + e.getMessage());
        }
        
        // Prepare response
        Map<String, Object> result = new HashMap<>();
        result.put("success", errorCount == 0);
        result.put("totalRecords", successCount + errorCount);
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        result.put("errors", errors);
        result.put("message", String.format("Import completed: %d successful, %d errors", successCount, errorCount));
        
        logger.info("CSV import completed. Success: {}, Errors: {}", successCount, errorCount);
        
        return result;
    }
    
    /**
     * Generate a unique ID for categoria based on name
     * Format: CAT_{NORMALIZED_NAME}
     */
    private String generateCategoriaId(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "CAT_" + UUID.randomUUID().toString();
        }
        
        // Normalize the name: remove accents, convert to uppercase, replace spaces and special characters
        String normalized = nombre.trim()
            .toUpperCase()
            .replaceAll("[ÁÀÂÃÄÅ]", "A")
            .replaceAll("[ÉÈÊË]", "E")
            .replaceAll("[ÍÌÎÏ]", "I")
            .replaceAll("[ÓÒÔÕÖ]", "O")
            .replaceAll("[ÚÙÛÜ]", "U")
            .replaceAll("[Ñ]", "N")
            .replaceAll("[Ç]", "C")
            .replaceAll("[^A-Z0-9]", "_")  // Replace non-alphanumeric with underscore
            .replaceAll("_+", "_")         // Replace multiple underscores with single
            .replaceAll("^_|_$", "");      // Remove leading/trailing underscores
        
        // Limit length to avoid very long IDs
        if (normalized.length() > 50) {
            normalized = normalized.substring(0, 50);
        }
        
        return "CAT_" + normalized;
    }
    
    /**
     * Parse saldo value from CSV string
     * Handles formats like "$3,307,877.56", "$-", "-", etc.
     */
    private Double parseSaldoValue(String saldoStr) {
        if (saldoStr == null || saldoStr.trim().isEmpty() || 
            saldoStr.trim().equals("$-") || saldoStr.trim().equals("-")) {
            return 0.0;
        }
        
        try {
            // Clean the saldo string: remove $, commas, and spaces
            String cleanSaldo = saldoStr.trim()
                    .replace("$", "")
                    .replace(",", "")
                    .replace(" ", "");
            
            // Handle negative values
            boolean isNegative = cleanSaldo.startsWith("-");
            if (isNegative) {
                cleanSaldo = cleanSaldo.substring(1);
            }
            
            // Handle empty string after cleaning
            if (cleanSaldo.isEmpty()) {
                return 0.0;
            }
            
            Double value = Double.parseDouble(cleanSaldo);
            return isNegative ? -value : value;
            
        } catch (NumberFormatException e) {
            logger.warn("Could not parse saldo value: '{}', defaulting to 0.0", saldoStr);
            return 0.0;
        }
    }
}
