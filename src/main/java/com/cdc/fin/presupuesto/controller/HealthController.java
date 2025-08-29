package com.cdc.fin.presupuesto.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> health() {
        log.info("Health check requested");
        Map<String, Object> status = Map.of(
                "status", "UP",
                "service", "Presupuesto Backend",
                "timestamp", Instant.now().toEpochMilli()
        );
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(status);
    }
}