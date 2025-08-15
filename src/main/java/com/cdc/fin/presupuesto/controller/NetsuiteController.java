package com.cdc.fin.presupuesto.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import com.cdc.fin.presupuesto.service.NetSuiteService;

@RestController
@RequestMapping("/netsuite")
public class NetsuiteController {

    private final NetSuiteService netSuiteService;

    @Autowired
    public NetsuiteController(NetSuiteService netSuiteService) {
        this.netSuiteService = netSuiteService;
    }

    @GetMapping("/accounts")
    public ResponseEntity<String> getAccounts() {
        String response = netSuiteService.getResource("account");
    System.out.println("Netsuite /account response: " + response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/departments")
    public ResponseEntity<String> getDepartments() {
        String response = netSuiteService.getResource("department");
    System.out.println("Netsuite /department response: " + response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customers")
    public ResponseEntity<String> getCustomers() {
        String response = netSuiteService.getResource("customer");
    System.out.println("Netsuite /customer response: " + response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/budgets")
    public ResponseEntity<String> getBudgets() {
        String response = netSuiteService.getResource("budget");
    System.out.println("Netsuite /budget response: " + response);
        return ResponseEntity.ok(response);
    }
}
