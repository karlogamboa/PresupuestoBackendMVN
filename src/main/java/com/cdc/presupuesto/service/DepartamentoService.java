package com.cdc.presupuesto.service;

import com.cdc.presupuesto.model.Departamento;
import com.cdc.presupuesto.repository.DepartamentoRepository;
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
public class DepartamentoService {
    private static final Logger logger = LoggerFactory.getLogger(DepartamentoService.class);

    @Autowired
    private DepartamentoRepository departamentoRepository;

    public List<Departamento> getAllDepartamentos() {
        return departamentoRepository.findAll();
    }

    public Departamento getDepartamentoById(String id) {
        return departamentoRepository.findById(id).orElse(null);
    }

    public Departamento saveDepartamento(Departamento departamento) {
        return departamentoRepository.save(departamento);
    }

    public void deleteDepartamento(String id) {
        departamentoRepository.deleteById(id);
    }

    public Map<String, Object> importDepartamentosFromCSV(MultipartFile file, boolean replaceAll) throws IOException, CsvException {
        logger.info("Starting CSV import for departamentos. Replace all: {}", replaceAll);
        
        List<Departamento> departamentos = new ArrayList<>();
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
                    if (record.length < 4) {
                        errors.add("Row " + rowNumber + ": Insufficient columns (expected 4, got " + record.length + ")");
                        errorCount++;
                        continue;
                    }
                    
                    Departamento departamento = new Departamento();
                    String id = UUID.randomUUID().toString();                    
                    departamento.setId(id);
                    departamento.setNombreDepartamento(record[1].trim());
                    departamento.setSubDepartamento(record[2].trim());
                    departamento.setCeco(record[3].trim());
                    
                    // Validate required fields
                    if (departamento.getNombreDepartamento().isEmpty()) {
                        errors.add("Row " + rowNumber + ": Nombre Departamento is required");
                        errorCount++;
                        continue;
                    }
                    
                    departamentos.add(departamento);
                    successCount++;
                    
                } catch (Exception e) {
                    errors.add("Row " + rowNumber + ": Error processing record - " + e.getMessage());
                    errorCount++;
                    logger.error("Error processing CSV row {}: {}", rowNumber, e.getMessage());
                }
            }
            
            // If replace all is true, delete all existing departamentos first
            if (replaceAll && !departamentos.isEmpty()) {
                logger.info("Deleting all existing departamentos before import");
                departamentoRepository.deleteAll();
            }
            
            // Save all valid departamentos
            if (!departamentos.isEmpty()) {
                logger.info("Saving {} departamentos to database", departamentos.size());
                departamentoRepository.saveAll(departamentos);
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
