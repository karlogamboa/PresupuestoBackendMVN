package com.cdc.fin.presupuesto.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cdc.fin.presupuesto.model.SolicitudPresupuesto;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

@Service
public class EmailService {

    private final SesClient sesClient;

    @Value("${email.charset}")
    private String charsetUtf8;

    @Value("${email.from}")
    private String defaultFrom;

    @Value("${email.cc}")
    private String defaultCc;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    public EmailService(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    public String sendSimpleEmail(String from, String to, String subject, String body) throws SesException {
        if (from == null || from.isEmpty()) {
            from = defaultFrom;
        }
        final String toAddress;
        if (to == null || to.isEmpty()) {
            toAddress = defaultFrom;
        } else {
            toAddress = to;
        }
        logger.info("Enviando correo simple: from={}, to={}, subject={}", from, toAddress, subject);
        logger.debug("Parámetros SES: charsetUtf8={}", charsetUtf8);
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                .source(from)
                .destination(d -> {
                    d.toAddresses(toAddress);
                    if (defaultCc != null && !defaultCc.isEmpty()) {
                        d.ccAddresses(defaultCc);
                    }
                })
                .message(m -> m
                    .subject(s -> s.data(subject).charset(charsetUtf8))
                    .body(b -> b.text(tb -> tb.data(body).charset(charsetUtf8)))
                )
                .build();
            logger.debug("Request SES: {}", request);
            SendEmailResponse response = sesClient.sendEmail(request);
            logger.info("Correo enviado correctamente. messageId={}", response.messageId());
            logger.debug("SES Response: {}", response);
            return response.messageId();
        } catch (SesException e) {
            logger.error("SES Exception al enviar correo: {}", e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Excepción inesperada al enviar correo: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String sendHtmlEmail(String from, String to, String subject, String htmlBody, String textBody) throws SesException {
        if (from == null || from.isEmpty()) {
            from = defaultFrom;
        }
        logger.info("Enviando correo HTML: from={}, to={}, subject={}", from, to, subject);
        logger.debug("Parámetros SES: charsetUtf8={}", charsetUtf8);
        try {
            SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                .source(from)
                .destination(d -> {
                    d.toAddresses(to);
                    if (defaultCc != null && !defaultCc.isEmpty()) {
                        d.ccAddresses(defaultCc);
                    }
                })
                .message(m -> m
                    .subject(s -> s.data(subject).charset(charsetUtf8))
                    .body(b -> {
                        b.html(h -> h.data(htmlBody).charset(charsetUtf8));
                        b.text(t -> t.data(textBody != null ? textBody : "").charset(charsetUtf8));
                    })
                )
                .build();
            logger.debug("Request SES: {}", sendEmailRequest);
            SendEmailResponse response = sesClient.sendEmail(sendEmailRequest);
            logger.info("Correo HTML enviado correctamente. messageId={}", response.messageId());
            logger.debug("SES Response: {}", response);
            return response.messageId();
        } catch (SesException e) {
            logger.error("SES Exception al enviar correo HTML: {}", e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Excepción inesperada al enviar correo HTML: {}", e.getMessage(), e);
            throw e;
        }
    }

    // --- Centralización de formatos de correos ---

    // 1. Pendiente ➡ Aprobado
    public static String cuerpoSolicitudAprobada(SolicitudPresupuesto s, String fechaCambio, String nombreAprobador, String puestoAprobador) {
        String nombreSolicitante = s.getSolicitante() != null ? s.getSolicitante() : "";
        String nombreDepartamento = s.getDepartamento() != null ? s.getDepartamento() : "";
        String numeroDepartamento = s.getCentroCostos() != null ? s.getCentroCostos() : "";
        String nombreCuenta = s.getCategoriaGasto() != null ? s.getCategoriaGasto() : "";
        String numeroCuenta = s.getCuentaGastos() != null ? s.getCuentaGastos() : "";
        String periodo = s.getPeriodoPresupuesto() != null ? s.getPeriodoPresupuesto() : "";
        String fechaSolicitud = s.getFecha() != null ? s.getFecha() : "";
        return "Hola " + nombreSolicitante + ",\n" +
                "Tu solicitud de presupuesto ha sido aprobada el " + fechaCambio + ".\n" +
                "Detalles de la solicitud:\n" +
                "\t• Departamento: " + nombreDepartamento + " (" + numeroDepartamento + ")\n" +
                "\t• Cuenta de gasto: " + nombreCuenta + " (" + numeroCuenta + ")\n" +
                "\t• Periodo: " + periodo + "\n" +
                "\t• Fecha de solicitud: " + fechaSolicitud + "\n" +
                "Ahora pasará a la etapa de procesamiento.\n" +
                "Atentamente,\n" + nombreAprobador + "\n" + puestoAprobador;
    }

    // 2. Pendiente ➡ Rechazado
    public static String cuerpoSolicitudRechazada(SolicitudPresupuesto s, String fechaCambio, String motivoRechazo, String nombreAprobador, String puestoAprobador) {
        String nombreSolicitante = s.getSolicitante() != null ? s.getSolicitante() : "";
        String nombreDepartamento = s.getDepartamento() != null ? s.getDepartamento() : "";
        String numeroDepartamento = s.getCentroCostos() != null ? s.getCentroCostos() : "";
        String nombreCuenta = s.getCategoriaGasto() != null ? s.getCategoriaGasto() : "";
        String numeroCuenta = s.getCuentaGastos() != null ? s.getCuentaGastos() : "";
        String periodo = s.getPeriodoPresupuesto() != null ? s.getPeriodoPresupuesto() : "";
        String fechaSolicitud = s.getFecha() != null ? s.getFecha() : "";
        return "Hola " + nombreSolicitante + ",\n" +
                "Tu solicitud de presupuesto ha sido rechazada el " + fechaCambio + ".\n" +
                "Detalles de la solicitud:\n" +
                "\t• Departamento: " + nombreDepartamento + " (" + numeroDepartamento + ")\n" +
                "\t• Cuenta de gasto: " + nombreCuenta + " (" + numeroCuenta + ")\n" +
                "\t• Periodo: " + periodo + "\n" +
                "\t• Fecha de solicitud: " + fechaSolicitud + "\n" +
                "\t• Motivo de rechazo:\n" +
                "\t◦ La solicitud no cumple con los criterios establecidos para aprobación.\n" +
                (motivoRechazo == null || motivoRechazo.isEmpty() ? "" : ("\t◦ " + motivoRechazo + "\n")) +
                "Por favor revisa la información y considera realizar una nueva solicitud si corresponde.\n" +
                "Atentamente,\n" + nombreAprobador + "\n" + puestoAprobador;
    }

    // 3. Aprobado ➡ Procesado
    public static String cuerpoSolicitudProcesada(SolicitudPresupuesto s, String fechaCambio, String nombreProcesador, String puestoProcesador) {
        String nombreSolicitante = s.getSolicitante() != null ? s.getSolicitante() : "";
        String nombreDepartamento = s.getDepartamento() != null ? s.getDepartamento() : "";
        String numeroDepartamento = s.getCentroCostos() != null ? s.getCentroCostos() : "";
        String nombreCuenta = s.getCategoriaGasto() != null ? s.getCategoriaGasto() : "";
        String numeroCuenta = s.getCuentaGastos() != null ? s.getCuentaGastos() : "";
        String periodo = s.getPeriodoPresupuesto() != null ? s.getPeriodoPresupuesto() : "";
        String fechaSolicitud = s.getFecha() != null ? s.getFecha() : "";
        return "Hola " + nombreSolicitante + ",\n" +
                "Tu solicitud de presupuesto ha sido procesada el " + fechaCambio + ".\n" +
                "Detalles de la solicitud:\n" +
                "\t• Departamento: " + nombreDepartamento + " (" + numeroDepartamento + ")\n" +
                "\t• Cuenta de gasto: " + nombreCuenta + " (" + numeroCuenta + ")\n" +
                "\t• Periodo: " + periodo + "\n" +
                "\t• Fecha de solicitud: " + fechaSolicitud + "\n" +
                "Ya puedes ingresar tu orden de compra en NetSuite.\n" +
                "Gracias por utilizar el sistema de presupuestos.\n" +
                "Atentamente,\n" + nombreProcesador + "\n" + puestoProcesador;
    }

    // 4. Pendiente ➡ Procesado (sin pasar por aprobación)
    public static String cuerpoSolicitudProcesadaDirecta(SolicitudPresupuesto s, String fechaCambio, String nombreProcesador, String puestoProcesador) {
        String nombreSolicitante = s.getSolicitante() != null ? s.getSolicitante() : "";
        String nombreDepartamento = s.getDepartamento() != null ? s.getDepartamento() : "";
        String numeroDepartamento = s.getCentroCostos() != null ? s.getCentroCostos() : "";
        String nombreCuenta = s.getCategoriaGasto() != null ? s.getCategoriaGasto() : "";
        String numeroCuenta = s.getCuentaGastos() != null ? s.getCuentaGastos() : "";
        String periodo = s.getPeriodoPresupuesto() != null ? s.getPeriodoPresupuesto() : "";
        String fechaSolicitud = s.getFecha() != null ? s.getFecha() : "";
        return "Hola " + nombreSolicitante + ",\n" +
                "Tu solicitud de presupuesto ha sido procesada directamente el " + fechaCambio + ".\n" +
                "Detalles de la solicitud:\n" +
                "\t• Departamento: " + nombreDepartamento + " (" + numeroDepartamento + ")\n" +
                "\t• Cuenta de gasto: " + nombreCuenta + " (" + numeroCuenta + ")\n" +
                "\t• Periodo: " + periodo + "\n" +
                "\t• Fecha de solicitud: " + fechaSolicitud + "\n" +
                "Ya puedes ingresar tu orden de compra en NetSuite.\n" +
                "Si tienes dudas, contacta al área de presupuestos.\n" +
                "Atentamente,\n" + nombreProcesador + "\n" + puestoProcesador;
    }

    // 5A. Nueva solicitud recibida (para el aprobador)
    public static String cuerpoNuevaSolicitudAprobador(SolicitudPresupuesto s, String fechaCambio) {
        String nombreSolicitante = s.getSolicitante() != null ? s.getSolicitante() : "";
        String nombreDepartamento = s.getDepartamento() != null ? s.getDepartamento() : "";
        String numeroDepartamento = s.getCentroCostos() != null ? s.getCentroCostos() : "";
        String nombreCuenta = s.getCategoriaGasto() != null ? s.getCategoriaGasto() : "";
        String numeroCuenta = s.getCuentaGastos() != null ? s.getCuentaGastos() : "";
        String periodo = s.getPeriodoPresupuesto() != null ? s.getPeriodoPresupuesto() : "";
        String fechaSolicitud = s.getFecha() != null ? s.getFecha() : "";
        return "Hola " + ",\n" +
                "Se ha recibido una nueva solicitud de presupuesto el " + fechaCambio + " que requiere tu revisión:\n" +
                "\t• Solicitante: " + nombreSolicitante + "\n" +
                "\t• Departamento: " + nombreDepartamento + " (" + numeroDepartamento + ")\n" +
                "\t• Cuenta de gasto: " + nombreCuenta + " (" + numeroCuenta + ")\n" +
                "\t• Periodo: " + periodo + "\n" +
                "\t• Fecha de solicitud: " + fechaSolicitud + "\n" +
                "Por favor ingresa al sistema para aprobar o rechazar esta solicitud.\n" +
                "Atentamente,\nSistema de Presupuestos";
    }

    // 5B. Confirmación de recepción de solicitud (para el solicitante)
    public static String cuerpoNuevaSolicitudSolicitante(SolicitudPresupuesto s, String fechaCambio) {
        String nombreSolicitante = s.getSolicitante() != null ? s.getSolicitante() : "";
        String nombreDepartamento = s.getDepartamento() != null ? s.getDepartamento() : "";
        String numeroDepartamento = s.getCentroCostos() != null ? s.getCentroCostos() : "";
        String nombreCuenta = s.getCategoriaGasto() != null ? s.getCategoriaGasto() : "";
        String numeroCuenta = s.getCuentaGastos() != null ? s.getCuentaGastos() : "";
        String periodo = s.getPeriodoPresupuesto() != null ? s.getPeriodoPresupuesto() : "";
        String fechaSolicitud = s.getFecha() != null ? s.getFecha() : "";
        return "Hola " + nombreSolicitante + ",\n" +
                "Tu solicitud de presupuesto ha sido registrada el " + fechaCambio + " y está pendiente de revisión.\n" +
                "Detalles de la solicitud:\n" +
                "\t• Departamento: " + nombreDepartamento + " (" + numeroDepartamento + ")\n" +
                "\t• Cuenta de gasto: " + nombreCuenta + " (" + numeroCuenta + ")\n" +
                "\t• Periodo: " + periodo + "\n" +
                "\t• Fecha de solicitud: " + fechaSolicitud + "\n" +
                "Recibirás una notificación cuando tu solicitud sea aprobada o rechazada.\n" +
                "Atentamente,\nSistema de Presupuestos";
    }

    // Métodos de alto nivel para envío de correos de negocio

    public void sendSolicitudProcesadaEmail(String to, SolicitudPresupuesto s, String nombreProcesador, String puestoProcesador) {
        String asunto = "Solicitud de presupuesto procesada";
        String cuerpo = cuerpoSolicitudProcesada(s, fechaCambioActual(), nombreProcesador, puestoProcesador);
        sendSafe(null, to, asunto, cuerpo);
    }

    public void sendSolicitudProcesadaDirectaEmail(String to, SolicitudPresupuesto s, String nombreProcesador, String puestoProcesador) {
        String asunto = "Solicitud de presupuesto procesada directamente";
        String cuerpo = cuerpoSolicitudProcesadaDirecta(s, fechaCambioActual(), nombreProcesador, puestoProcesador);
        sendSafe(null, to, asunto, cuerpo);
    }

    public void sendSolicitudAprobadaEmail(String to, SolicitudPresupuesto s, String nombreAprobador, String puestoAprobador) {
        String asunto = "Solicitud de presupuesto aprobada";
        String cuerpo = cuerpoSolicitudAprobada(s, fechaCambioActual(), nombreAprobador, puestoAprobador);
        sendSafe(null, to, asunto, cuerpo);
    }

    public void sendSolicitudRechazadaEmail(String to, SolicitudPresupuesto s, String motivoRechazo, String nombreAprobador, String puestoAprobador) {
        String asunto = "Solicitud de presupuesto rechazada";
        String cuerpo = cuerpoSolicitudRechazada(s, fechaCambioActual(), motivoRechazo, nombreAprobador, puestoAprobador);
        sendSafe(null, to, asunto, cuerpo);
    }

    public void sendNuevaSolicitudAprobadorEmail(SolicitudPresupuesto s) {
        String asunto = "Nueva solicitud de presupuesto recibida";
        String cuerpo = cuerpoNuevaSolicitudAprobador(s,  fechaCambioActual());
        sendSafe(null, null, asunto, cuerpo);
    }

    public void sendNuevaSolicitudSolicitanteEmail(String to, SolicitudPresupuesto s) {
        String asunto = "Solicitud de presupuesto recibida";
        String cuerpo = cuerpoNuevaSolicitudSolicitante(s, fechaCambioActual());
        sendSafe(null, to, asunto, cuerpo);
    }

    // Utilidad interna para evitar excepción no controlada en los métodos de negocio
    private void sendSafe(String from, String to, String subject, String body) {
        try {
            sendSimpleEmail(from, to, subject, body);
        } catch (Exception e) {
            logger.error("Error enviando correo a {}: {}", to, e.getMessage(), e);
        }
    }

    // Utilidad para obtener fecha/hora actual formateada
    public static String fechaCambioActual() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dtf.format(LocalDateTime.now());
    }

    /**
     * Envía la notificación de cambio de estatus de solicitud de presupuesto según transición.
     * 
     * @param estatusAnterior El estatus anterior (ej: "Pendiente")
     * @param nuevoEstatus El nuevo estatus (ej: "Aprobado", "Rechazado", "Procesado")
     * @param correoSolicitante Correo del destinatario
     * @param solicitud La solicitud de presupuesto
     * @param motivoRechazo Motivo de rechazo (si aplica)
     * @param nombreAprobador Nombre del aprobador/procesador
     * @param puestoAprobador Puesto del aprobador/procesador
     */
    public void sendNotificacionCambioEstatus(
            String estatusAnterior,
            String nuevoEstatus,
            String correoSolicitante,
            SolicitudPresupuesto solicitud,
            String motivoRechazo,
            String nombreAprobador,
            String puestoAprobador
    ) {
        if (correoSolicitante == null || correoSolicitante.isEmpty()) {
            logger.warn("No se envió correo de cambio de estatus porque el correo es nulo o vacío");
            return;
        }
        String fechaCambio = fechaCambioActual();
        if ("Pendiente".equalsIgnoreCase(estatusAnterior) && "Aprobado".equalsIgnoreCase(nuevoEstatus)) {
            sendSolicitudAprobadaEmail(correoSolicitante, solicitud, nombreAprobador, puestoAprobador);
        } else if ("Pendiente".equalsIgnoreCase(estatusAnterior) && "Rechazado".equalsIgnoreCase(nuevoEstatus)) {
            sendSolicitudRechazadaEmail(correoSolicitante, solicitud, motivoRechazo, nombreAprobador, puestoAprobador);
        } else if ("Aprobado".equalsIgnoreCase(estatusAnterior) && "Procesado".equalsIgnoreCase(nuevoEstatus)) {
            sendSolicitudProcesadaEmail(correoSolicitante, solicitud, nombreAprobador, puestoAprobador);
        } else if ("Pendiente".equalsIgnoreCase(estatusAnterior) && "Procesado".equalsIgnoreCase(nuevoEstatus)) {
            sendSolicitudProcesadaDirectaEmail(correoSolicitante, solicitud, nombreAprobador, puestoAprobador);
        } else {
            logger.info("No se envía correo para transición de estatus: {} -> {}", estatusAnterior, nuevoEstatus);
        }
    }

}