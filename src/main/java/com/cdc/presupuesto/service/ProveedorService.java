package com.cdc.presupuesto.service;

import com.cdc.presupuesto.model.Proveedor;
import com.cdc.presupuesto.repository.ProveedorRepository;
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
public class ProveedorService {
    private static final Logger logger = LoggerFactory.getLogger(ProveedorService.class);

    @Autowired
    private ProveedorRepository proveedorRepository;

    public List<Proveedor> getAllProveedores() {
        return proveedorRepository.findAll();
    }

    public Proveedor getProveedorById(String id) {
        return proveedorRepository.findById(id).orElse(null);
    }

    public Proveedor saveProveedor(Proveedor proveedor) {
        return proveedorRepository.save(proveedor);
    }

    public void deleteProveedor(String id) {
        proveedorRepository.deleteById(id);
    }

    public Map<String, Object> importProveedoresFromCSV(MultipartFile file, boolean replaceAll) throws IOException, CsvException {
        List<Proveedor> proveedores = new ArrayList<>();
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
                    // Expected CSV format based on the provided file (11 columns total):
                    // 0: ID, 1: Nombre, 2: Duplicado, 3: Categoría, 4: Subsidiaria principal, 
                    // 5: Contacto principal, 6: Teléfono, 7: Correo electrónico, 8: Acceso de inicio de sesión, 
                    // 9: Número Proveedor, 10: Cuentas de gastos
                    
                    if (record.length < 11) {
                        errors.add("Row " + rowNumber + ": Insufficient columns (expected 11, got " + record.length + ")");
                        errorCount++;
                        continue;
                    }
                    
                    Proveedor proveedor = new Proveedor();
                    
                    // Map fields from CSV columns with safe value extraction
                    proveedor.setId(getColumnValue(record, 0)); // ID
                    proveedor.setNombre(getColumnValue(record, 1)); // Nombre
                    proveedor.setDuplicado(getColumnValue(record, 2)); // Duplicado
                    proveedor.setCategoria(getColumnValue(record, 3)); // Categoría
                    proveedor.setSubsidiariaPrincipal(getColumnValue(record, 4)); // Subsidiaria principal
                    proveedor.setContactoPrincipal(getColumnValue(record, 5)); // Contacto principal
                    proveedor.setTelefono(getColumnValue(record, 6)); // Teléfono
                    proveedor.setCorreoElectronico(getColumnValue(record, 7)); // Correo electrónico
                    proveedor.setAccesoInicioSesion(getColumnValue(record, 8)); // Acceso de inicio de sesión
                    proveedor.setNumeroProveedor(getColumnValue(record, 9)); // Número Proveedor
                    proveedor.setCuentasGastos(getColumnValue(record, 10)); // Cuentas de gastos
                    
                    // Validate required fields
                    if (proveedor.getId().isEmpty()) {
                        errors.add("Row " + rowNumber + ": ID is required (found empty) - SKIPPED");
                        errorCount++;
                        continue;
                    }
                    
                    if (proveedor.getNombre().isEmpty()) {
                        errors.add("Row " + rowNumber + ": Nombre is required (found empty) - ID: '" + proveedor.getId() + "' - SKIPPED");
                        errorCount++;
                        continue;
                    }
                    
                    // Skip rows that appear to be system/reference data (common patterns)
                    String idLower = proveedor.getId().toLowerCase();
                    if (idLower.contains("contador") || idLower.contains("tax agency") || 
                        idLower.equals("1") || idLower.equals("default")) {
                        errors.add("Row " + rowNumber + ": Appears to be system/reference data - ID: '" + proveedor.getId() + "' - SKIPPED");
                        errorCount++;
                        continue;
                    }
                    
                    logger.debug("Row {}: Created proveedor with ID={}, Nombre='{}'", 
                               rowNumber, proveedor.getId(), proveedor.getNombre());
                    
                    proveedores.add(proveedor);
                    successCount++;
                    
                } catch (Exception e) {
                    errors.add("Row " + rowNumber + ": Error processing record - " + e.getMessage());
                    errorCount++;
                    logger.error("Error processing CSV row {}: {}", rowNumber, e.getMessage());
                }
            }
            
            // If replace all is true, delete all existing proveedores first
            if (replaceAll && !proveedores.isEmpty()) {
                logger.info("Deleting all existing proveedores before import");
                proveedorRepository.deleteAll();
            }
            
            // Save all valid proveedores
            if (!proveedores.isEmpty()) {
                logger.info("Saving {} proveedores to database", proveedores.size());
                proveedorRepository.saveAll(proveedores);
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
        
        if (errorCount > 0) {
            result.put("message", String.format("Import completed with issues: %d successful, %d errors/skipped. Check errors for details.", successCount, errorCount));
        } else {
            result.put("message", String.format("Import completed successfully: %d records imported", successCount));
        }
        
        logger.info("CSV import completed. Success: {}, Errors/Skipped: {}", successCount, errorCount);
        
        return result;
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
