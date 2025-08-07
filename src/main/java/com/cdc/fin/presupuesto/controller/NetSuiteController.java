package com.cdc.fin.presupuesto.controller;

import com.cdc.fin.presupuesto.service.NetSuiteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
public class NetSuiteController {

    private final NetSuiteService netSuiteService;

    public NetSuiteController(NetSuiteService netSuiteService) {
        this.netSuiteService = netSuiteService;
    }

    @GetMapping("/api/netsuite/test-connection")
    public ResponseEntity<?> testConnection() {
        String result = netSuiteService.testConnection();
        return ResponseEntity.ok(result);
    }
}