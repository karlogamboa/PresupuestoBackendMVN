package com.cdc.presupuesto.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class EmailService {

    private static final String CHARSET_UTF8 = "UTF-8";

    private final SesClient sesClient;

    @Autowired
    public EmailService(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    public void sendBudgetRequestNotification(String from, String to, String requestId, String requesterName, Double amount) throws SesException {
        String subject = "Nueva Solicitud de Presupuesto - " + requestId;
        String body = String.format(
            "Estimado/a,\n\nSe ha recibido una nueva solicitud de presupuesto:\n\nID de Solicitud: %s\nSolicitante: %s\nMonto: $%.2f\n\nPor favor, revise la solicitud en el sistema.\n\nSaludos cordiales,\nSistema de Presupuestos CDC",
            requestId, requesterName, amount
        );
        sendSimpleEmail(from, to, subject, body);
    }

    public void sendBudgetStatusNotification(String from, String to, String requestId, String status, String comments) throws SesException {
        String subject = "Actualización de Solicitud de Presupuesto - " + requestId;
        String statusText = switch (status.toUpperCase()) {
            case "APROBADO" -> "APROBADA";
            case "RECHAZADO" -> "RECHAZADA";
            default -> "ACTUALIZADA";
        };
        String body = String.format(
            "Estimado/a,\n\nSu solicitud de presupuesto ha sido %s:\n\nID de Solicitud: %s\nEstado: %s\n%s\n\nSaludos cordiales,\nPresupuestos",
            statusText, requestId, status,
            comments != null && !comments.isEmpty() ? "Comentarios: " + comments : ""
        );
        sendSimpleEmail(from, to, subject, body);
    }

    public void sendSimpleEmail(String from, String to, String subject, String body) throws SesException {
        SendEmailRequest request = SendEmailRequest.builder()
                .source(from)
                .destination(Destination.builder().toAddresses(to).build())
                .message(Message.builder()
                        .subject(Content.builder().data(subject).charset(CHARSET_UTF8).build())
                        .body(Body.builder().text(Content.builder().data(body).charset(CHARSET_UTF8).build()).build())
                        .build())
                .build();
        sesClient.sendEmail(request);
    }
}
