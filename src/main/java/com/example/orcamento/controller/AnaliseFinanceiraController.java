package com.example.orcamento.controller;

import com.example.orcamento.dto.GastoRecorrenteDTO;
import com.example.orcamento.dto.PrevisaoGastoDTO;
import com.example.orcamento.dto.SugestaoEconomiaDTO;
import com.example.orcamento.service.AnaliseFinanceiraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/despesas/analise")
public class AnaliseFinanceiraController {

    @Autowired
    private AnaliseFinanceiraService analiseFinanceiraService;

    @GetMapping("/recorrentes")
    public ResponseEntity<List<GastoRecorrenteDTO>> analisarGastosRecorrentes() {
        return ResponseEntity.ok(analiseFinanceiraService.analisarGastosRecorrentes());
    }

    @GetMapping("/sugestoes")
    public ResponseEntity<List<SugestaoEconomiaDTO>> gerarSugestoes() {
        return ResponseEntity.ok(analiseFinanceiraService.gerarSugestoes());
    }

    @GetMapping("/previsoes")
    public ResponseEntity<List<PrevisaoGastoDTO>> gerarPrevisoes() {
        return ResponseEntity.ok(analiseFinanceiraService.gerarPrevisoes());
    }
}
