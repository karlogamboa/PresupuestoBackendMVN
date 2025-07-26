package com.cdc.fin.presupuesto.controller;

import com.cdc.fin.presupuesto.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailService emailService;
    @Value("${email.from}")
    private String emailFrom;

    @Autowired
    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest request) {
        try {
            String messageId = emailService.sendSimpleEmail(
                emailFrom,
                request.getTo(),
                request.getSubject(),
                request.getBody()
            );
            return ResponseEntity.ok(messageId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al enviar el correo: " + e.getMessage());
        }
    }

    @PostMapping("/send-budget-notification")
    public ResponseEntity<String> sendBudgetNotification(@RequestBody BudgetNotificationRequest request) {
        try {
            emailService.sendBudgetRequestNotification(
                emailFrom,
                request.getTo(),
                request.getRequestId(),
                request.getRequesterName(),
                request.getAmount()
            );
            return ResponseEntity.ok("Notificación enviada correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al enviar la notificación: " + e.getMessage());
        }
    }

    @PostMapping("/send-status-notification")
    public ResponseEntity<String> sendStatusNotification(@RequestBody StatusNotificationRequest request) {
        try {
            emailService.sendBudgetStatusNotification(
                emailFrom,
                request.getTo(),
                request.getRequestId(),
                request.getStatus(),
                request.getComments()
            );
            return ResponseEntity.ok("Notificación de estatus enviada correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al enviar la notificación de estatus: " + e.getMessage());
        }
    }

    // DTOs internos para las peticiones
    public static class EmailRequest {
        private String to;
        private String subject;
        private String body;
        // getters y setters
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }

    public static class BudgetNotificationRequest {
        private String to;
        private String requestId;
        private String requesterName;
        private Double amount;
        // getters y setters
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getRequesterName() { return requesterName; }
        public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
    }

    public static class StatusNotificationRequest {
        private String to;
        private String requestId;
        private String status;
        private String comments;
        // getters y setters
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }
}
