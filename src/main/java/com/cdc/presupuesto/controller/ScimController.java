package com.cdc.presupuesto.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scim/v2")
public class ScimController {

    @GetMapping("/Users")
    public String listUsers() {
        // Implementa lógica SCIM aquí
        return "{\"Resources\":[]}";
    }

    // Agrega más endpoints SCIM según sea necesario
}
