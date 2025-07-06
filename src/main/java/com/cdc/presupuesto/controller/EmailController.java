package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.ses.model.SesException;

import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class EmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendEmail(@RequestBody EmailRequest emailRequest,
                                                        @AuthenticationPrincipal Jwt jwt) {
        try {
            String messageId;
            
            if (emailRequest.getHtmlBody() != null && !emailRequest.getHtmlBody().isEmpty()) {
                // Enviar email HTML
                messageId = emailService.sendHtmlEmail(
                    emailRequest.getFrom(),
                    emailRequest.getTo(),
                    emailRequest.getSubject(),
                    emailRequest.getHtmlBody(),
                    emailRequest.getBody()
                );
            } else {
                // Enviar email de texto simple
                messageId = emailService.sendSimpleEmail(
                    emailRequest.getFrom(),
                    emailRequest.getTo(),
                    emailRequest.getSubject(),
                    emailRequest.getBody()
                );
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success", 
                    "message", "Email sent successfully",
                    "messageId", messageId
            ));
        } catch (SesException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error", 
                            "message", "Failed to send email: " + e.awsErrorDetails().errorMessage(),
                            "errorCode", e.awsErrorDetails().errorCode()
                    ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error", 
                            "message", "Failed to send email: " + e.getMessage()
                    ));
        }
    }

    @PostMapping("/send-budget-notification")
    public ResponseEntity<Map<String, String>> sendBudgetNotification(@RequestBody BudgetNotificationRequest request,
                                                                      @AuthenticationPrincipal Jwt jwt) {
        try {
            emailService.sendBudgetRequestNotification(
                request.getFrom(),
                request.getTo(),
                request.getRequestId(),
                request.getRequesterName(),
                request.getAmount()
            );

            return ResponseEntity.ok(Map.of(
                    "status", "success", 
                    "message", "Budget notification sent successfully"
            ));
        } catch (SesException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error", 
                            "message", "Failed to send notification: " + e.awsErrorDetails().errorMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error", 
                            "message", "Failed to send notification: " + e.getMessage()
                    ));
        }
    }

    @PostMapping("/send-status-notification")
    public ResponseEntity<Map<String, String>> sendStatusNotification(@RequestBody StatusNotificationRequest request,
                                                                      @AuthenticationPrincipal Jwt jwt) {
        try {
            emailService.sendBudgetStatusNotification(
                request.getFrom(),
                request.getTo(),
                request.getRequestId(),
                request.getStatus(),
                request.getComments()
            );

            return ResponseEntity.ok(Map.of(
                    "status", "success", 
                    "message", "Status notification sent successfully"
            ));
        } catch (SesException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error", 
                            "message", "Failed to send notification: " + e.awsErrorDetails().errorMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error", 
                            "message", "Failed to send notification: " + e.getMessage()
                    ));
        }
    }

    // Clases de Request
    public static class EmailRequest {
        private String to;
        private String subject;
        private String body;
        private String htmlBody;
        private String from;

        // Getters and setters
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        
        public String getHtmlBody() { return htmlBody; }
        public void setHtmlBody(String htmlBody) { this.htmlBody = htmlBody; }
        
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
    }

    public static class BudgetNotificationRequest {
        private String to;
        private String from;
        private String requestId;
        private String requesterName;
        private Double amount;

        // Getters and setters
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getRequesterName() { return requesterName; }
        public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
        
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
    }

    public static class StatusNotificationRequest {
        private String to;
        private String from;
        private String requestId;
        private String status;
        private String comments;

        // Getters and setters
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }
}
