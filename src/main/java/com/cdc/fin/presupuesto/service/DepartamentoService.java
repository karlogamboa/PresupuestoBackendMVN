package com.cdc.fin.presupuesto.service;

import com.cdc.fin.presupuesto.model.Departamento;
import com.cdc.fin.presupuesto.repository.DepartamentoRepository;
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

    // Bulk insert departamentos using DynamoDB batchWrite
    @Autowired(required = false)
    private software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable<Departamento> departamentoTable;

    @Autowired(required = false)
    public void setDepartamentoTable(@org.springframework.beans.factory.annotation.Value("${aws.dynamodb.table.departamentos:fin-dynamodb-${ENVIRONMENT:qa}-presupuesto-departamentos}") String departamentoTableName) {
        if (dynamoDbEnhancedClient != null) {
            // El nombre debe ser exactamente el de la tabla DynamoDB
            this.departamentoTable = dynamoDbEnhancedClient.table(departamentoTableName, software.amazon.awssdk.enhanced.dynamodb.TableSchema.fromBean(Departamento.class));
        }
    }

    private void batchInsertDepartamentos(List<Departamento> departamentos) {
        if (dynamoDbEnhancedClient == null || departamentoTable == null) {
            departamentoRepository.saveAll(departamentos);
            return;
        }
        int batchSize = 25;
        for (int i = 0; i < departamentos.size(); i += batchSize) {
            List<Departamento> batch = departamentos.subList(i, Math.min(i + batchSize, departamentos.size()));
            software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch.Builder<Departamento> writeBatchBuilder =
                software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch.builder(Departamento.class)
                    .mappedTableResource(departamentoTable);
            for (Departamento departamento : batch) {
                writeBatchBuilder.addPutItem(departamento);
            }
            software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest.Builder batchWriteBuilder =
                software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest.builder();
            batchWriteBuilder.addWriteBatch(writeBatchBuilder.build());
            dynamoDbEnhancedClient.batchWriteItem(batchWriteBuilder.build());
        }
    }

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
                    if (record.length != 3) {
                        errors.add("Línea " + rowNumber + ": Columnas insuficientes (se esperaban 3, se obtuvieron " + record.length + ")");
                        errorCount++;
                        continue;
                    }
                    Departamento departamento = new Departamento();
                    String id = UUID.randomUUID().toString();
                    departamento.setId(id);
                    departamento.setNombreDepartamento(record[0].trim());
                    departamento.setSubDepartamento(record[1].trim());
                    departamento.setCeco(record[2].trim());
                    // Validar campos requeridos
                    if (departamento.getNombreDepartamento().isEmpty()) {
                        errors.add("Línea " + rowNumber + ": El campo 'Nombre Departamento' es requerido");
                        errorCount++;
                        continue;
                    }
                    departamentos.add(departamento);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Línea " + rowNumber + ": Error procesando el registro - " + e.getMessage());
                    errorCount++;
                    logger.error("Error procesando la línea CSV {}: {}", rowNumber, e.getMessage());
                }
            }
            
            // If replace all is true, delete all existing departamentos first
            if (replaceAll && !departamentos.isEmpty()) {
                departamentoRepository.deleteAll();
            }
            
        } catch (IOException e) {
            logger.error("Error reading CSV file: {}", e.getMessage());
            throw new IOException("Error reading CSV file: " + e.getMessage(), e);
        } catch (CsvException e) {
            logger.error("Error parsing CSV file: {}", e.getMessage());
            throw new CsvException("Error parsing CSV file: " + e.getMessage());
        }

        // Inserta en lote solo si no hay errores
        if (errorCount == 0 && !departamentos.isEmpty()) {
            batchInsertDepartamentos(departamentos);
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
        return result;
    }
}
