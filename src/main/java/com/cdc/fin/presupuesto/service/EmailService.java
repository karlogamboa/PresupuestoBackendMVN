package com.cdc.fin.presupuesto.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class EmailService {

    private final SesClient sesClient;

    @Value("${email.charset:UTF-8}")
    private String charsetUtf8;

    @Autowired
    public EmailService(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    public String sendSimpleEmail(String from, String to, String subject, String body) throws SesException {
        // Use Consumer Builder method for Body

        // Crear la petición de envío y enviar el email
        SendEmailResponse response = sesClient.sendEmail(
            SendEmailRequest.builder()
                .source(from)
                .destination(d -> d.toAddresses(to))
                .message(m -> m
                    .subject(s -> s.data(subject).charset(charsetUtf8))
                    .body(b -> b.text(tb -> tb.data(body).charset(charsetUtf8)))
                )
                .build()
        );
        return response.messageId();
    }

    public String sendHtmlEmail(String from, String to, String subject, String htmlBody, String textBody) throws SesException {
        // Crear la petición de envío usando Consumer Builder para destination y message
        SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                .source(from)
                .destination(d -> d.toAddresses(to))
                .message(m -> m
                    .subject(s -> s.data(subject).charset(charsetUtf8))
                    .body(b -> {
                        b.html(h -> h.data(htmlBody).charset(charsetUtf8));
                        b.text(t -> t.data(textBody != null ? textBody : "").charset(charsetUtf8));
                    })
                )
                .build();

        // Enviar el email
        SendEmailResponse response = sesClient.sendEmail(sendEmailRequest);
        return response.messageId();
    }

    public void sendBudgetRequestNotification(String from, String to, String requestId, String requesterName, Double amount) throws SesException {
        String subject = "Nueva Solicitud de Presupuesto - " + requestId;
        
        String body = String.format(
            """
            Estimado/a,

            Se ha recibido una nueva solicitud de presupuesto:

            ID de Solicitud: %s
            Solicitante: %s
            Monto: $%.2f

            Por favor, revise la solicitud en el sistema.

            Saludos cordiales,
            Sistema de Presupuestos CDC
            """,
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
            """
            Estimado/a,

            Su solicitud de presupuesto ha sido %s:

            ID de Solicitud: %s
            Estado: %s
            %s

            Saludos cordiales,
            Presupuestos
            """,
            statusText, requestId, status,
            comments != null && !comments.isEmpty() ? "Comentarios: " + comments : ""
        );

        sendSimpleEmail(from, to, subject, body);
    }
}
