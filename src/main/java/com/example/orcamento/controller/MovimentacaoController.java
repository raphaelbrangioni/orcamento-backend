// src/main/java/com/example/orcamento/controller/MovimentacaoController.java
package com.example.orcamento.controller;

import com.example.orcamento.dto.TransferenciaEntreContasRequestDTO;
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

    @PostMapping("/transferencias")
    public ResponseEntity<List<Movimentacao>> transferirEntreContas(@RequestBody TransferenciaEntreContasRequestDTO request) {
        log.info("Requisição POST em /api/v1/movimentacoes/transferencias, transferirEntreContas");
        List<Movimentacao> movimentacoes = movimentacaoService.transferirEntreContas(
                request.getContaOrigemId(),
                request.getContaDestinoId(),
                request.getValor(),
                request.getData(),
                request.getDescricao()
        );
        return ResponseEntity.ok(movimentacoes);
    }

    @PostMapping("/transferencias/{transferenciaId}/estornar")
    public ResponseEntity<List<Movimentacao>> estornarTransferencia(@PathVariable String transferenciaId) {
        log.info("Requisição POST em /api/v1/movimentacoes/transferencias/{}/estornar, estornarTransferencia", transferenciaId);
        List<Movimentacao> movimentacoes = movimentacaoService.estornarTransferencia(transferenciaId);
        return ResponseEntity.ok(movimentacoes);
    }
}