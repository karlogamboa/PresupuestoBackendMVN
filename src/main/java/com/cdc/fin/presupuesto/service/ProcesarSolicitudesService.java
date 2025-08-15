package com.cdc.fin.presupuesto.service;

// Dependencia: implementation 'org.apache.poi:poi-ooxml:5.2.3' (o similar)
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.cdc.fin.presupuesto.model.SolicitudPresupuesto;
import com.cdc.fin.presupuesto.repository.SolicitudPresupuestoRepository;
import com.cdc.fin.presupuesto.service.EmailService;
import com.cdc.fin.presupuesto.repository.PresupuestoRepository;
import com.cdc.fin.presupuesto.repository.DepartamentoRepository;
import com.cdc.fin.presupuesto.model.Presupuesto;
import com.cdc.fin.presupuesto.model.Departamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProcesarSolicitudesService {
    private static final Logger logger = LoggerFactory.getLogger(ProcesarSolicitudesService.class);

    private final SolicitudPresupuestoRepository solicitudPresupuestoRepository;
    private final EmailService emailService;
    private final UserInfoService userInfoService;
    private final PresupuestoRepository presupuestoRepository;
    private final DepartamentoRepository departamentoRepository;

    public ProcesarSolicitudesService(
        SolicitudPresupuestoRepository solicitudPresupuestoRepository,
        EmailService emailService,
        UserInfoService userInfoService,
        PresupuestoRepository presupuestoRepository,
        DepartamentoRepository departamentoRepository
    ) {
        this.solicitudPresupuestoRepository = solicitudPresupuestoRepository;
        this.emailService = emailService;
        this.userInfoService = userInfoService;
        this.presupuestoRepository = presupuestoRepository;
        this.departamentoRepository = departamentoRepository;
    }

    // Cambia la firma para devolver el Excel como byte[]
    // NO aceptes OutputStream como argumento, solo sin argumentos
    public byte[] procesarYExportarExcel() throws IOException {
        logger.info("Iniciando procesamiento y exportación de solicitudes aprobadas...");
        // Buscar todas las solicitudes con estatus "Aprobado"
        List<SolicitudPresupuesto> solicitudes = solicitudPresupuestoRepository.findAll().stream()
                .filter(s -> "Aprobado".equalsIgnoreCase(s.getEstatusConfirmacion()))
                .toList();
        logger.info("Solicitudes encontradas con estatus 'Aprobado': {}", solicitudes.size());
        String nombreProcesador = userInfoService.getNombreProcesadorActual();
        String puestoProcesador = userInfoService.getPuestoProcesadorActual();
        for (SolicitudPresupuesto s : solicitudes) {
            logger.debug("Procesando solicitud ID: {}, Solicitante: {}", s.getId(), s.getSolicitante());
            s.setEstatusConfirmacion("Procesado");
            s.setFechaActualizacion(java.time.Instant.now());
            solicitudPresupuestoRepository.save(s);
            logger.debug("Enviando correo de solicitud procesada a: {}", s.getCorreo());
            emailService.sendSolicitudProcesadaEmail(
                s.getCorreo(),
                s,
                nombreProcesador,
                puestoProcesador
            );
        }
        logger.info("Exportando a Excel...");
        byte[] excelBytes = exportarExcel(solicitudes);
        logger.info("Exportación a Excel finalizada.");
        // DEBUG: Tamaño y primeros bytes
        logger.debug("Excel generado, tamaño: {} bytes", excelBytes.length);
        if (excelBytes.length > 16) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append(String.format("%02X ", excelBytes[i]));
            }
            logger.debug("Primeros 16 bytes del archivo: {}", sb.toString());
        }
        return excelBytes;
    }

    // Cambia para devolver byte[]
    private byte[] exportarExcel(List<SolicitudPresupuesto> solicitudes) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Solicitudes Procesadas");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Presupuesto");
            header.createCell(1).setCellValue("Cuenta de Gastos");
            header.createCell(2).setCellValue("Departamento");
            header.createCell(3).setCellValue("Periodo");
            header.createCell(4).setCellValue("Monto");
            // ...puedes agregar más columnas si lo deseas...

            int rowIdx = 1;
            for (SolicitudPresupuesto s : solicitudes) {
                Row row = sheet.createRow(rowIdx++);
                // Normaliza CeCo para evitar errores de formato
                String ceco = s.getCentroCostos() != null ? s.getCentroCostos().trim() : "";
                logger.debug("Solicitud ID: {}, CentroCostos original: '{}'", s.getId(), s.getCentroCostos());
                // Si tiene guion, toma solo los números antes del guion
                if (ceco.contains("-")) {
                    String[] parts = ceco.split("-", 2);
                    if (parts.length > 0 && parts[0].trim().matches("\\d+")) {
                        logger.debug("CentroCostos '{}' contiene guion, usando '{}'", ceco, parts[0].trim());
                        ceco = parts[0].trim();
                    }
                }
                String cuentaGastos = s.getCuentaGastos() != null ? s.getCuentaGastos().trim() : "";
                logger.debug("Procesando fila: CeCo='{}', CuentaGastos='{}'", ceco, cuentaGastos);

                // Buscar presupuesto en la tabla de presupuestos (solo CeCo y CuentaGastos)
                String presupuesto = null;
                Presupuesto presupuestoObj = null;
                try {
                    presupuestoObj = presupuestoRepository.findByCecoAndCuentaGastos(ceco, cuentaGastos)
                        .orElse(null);
                    logger.debug("Resultado búsqueda presupuestoObj: {}", presupuestoObj);
                } catch (Exception ex) {
                    logger.warn("Error buscando presupuesto para CeCo {} y CuentaGastos {}: {}", ceco, cuentaGastos, ex.getMessage());
                }

                if (presupuestoObj != null && presupuestoObj.getPresupuesto() != null) {
                    presupuesto = presupuestoObj.getPresupuesto();
                    logger.debug("Presupuesto encontrado: {}", presupuesto);
                } else {
                    // Si no existe, buscar PresupuestoDefault en la tabla de departamentos
                    Departamento depto = null;
                    try {
                        depto = departamentoRepository.findByCeco(ceco);
                        logger.debug("Resultado búsqueda departamento: {}", depto);
                    } catch (Exception ex) {
                        logger.warn("Error buscando departamento para CeCo {}: {}", ceco, ex.getMessage());
                    }
                    if (depto != null) {
                        presupuesto = depto.getPresupuestoDefault();
                        if (presupuesto != null) {
                            logger.debug("Presupuesto default encontrado: {}", presupuesto);
                        } else {
                            logger.debug("Departamento encontrado para CeCo '{}', pero PresupuestoDefault es nulo o vacío", ceco);
                        }
                    } else {
                        logger.debug("No se encontró presupuesto ni departamento para CeCo '{}'", ceco);
                    }
                }

                row.createCell(0).setCellValue(presupuesto != null ? presupuesto : "");
                row.createCell(1).setCellValue(cuentaGastos);
                row.createCell(2).setCellValue(ceco);
                row.createCell(3).setCellValue(s.getPeriodoPresupuesto() != null ? s.getPeriodoPresupuesto() : "");
                // Monto
                row.createCell(4).setCellValue(s.getMontoSubtotal());
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                workbook.write(baos);
                return baos.toByteArray();
            }
        }
    }
}