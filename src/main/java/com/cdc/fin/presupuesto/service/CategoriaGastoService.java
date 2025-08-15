package com.cdc.fin.presupuesto.service;

import com.cdc.fin.presupuesto.model.CategoriaGasto;
import com.cdc.fin.presupuesto.repository.CategoriaGastoRepository;
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
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

@Service
public class CategoriaGastoService {
    private static final Logger logger = LoggerFactory.getLogger(CategoriaGastoService.class);

    @Autowired
    private CategoriaGastoRepository categoriaGastoRepository;

    // Bulk insert categorias using DynamoDB batchWrite
    @Autowired(required = false)
    private software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable<CategoriaGasto> categoriaGastoTable;

    @Autowired(required = false)
    public void setCategoriaGastoTable(@org.springframework.beans.factory.annotation.Value("${aws.dynamodb.table.categoriasGasto:fin-dynamodb-${ENVIRONMENT:qa}-presupuesto-categorias-gasto}") String categoriaGastoTableName) {
        if (dynamoDbEnhancedClient != null) {
            // El nombre debe ser exactamente el de la tabla DynamoDB
            this.categoriaGastoTable = dynamoDbEnhancedClient.table(categoriaGastoTableName, software.amazon.awssdk.enhanced.dynamodb.TableSchema.fromBean(CategoriaGasto.class));
        }
    }

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

    /**
     * Inserción masiva de categorías usando DynamoDB batchWrite.
     * Si DynamoDB Enhanced Client no está configurado, usa saveAll.
     */
    private void batchInsertCategorias(List<CategoriaGasto> categorias) {
        logger.info("Intentando guardar {} categorías en DynamoDB...", categorias.size());
        if (dynamoDbEnhancedClient == null) {
            logger.warn("DynamoDbEnhancedClient no está configurado. Usando saveAll en repositorio.");
            categoriaGastoRepository.saveAll(categorias);
            return;
        }
        if (categoriaGastoTable == null) {
            logger.warn("categoriaGastoTable no está configurada. Usando saveAll en repositorio.");
            categoriaGastoRepository.saveAll(categorias);
            return;
        }
        int batchSize = 25;
        try {
            for (int i = 0; i < categorias.size(); i += batchSize) {
                List<CategoriaGasto> batch = categorias.subList(i, Math.min(i + batchSize, categorias.size()));
                software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch.Builder<CategoriaGasto> writeBatchBuilder =
                    software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch.builder(CategoriaGasto.class)
                        .mappedTableResource(categoriaGastoTable);
                for (CategoriaGasto categoria : batch) {
                    writeBatchBuilder.addPutItem(categoria);
                }
                software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest.Builder batchWriteBuilder =
                    software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest.builder();
                batchWriteBuilder.addWriteBatch(writeBatchBuilder.build());
                try {
                    dynamoDbEnhancedClient.batchWriteItem(batchWriteBuilder.build());
                    logger.info("Batch insert exitoso para {} categorías.", batch.size());
                } catch (Exception e) {
                    logger.error("Error al guardar batch en DynamoDB: {}", e.getMessage(), e);
                }
            }
        } catch (software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException e) {
            logger.error("La tabla DynamoDB no existe o el nombre es incorrecto: {}", e.getMessage());
            throw new RuntimeException("Error: La tabla DynamoDB para categorías de gasto no existe o el nombre es incorrecto. Verifica la configuración.");
        }
    }

    public Map<String, Object> importCategoriasFromCSV(MultipartFile file, boolean replaceAll) throws IOException, CsvException {
        logger.info("Iniciando importación de categorías desde CSV...");
        List<CategoriaGasto> categorias = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        Set<String> idsVistos = new HashSet<>();
        Set<String> idsDuplicados = new HashSet<>();
        List<CategoriaGasto> categoriasSinDuplicados = new ArrayList<>();

        try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                .withSkipLines(1) // Skip header
                .build()) {
            
            List<String[]> records = csvReader.readAll();
            
            for (int i = 0; i < records.size(); i++) {
                String[] record = records.get(i);
                int rowNumber = i + 2; // +2 porque se omite el encabezado y arrays son 0-indexados
                
                try {
                    if (record.length < 5) {
                        errors.add("Línea " + rowNumber + ": Columnas insuficientes (se esperaban al menos 5, se obtuvieron " + record.length + ")");
                        errorCount++;
                        continue;
                    }
                    
                    // Validar que el nombre no esté vacío primero
                    String nombre = record[0].trim();
                    if (nombre.isEmpty()) {
                        errors.add("Línea " + rowNumber + ": El campo 'Nombre' es requerido");
                        errorCount++;
                        continue;
                    }
                    
                    CategoriaGasto categoria = new CategoriaGasto();
                    
                    // Generar ID automáticamente a partir del nombre
                    String id = generateCategoriaId(nombre);
                    if (!idsVistos.add(id)) {
                        idsDuplicados.add(id);
                        errorCount++;
                        errors.add("Línea " + rowNumber + ": ID duplicado '" + id + "' - OMITIDO");
                        continue;
                    }
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
                        logger.warn("Línea {}: No se pudo convertir el saldo '{}', se asigna 0.0", rowNumber, record[4]);
                        categoria.setSaldo(0.0);
                    }
                    
                    // Validate required fields
                    if (categoria.getNombre().isEmpty()) {
                        errors.add("Línea " + rowNumber + ": El campo 'Nombre' es requerido");
                        errorCount++;
                        continue;
                    }
                    
                    logger.debug("Categoría creada: ID={}, Nombre={}, Saldo={}", 
                            categoria.getId(), categoria.getNombre(), categoria.getSaldo());
                    
                    categoriasSinDuplicados.add(categoria);
                    successCount++;
                    
                } catch (Exception e) {
                    errors.add("Línea " + rowNumber + ": Error procesando el registro - " + e.getMessage());
                    errorCount++;
                    logger.error("Error procesando la línea CSV {}: {}", rowNumber, e.getMessage());
                }
            }
            
            // If replace all is true, delete all existing categorias first
            if (replaceAll) {
                logger.info("Eliminando todas las categorías existentes antes de importar.");
                categoriaGastoRepository.deleteAll();
            }
        } catch (IOException e) {
            logger.error("Error leyendo el archivo CSV: {}", e.getMessage(), e);
            throw new IOException("Error reading CSV file: " + e.getMessage(), e);
        } catch (CsvException e) {
            logger.error("Error parseando el archivo CSV: {}", e.getMessage(), e);
            throw new CsvException("Error parsing CSV file: " + e.getMessage());
        }

        // Inserta en lote solo si hay al menos un registro válido
        if (!categoriasSinDuplicados.isEmpty()) {
            logger.info("Guardando {} categorías importadas...", categoriasSinDuplicados.size());
            batchInsertCategorias(categoriasSinDuplicados);
        } else {
            logger.warn("No hay categorías válidas para guardar.");
        }
        if (!idsDuplicados.isEmpty()) {
            errors.add("Advertencia: Se omitieron " + idsDuplicados.size() + " categorías duplicadas por ID. IDs duplicados: " + String.join(", ", idsDuplicados));
        }

        // Prepare response
        Map<String, Object> result = new HashMap<>();
        result.put("success", errorCount == 0);
        result.put("totalRecords", successCount + errorCount);
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        result.put("errors", errors);
        if (errorCount > 0) {
            result.put("message", String.format("Importación completada con advertencias: %d exitosos, %d errores/omitidos. Consulta el detalle de errores.", successCount, errorCount));
        } else {
            result.put("message", String.format("Importación completada exitosamente: %d registros importados.", successCount));
        }
        
        logger.info("Importación de CSV de categorías completada. Éxito: {}, Errores/Omitidos: {}", successCount, errorCount);
        
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