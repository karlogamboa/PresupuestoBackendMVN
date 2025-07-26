package com.cdc.fin.presupuesto.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/user")
    public Map<String, Object> getUserInfo() {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("error", "SAML2 principal not available. Check dependencies.");
        return userInfo;
    }
}