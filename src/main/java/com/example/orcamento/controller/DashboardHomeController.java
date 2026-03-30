package com.example.orcamento.controller;

import com.example.orcamento.dto.dashboard.DashboardHomeDTO;
import com.example.orcamento.service.DashboardHomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard/home")
@RequiredArgsConstructor
public class DashboardHomeController {

    private final DashboardHomeService dashboardHomeService;

    @GetMapping
    public ResponseEntity<DashboardHomeDTO> obterDashboard(
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes
    ) {
        return ResponseEntity.ok(dashboardHomeService.obterDashboard(ano, mes));
    }
}
