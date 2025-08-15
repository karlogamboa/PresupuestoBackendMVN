package com.cdc.fin.presupuesto.service;

import com.cdc.fin.presupuesto.model.Presupuesto;
import com.cdc.fin.presupuesto.model.Departamento;
import com.cdc.fin.presupuesto.repository.PresupuestoRepository;
import com.cdc.fin.presupuesto.repository.DepartamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PresupuestoService {

    private static final Logger logger = LoggerFactory.getLogger(PresupuestoService.class);

    @Autowired
    private PresupuestoRepository presupuestoRepository;

    @Autowired
    private DepartamentoRepository departamentoRepository;

    public String getPresupuesto(String ceCo, String cuentaGastos) {

        Optional<Presupuesto> presupuestoOpt = presupuestoRepository
            .findByCecoAndCuentaGastos(ceCo, cuentaGastos);

        if (presupuestoOpt.isPresent()) {
            return presupuestoOpt.get().getPresupuesto();
        } else {
            Optional<Departamento> deptoOpt = Optional.ofNullable(departamentoRepository.findByCeco(ceCo));
            if (deptoOpt.isPresent()) {
                return deptoOpt.get().getPresupuestoDefault();
            }
        }
        return null;
    }

    public Map<String, Object> importPresupuestosFromCSV(MultipartFile file, boolean replaceAll) throws Exception {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        List<Presupuesto> presupuestos = new ArrayList<>();

        try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)).build()) {
            logger.info("Iniciando importación de presupuestos desde CSV...");
            String[] header = csvReader.readNext();
            if (header == null) throw new Exception("El archivo CSV está vacío");

            // Busca los índices de las columnas necesarias
            int idxCeCo = -1, idxCuentaGastos = -1, idxPresupuesto = -1;
            for (int i = 0; i < header.length; i++) {
                String col = header[i].trim().replace(" ", "");
                if (col.equalsIgnoreCase("CeCo")) idxCeCo = i;
                if (col.equalsIgnoreCase("CuentaGastos")) idxCuentaGastos = i;
                if (col.equalsIgnoreCase("Presupuesto")) idxPresupuesto = i;
            }
            if (idxCeCo == -1 || idxCuentaGastos == -1 || idxPresupuesto == -1) {
                throw new Exception("El archivo CSV debe contener las columnas: CeCo, CuentaGastos, Presupuesto");
            }

            String[] record;
            int rowNumber = 2;
            while ((record = csvReader.readNext()) != null) {
                try {
                    Presupuesto presupuesto = new Presupuesto();
                    // Asigna el campo clave si falta (ejemplo: id)
                    if (presupuesto.getId() == null || presupuesto.getId().isEmpty()) {
                        presupuesto.setId(java.util.UUID.randomUUID().toString());
                    }
                    presupuesto.setCeco(record[idxCeCo].trim());
                    presupuesto.setCuentaGastos(record[idxCuentaGastos].trim());
                    presupuesto.setPresupuesto(record[idxPresupuesto].trim());
                    presupuestos.add(presupuesto);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Línea " + rowNumber + ": " + e.getMessage());
                    errorCount++;
                }
                rowNumber++;
            }
            if (replaceAll && !presupuestos.isEmpty()) {
                logger.info("Eliminando todos los presupuestos existentes antes de importar.");
                presupuestoRepository.deleteAll();
            }
        } catch (Exception e) {
            logger.error("Error leyendo el archivo CSV: {}", e.getMessage(), e);
            errors.add("Error leyendo el archivo CSV: " + e.getMessage());
            throw new RuntimeException("Error leyendo el archivo CSV: " + e.getMessage(), e);
        }

        // Guarda los presupuestos importados
        if (!presupuestos.isEmpty()) {
            logger.info("Guardando {} presupuestos importados...", presupuestos.size());
            try {
                presupuestoRepository.saveAll(presupuestos);
            } catch (Exception e) {
                logger.error("Error en saveAll presupuestos: {}", e.getMessage(), e);
                errors.add("Error al guardar presupuestos en DynamoDB: " + e.getMessage());
                throw new RuntimeException("Error al guardar presupuestos en DynamoDB: " + e.getMessage(), e);
            }
        } else {
            logger.warn("No hay presupuestos válidos para guardar.");
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
        logger.info("Importación de CSV de presupuestos completada. Éxito: {}, Errores/Omitidos: {}", successCount, errorCount);
        return result;
    }
}