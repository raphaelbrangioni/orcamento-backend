// src/main/java/com/example/orcamento/controller/MovimentacaoController.java
package com.example.orcamento.controller;

import com.example.orcamento.model.Movimentacao;
import com.example.orcamento.service.MovimentacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/movimentacoes")
@RequiredArgsConstructor
@Slf4j
public class MovimentacaoController {

    private final MovimentacaoService movimentacaoService;

    @GetMapping
    public ResponseEntity<List<Movimentacao>> listarMovimentacoes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long contaCorrenteId) {
        log.info("Requisição GET em /api/v1/movimentacoes, listarMovimentacoes");
        log.info("dataInicio: {}, dataFim: {}, contaCorrenteId: {}", dataInicio, dataFim, contaCorrenteId);

        List<Movimentacao> movimentacoes = movimentacaoService.listarMovimentacoesPorConta(contaCorrenteId, dataInicio, dataFim);
        return ResponseEntity.ok(movimentacoes);
    }
}