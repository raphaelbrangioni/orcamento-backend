package com.example.orcamento.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.orcamento.service.AlertaService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/alertas")
public class AlertasController {

    @Autowired
    private AlertaService alertaService;

    @GetMapping("/limites")
    public ResponseEntity<List<Map<String, Object>>> verificarLimites() {
        List<Map<String, Object>> alertas = alertaService.verificarLimites();
        return ResponseEntity.ok(alertas);
    }
}