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
                    if (record.length < 6) {
                        errors.add("Row " + rowNumber + ": Insufficient columns (expected 6, got " + record.length + ")");
                        errorCount++;
                        continue;
                    }
                    
                    CategoriaGasto categoria = new CategoriaGasto();
                    String id = record[0].trim();
                    if (id.isEmpty()) {
                        id = UUID.randomUUID().toString();
                    }
                    categoria.setId(id);
                    categoria.setNombre(record[1].trim());
                    categoria.setDescripcion(record[2].trim());
                    categoria.setCuentaDeGastos(record[3].trim());
                    categoria.setCuenta(record[4].trim());
                    
                    // Parse saldo
                    try {
                        String saldoStr = record[5].trim();
                        if (!saldoStr.isEmpty()) {
                            categoria.setSaldo(Double.parseDouble(saldoStr));
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Row " + rowNumber + ": Invalid saldo format: " + record[5]);
                        errorCount++;
                        continue;
                    }
                    
                    // Validate required fields
                    if (categoria.getNombre().isEmpty()) {
                        errors.add("Row " + rowNumber + ": Nombre is required");
                        errorCount++;
                        continue;
                    }
                    
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
}
