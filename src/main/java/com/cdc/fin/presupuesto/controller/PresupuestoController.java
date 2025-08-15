package com.cdc.fin.presupuesto.controller;

import com.cdc.fin.presupuesto.service.PresupuestoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/presupuestos")
public class PresupuestoController {
    @Autowired
    private PresupuestoService presupuestoService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPresupuesto(
        @RequestParam("centroCostos") String ceCo,
        @RequestParam("cuentaGastos") String cuentaGastos,
        @RequestParam("periodo") String periodo
    ) {
        // Revisar si ceCo tiene el formato esperado y extraer solo los dígitos antes del "-"
        if (ceCo != null && ceCo.contains("-")) {
            String[] parts = ceCo.split("-", 2);
            if (parts.length > 0 && parts[0].matches("\\d+")) {
                ceCo = parts[0];
            }
        }

        String presupuesto = presupuestoService.getPresupuesto(ceCo, cuentaGastos);
        Map<String, Object> result = new HashMap<>();
        if (presupuesto != null) {
            result.put("success", true);
            result.put("presupuesto", presupuesto);
            return ResponseEntity.ok(result);
        } else {
            result.put("success", false);
            result.put("message", "Presupuesto no encontrado");
            return ResponseEntity.ok(result);
        }
    }


    @PostMapping("/import-csv")
    public ResponseEntity<?> importPresupuestosFromCSV(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "replaceAll", defaultValue = "false") boolean replaceAll) {
        try {
            Map<String, Object> result = presupuestoService.importPresupuestosFromCSV(file, replaceAll);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
