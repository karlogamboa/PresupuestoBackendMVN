package com.cdc.presupuesto.service;

import com.cdc.presupuesto.model.Solicitante;
import com.cdc.presupuesto.repository.SolicitanteRepository;
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

@Service
public class SolicitanteService {
    private static final Logger logger = LoggerFactory.getLogger(SolicitanteService.class);

    @Autowired
    private SolicitanteRepository solicitanteRepository;

    public List<Solicitante> getAllSolicitantes() {
        return solicitanteRepository.findAll();
    }

    public Solicitante getSolicitanteByNumEmpleado(String numEmpleado) {
        return solicitanteRepository.findByNumEmpleado(numEmpleado).orElse(null);
    }

    public Solicitante getSolicitanteByCorreoElectronico(String correoElectronico) {
        return solicitanteRepository.findAll().stream()
            .filter(s -> s.getCorreoElectronico() != null && s.getCorreoElectronico().equalsIgnoreCase(correoElectronico))
            .findFirst()
            .orElse(null);
    }

    public Solicitante saveSolicitante(Solicitante solicitante) {
        return solicitanteRepository.save(solicitante);
    }

    public void deleteSolicitante(String numEmpleado) {
        solicitanteRepository.deleteByNumEmpleado(numEmpleado);
    }

    public Map<String, Object> importSolicitantesFromCSV(MultipartFile file, boolean replaceAll) throws IOException, CsvException {
        
        List<Solicitante> solicitantes = new ArrayList<>();
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
                
                logger.debug("Processing row {}: {} columns", rowNumber, record.length);
                
                try {
                    // Expected CSV format based on the provided file (41 columns total):
                    // 0: Nombre, 1: Teléfono, 2: Correo electrónico, 3: Supervisor, 4: Acceso de inicio de sesión, 
                    // 5: Clase, 6: Subsidiaria, 7: Departamento, 8: Ubicación, 9: Representante de ventas, 
                    // 10: Representante de soporte, 11: Puesto de trabajo, 12: Cuenta, 13: Tipo de empleado, 
                    // ... 31: Aprobador de gastos, ... 40: ID interno
                    
                    if (record.length < 41) {
                        errors.add("Row " + rowNumber + ": Insufficient columns (expected 41, got " + record.length + ")");
                        errorCount++;
                        continue;
                    }
                    
                    Solicitante solicitante = new Solicitante();
                    
                    // Use ID interno as NumEmpleado (last column - index 40)
                    String idInternoStr = getColumnValue(record, 40);
                    logger.debug("Row {}: ID interno = '{}'", rowNumber, idInternoStr);
                    
                    if (idInternoStr.isEmpty()) {
                        errors.add("Row " + rowNumber + ": ID interno is required as NumEmpleado");
                        errorCount++;
                        continue;
                    }
                    
                    try {
                        int idInterno = Integer.parseInt(idInternoStr);
                        solicitante.setIdInterno(idInterno);
                        solicitante.setNumEmpleado(idInternoStr); // Use ID interno as NumEmpleado
                    } catch (NumberFormatException e) {
                        errors.add("Row " + rowNumber + ": Invalid ID interno format: " + idInternoStr);
                        errorCount++;
                        continue;
                    }
                    
                    // Map fields from CSV columns
                    solicitante.setNombre(getColumnValue(record, 0)); // Nombre
                    
                    // Extract subsidiaria from column 6 (format: "Empresa principal : Circulo de Crédito SA de CV")
                    String subsidiaria = extractFromColumn(getColumnValue(record, 6), false);
                    solicitante.setSubsidiaria(subsidiaria);
                    
                    // Extract departamento from column 7 (format: "SEGURIDAD DE LA INFORMACION : 336139-GERENCIA LEGAL")
                    String departamento = extractFromColumn(getColumnValue(record, 7), true);
                    solicitante.setDepartamento(departamento);
                    
                    solicitante.setPuestoTrabajo(getColumnValue(record, 11)); // Puesto de trabajo
                    
                    // Parse aprobadorGastos (column 31)
                    String aprobadorStr = getColumnValue(record, 31).toLowerCase();
                    solicitante.setAprobadorGastos(!aprobadorStr.isEmpty());
                    
                    // Validate required fields
                    if (solicitante.getNumEmpleado().isEmpty() || solicitante.getNombre().isEmpty()) {
                        errors.add("Row " + rowNumber + ": NumEmpleado and Nombre are required fields");
                        errorCount++;
                        continue;
                    }
                    
                    logger.debug("Row {}: Created solicitante with NumEmpleado={}, Nombre='{}'", 
                               rowNumber, solicitante.getNumEmpleado(), solicitante.getNombre());
                    
                    solicitantes.add(solicitante);
                    successCount++;
                    
                } catch (Exception e) {
                    errors.add("Row " + rowNumber + ": Error processing record - " + e.getMessage());
                    errorCount++;
                    logger.error("Error processing CSV row {}: {}", rowNumber, e.getMessage());
                }
            }
            
            // If replace all is true, delete all existing solicitantes first
            if (replaceAll && !solicitantes.isEmpty()) {
                logger.info("Deleting all existing solicitantes before import");
                solicitanteRepository.deleteAll();
            }
            
            // Save all valid solicitantes
            if (!solicitantes.isEmpty()) {
                logger.info("Saving {} solicitantes to database (batch)", solicitantes.size());
                solicitanteRepository.saveAllBatch(solicitantes);
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
     * Extract value from CSV column that may contain prefixes
     * Example: "SEGURIDAD DE LA INFORMACION : 336139-GERENCIA LEGAL" -> "SEGURIDAD DE LA INFORMACION"
     */
    private String extractFromColumn(String columnValue, boolean usePrefix) {
        if (columnValue == null || columnValue.trim().isEmpty()) {
            return "";
        }
        
        String trimmed = columnValue.trim();
        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":", 2);
            return usePrefix ? parts[0].trim() : parts[1].trim();
        }
        
        return trimmed;
    }
    
    /**
     * Safe method to get column value with bounds checking
     */
    private String getColumnValue(String[] record, int index) {
        if (record.length > index) {
            return record[index] != null ? record[index].trim() : "";
        }
        return "";
    }
}
