// src/main/java/com/example/orcamento/controller/RelatorioController.java
package com.example.orcamento.controller;

import com.example.orcamento.service.RelatorioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/relatorios")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:8080")
@CrossOrigin(origins = "http://localhost")
@Slf4j
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/tipo-despesa")
    public ResponseEntity<List<Map<String, Object>>> getGastosPorTipoDespesa(
            @RequestParam("mesAnoFatura") String mesAnoFatura,
            @RequestParam(value = "cartaoId", required = false) Long cartaoId,
            @RequestParam(value = "proprietario", required = false) String proprietario) {
        log.info("Requisição GET em /api/v1/relatorios/tipo-despesa, getGastosPorTipoDespesa");
        List<Map<String, Object>> relatorio = relatorioService.getGastosPorTipoDespesa(mesAnoFatura, cartaoId, proprietario);
        return ResponseEntity.ok(relatorio);
    }

    // Adicione ao existente
    @GetMapping("/cartao-credito")
    public ResponseEntity<List<Map<String, Object>>> getGastosPorCartaoCredito(
            @RequestParam("mesAnoFatura") String mesAnoFatura) {
        log.info("Requisição GET em /api/v1/relatorios/cartao-credito, getGastosPorCartaoCredito");
        List<Map<String, Object>> relatorio = relatorioService.getGastosPorCartaoCredito(mesAnoFatura);
        return ResponseEntity.ok(relatorio);
    }

    // Novo endpoint para despesas gerais
    @GetMapping("/despesas-por-tipo")
    public ResponseEntity<List<Map<String, Object>>> getDespesasPorTipo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long subcategoriaId) {
        log.info("Requisição GET em /api/v1/relatorios/despesas-por-tipo com filtros: dataInicio={}, dataFim={}, subcategoriaId={}",
                dataInicio, dataFim, subcategoriaId);
        List<Map<String, Object>> relatorio = relatorioService.getDespesasPorSubcategoria(dataInicio, dataFim, subcategoriaId);
        return ResponseEntity.ok(relatorio);
    }
}