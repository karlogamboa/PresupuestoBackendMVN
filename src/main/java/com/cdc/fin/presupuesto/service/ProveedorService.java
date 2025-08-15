package com.cdc.fin.presupuesto.service;

import com.cdc.fin.presupuesto.model.Proveedor;
import com.cdc.fin.presupuesto.repository.ProveedorRepository;
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
    @Autowired(required = false)
    private software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable<Proveedor> proveedorTable;

    @Autowired(required = false)
    public void setProveedorTable(@org.springframework.beans.factory.annotation.Value("${aws.dynamodb.table.proveedores:fin-dynamodb-${ENVIRONMENT:qa}-presupuesto-proveedores}") String proveedorTableName) {
        if (dynamoDbEnhancedClient != null) {
            // El nombre debe ser exactamente el de la tabla DynamoDB
            this.proveedorTable = dynamoDbEnhancedClient.table(proveedorTableName, software.amazon.awssdk.enhanced.dynamodb.TableSchema.fromBean(Proveedor.class));
        }
    }

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

    /**
     * Bulk insert proveedores using DynamoDB batchWrite.
     * Falls back to saveAll if DynamoDB Enhanced Client is not configured.
     */
    private void batchInsertProveedores(List<Proveedor> proveedores) {
        logger.info("Intentando guardar {} proveedores en DynamoDB...", proveedores.size());
        if (dynamoDbEnhancedClient == null) {
            logger.warn("DynamoDbEnhancedClient no está configurado. Usando saveAll en repositorio.");
            proveedorRepository.saveAll(proveedores);
            return;
        }
        if (proveedorTable == null) {
            logger.warn("proveedorTable no está configurada. Usando saveAll en repositorio.");
            proveedorRepository.saveAll(proveedores);
            return;
        }
        int batchSize = 25; // DynamoDB batch write limit
        for (int i = 0; i < proveedores.size(); i += batchSize) {
            List<Proveedor> batch = proveedores.subList(i, Math.min(i + batchSize, proveedores.size()));
            software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch.Builder<Proveedor> writeBatchBuilder =
                software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch.builder(Proveedor.class)
                    .mappedTableResource(proveedorTable);
            for (Proveedor proveedor : batch) {
                writeBatchBuilder.addPutItem(proveedor);
            }
            software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest.Builder batchWriteBuilder =
                software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest.builder();
            batchWriteBuilder.addWriteBatch(writeBatchBuilder.build());
            try {
                dynamoDbEnhancedClient.batchWriteItem(batchWriteBuilder.build());
                logger.info("Batch insert exitoso para {} proveedores.", batch.size());
            } catch (Exception e) {
                logger.error("Error al guardar batch en DynamoDB: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Import proveedores from CSV file.
     * @param file CSV file
     * @param replaceAll If true, deletes all existing proveedores before import
     * @return Map with import summary and errors
     */
    public Map<String, Object> importProveedoresFromCSV(MultipartFile file, boolean replaceAll) throws IOException, CsvException {
        logger.info("Iniciando importación de proveedores desde CSV...");
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        List<Proveedor> proveedoresExitosos = new ArrayList<>();

        try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                .withSkipLines(1)
                .build()) {
            String[] record;
            int rowNumber = 2;
            while ((record = csvReader.readNext()) != null) {
                if (record.length < 11) {
                    errors.add("Row " + rowNumber + ": Insufficient columns (expected 11, got " + record.length + ")");
                    errorCount++;
                    rowNumber++;
                    continue;
                }
                Proveedor proveedor = new Proveedor();
                proveedor.setId(getColumnValue(record, 0));
                proveedor.setNombre(getColumnValue(record, 1));
                proveedor.setDuplicado(getColumnValue(record, 2));
                proveedor.setCategoria(getColumnValue(record, 3));
                proveedor.setSubsidiariaPrincipal(getColumnValue(record, 4));
                proveedor.setContactoPrincipal(getColumnValue(record, 5));
                proveedor.setTelefono(getColumnValue(record, 6));
                proveedor.setCorreoElectronico(getColumnValue(record, 7));
                proveedor.setAccesoInicioSesion(getColumnValue(record, 8));
                proveedor.setNumeroProveedor(getColumnValue(record, 9));
                proveedor.setCuentasGastos(getColumnValue(record, 10));

                if (proveedor.getId().isEmpty()) {
                    errors.add("Línea " + rowNumber + ": El campo 'ID' es requerido (vacío) - OMITIDO");
                    errorCount++;
                    rowNumber++;
                    continue;
                }
                if (proveedor.getNombre().isEmpty()) {
                    errors.add("Línea " + rowNumber + ": El campo 'Nombre' es requerido (vacío) - ID: '" + proveedor.getId() + "' - OMITIDO");
                    errorCount++;
                    rowNumber++;
                    continue;
                }
                String idLower = proveedor.getId().toLowerCase();
                if (idLower.contains("contador") || idLower.contains("tax agency") || idLower.equals("1") || idLower.equals("default")) {
                    errors.add("Línea " + rowNumber + ": Parece ser dato de sistema/referencia - ID: '" + proveedor.getId() + "' - OMITIDO");
                    errorCount++;
                    rowNumber++;
                    continue;
                }
                successCount++;
                proveedoresExitosos.add(proveedor);
                rowNumber++;
            }

            if (replaceAll) {
                logger.info("Eliminando todos los proveedores existentes antes de importar.");
                proveedorRepository.deleteAll();
            }
        } catch (IOException e) {
            logger.error("Error leyendo el archivo CSV: {}", e.getMessage(), e);
            throw new IOException("Error reading CSV file: " + e.getMessage(), e);
        } catch (CsvException e) {
            logger.error("Error parseando el archivo CSV: {}", e.getMessage(), e);
            throw new CsvException("Error parsing CSV file: " + e.getMessage());
        }

        // Guarda solo los exitosos
        if (!proveedoresExitosos.isEmpty()) {
            logger.info("Guardando {} proveedores importados...", proveedoresExitosos.size());
            batchInsertProveedores(proveedoresExitosos);
        } else {
            logger.warn("No hay proveedores válidos para guardar.");
        }

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

        logger.info("Importación de CSV de proveedores completada. Éxito: {}, Errores/Omitidos: {}", successCount, errorCount);

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

    /**
     * Paginación de proveedores sin filtro.
     */
    public Map<String, Object> getProveedoresPaginated(int page, int size) {
        List<Proveedor> all = proveedorRepository.findAll();
        int total = all.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<Proveedor> paged = all.subList(fromIndex, toIndex);
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("proveedores", paged);
        return result;
    }

    /**
     * Paginación de proveedores filtrando por nombre (mínimo 3 letras).
     */
    public Map<String, Object> getProveedoresPaginatedByNombre(String nombre, int page, int size) {
        String nombreLower = nombre.toLowerCase();
        List<Proveedor> filtered = proveedorRepository.findAll().stream()
            .filter(p -> p.getNombre() != null && p.getNombre().toLowerCase().contains(nombreLower))
            .toList();
        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<Proveedor> paged = filtered.subList(fromIndex, toIndex);
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("proveedores", paged);
        return result;
    }
}
