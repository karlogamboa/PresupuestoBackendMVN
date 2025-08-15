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
        logger.info("Intentando guardar {} departamentos en DynamoDB...", departamentos.size());
        if (dynamoDbEnhancedClient == null) {
            logger.warn("DynamoDbEnhancedClient no está configurado. Usando saveAll en repositorio.");
            departamentoRepository.saveAll(departamentos);
            return;
        }
        if (departamentoTable == null) {
            logger.warn("departamentoTable no está configurada. Usando saveAll en repositorio.");
            departamentoRepository.saveAll(departamentos);
            return;
        }
        int batchSize = 25;
        for (int i = 0; i < departamentos.size(); i += batchSize) {
            List<Departamento> batch = departamentos.subList(i, Math.min(i + batchSize, departamentos.size()));
            // Asigna el campo clave si falta (ejemplo: id)
            for (Departamento departamento : batch) {
                if (departamento.getId() == null || departamento.getId().isEmpty()) {
                    departamento.setId(UUID.randomUUID().toString());
                }
                // Si tu tabla usa otra clave, asígnala aquí
            }
            software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch.Builder<Departamento> writeBatchBuilder =
                software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch.builder(Departamento.class)
                    .mappedTableResource(departamentoTable);
            for (Departamento departamento : batch) {
                writeBatchBuilder.addPutItem(departamento);
            }
            software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest.Builder batchWriteBuilder =
                software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest.builder();
            batchWriteBuilder.addWriteBatch(writeBatchBuilder.build());
            try {
                dynamoDbEnhancedClient.batchWriteItem(batchWriteBuilder.build());
                logger.info("Batch insert exitoso para {} departamentos.", batch.size());
            } catch (Exception e) {
                logger.error("Error al guardar batch en DynamoDB: {}", e.getMessage(), e);
                // Lanza excepción para que el frontend reciba el error
                throw new RuntimeException("Error al guardar departamentos en DynamoDB: " + e.getMessage(), e);
            }
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

    public Map<String, Object> importDepartamentosFromCSV(MultipartFile file, boolean replaceAll) throws Exception {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        List<Departamento> departamentos = new ArrayList<>();

        try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)).build()) {
            logger.info("Iniciando importación de departamentos desde CSV...");
            String[] header = csvReader.readNext();
            if (header == null) throw new Exception("El archivo CSV está vacío");

            // Permite variantes de nombres de columna
            int idxNombre = -1, idxSub = -1, idxCeCo = -1, idxPresupuesto = -1;
            for (int i = 0; i < header.length; i++) {
                String col = header[i].trim().replace(" ", "").replace("-", "").toLowerCase();
                if (col.equals("departamento") || col.equals("nombredepartamento")) idxNombre = i;
                if (col.equals("nombre")) idxSub = i;
                if (col.equals("ceco")) idxCeCo = i;
                if (col.equals("presupuestodefault")) idxPresupuesto = i;
            }
            // Si no se detecta, intenta variantes más flexibles
            if (idxNombre == -1) {
                for (int i = 0; i < header.length; i++) {
                    String col = header[i].trim().replace(" ", "").replace("-", "").toLowerCase();
                    if (col.contains("departamento")) { idxNombre = i; break; }
                }
            }
            if (idxSub == -1) {
                for (int i = 0; i < header.length; i++) {
                    String col = header[i].trim().replace(" ", "").replace("-", "").toLowerCase();
                    if (col.contains("nombre")) { idxSub = i; break; }
                }
            }
            if (idxCeCo == -1) {
                for (int i = 0; i < header.length; i++) {
                    String col = header[i].trim().replace(" ", "").replace("-", "").toLowerCase();
                    if (col.contains("ceco")) { idxCeCo = i; break; }
                }
            }
            if (idxPresupuesto == -1) {
                for (int i = 0; i < header.length; i++) {
                    String col = header[i].trim().replace("-", "").toLowerCase();
                    // Permite "Presupuesto Default" con espacio
                    if (col.replace(" ", "").contains("presupuestodefault") || col.contains("presupuesto default")) {
                        idxPresupuesto = i; break;
                    }
                    // Permite cualquier columna que contenga "presupuesto"
                    if (col.contains("presupuesto")) { idxPresupuesto = i; break; }
                }
            }
            if (idxNombre == -1 || idxSub == -1 || idxCeCo == -1 || idxPresupuesto == -1) {
                throw new Exception("El archivo CSV debe contener las columnas: Departamento, Sub-Departamento, CeCo, Presupuesto Default");
            }

            String[] record;
            int rowNumber = 2;
            while ((record = csvReader.readNext()) != null) {
                try {
                    Departamento departamento = new Departamento();
                    departamento.setNombreDepartamento(record[idxNombre].trim());
                    departamento.setSubDepartamento(record[idxSub].trim());
                    departamento.setCeco(record[idxCeCo].trim());
                    departamento.setPresupuestoDefault(record[idxPresupuesto].trim());
                    if (departamento.getNombreDepartamento().isEmpty()) {
                        errors.add("Línea " + rowNumber + ": El campo 'Departamento' es requerido");
                        errorCount++;
                    } else {
                        departamentos.add(departamento);
                        successCount++;
                    }
                } catch (Exception e) {
                    errors.add("Línea " + rowNumber + ": " + e.getMessage());
                    errorCount++;
                }
                rowNumber++;
            }
            if (replaceAll) {
                logger.info("Eliminando todos los departamentos existentes antes de importar.");
                departamentoRepository.deleteAll();
            }
        } catch (Exception e) {
            logger.error("Error leyendo el archivo CSV: {}", e.getMessage(), e);
            errors.add("Error leyendo el archivo CSV: " + e.getMessage());
            // Lanza excepción para que el frontend reciba el error
            throw new RuntimeException("Error leyendo el archivo CSV: " + e.getMessage(), e);
        }

        // Guarda los departamentos importados
        if (!departamentos.isEmpty()) {
            logger.info("Guardando {} departamentos importados...", departamentos.size());
            try {
                batchInsertDepartamentos(departamentos);
            } catch (Exception e) {
                logger.error("Error en batchInsertDepartamentos: {}", e.getMessage(), e);
                errors.add("Error al guardar departamentos en DynamoDB: " + e.getMessage());
                // Lanza excepción para que el frontend reciba el error
                throw new RuntimeException("Error al guardar departamentos en DynamoDB: " + e.getMessage(), e);
            }
        } else {
            logger.warn("No hay departamentos válidos para guardar.");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", errorCount == 0);
        result.put("totalRecords", successCount + errorCount);
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        result.put("errors", errors);
        result.put("message", errorCount > 0
            ? String.format("Importación completada con advertencias: %d exitosos, %d errores/omitidos.", successCount, errorCount)
            : String.format("Importación completada exitosamente: %d registros importados.", successCount));
        logger.info("Importación de CSV de departamentos completada. Éxito: {}, Errores/Omitidos: {}", successCount, errorCount);
        return result;
    }
}