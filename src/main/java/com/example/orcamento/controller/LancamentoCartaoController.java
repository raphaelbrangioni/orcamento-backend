// src/main/java/com/example/orcamento/controller/LancamentoCartaoController.java
package com.example.orcamento.controller;

import com.example.orcamento.dto.dashboard.FaturaCartaoAnualDTO;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.service.LancamentoCartaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/lancamentos-cartao")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost")
@Slf4j
public class LancamentoCartaoController {
    private final LancamentoCartaoService lancamentoCartaoService;

    @GetMapping
    public ResponseEntity<List<LancamentoCartao>> listarLancamentos() {
        log.info("Requisição GET em /api/v1/lancamentos-cartao, listarLancamentos");
        return ResponseEntity.ok(lancamentoCartaoService.listarLancamentos());
    }


    @GetMapping("/filtrar")
    public ResponseEntity<List<LancamentoCartao>> listarLancamentosPorFiltros(
            @RequestParam(required = false) Long cartaoId,
            @RequestParam(required = false) String mesAnoFatura) {
        log.info("Requisição GET em /api/v1/lancamentos-cartao/filtrar com filtros - cartaoId: {}, mesAnoFatura: {}", cartaoId, mesAnoFatura);

        List<LancamentoCartao> lancamentos = lancamentoCartaoService.listarLancamentosPorFiltros(cartaoId, mesAnoFatura);
        return ResponseEntity.ok(lancamentos);
    }


    @PostMapping
    public ResponseEntity<LancamentoCartao> cadastrarLancamento(@RequestBody LancamentoCartao lancamento) {
        log.info("Requisição POST em /api/v1/lancamentos-cartao, cadastrarLancamento");
        log.info("Lancamento: {}", lancamento);
        return ResponseEntity.ok(lancamentoCartaoService.cadastrarLancamento(lancamento));
    }

    @PostMapping("/multiplos")
    public ResponseEntity<List<LancamentoCartao>> cadastrarMultiplosLancamentos(@RequestBody List<LancamentoCartao> lancamentos) {
        log.info("Requisição POST em /api/v1/lancamentos-cartao/multiplos, cadastrarMultiplosLancamentos");
        log.info("Payload recebido: {}", lancamentos);
        return ResponseEntity.ok(lancamentoCartaoService.cadastrarMultiplosLancamentos(lancamentos));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LancamentoCartao> atualizarLancamento(@PathVariable Long id, @RequestBody LancamentoCartao lancamento) {
        log.info("Requisição PUT em /api/v1/lancamentos-cartao/{id}, atualizarLancamento");
        log.info("ID informado {}, lançamento: {}", id, lancamento);
        return ResponseEntity.ok(lancamentoCartaoService.atualizarLancamento(id, lancamento));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirLancamento(@PathVariable Long id) {
        log.info("Requisição DELETE em /api/v1/lancamentos-cartao/{id}, excluirLancamento");
        log.info("ID informado para ser deletado {}", id);
        lancamentoCartaoService.excluirLancamento(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/fatura-por-cartao")
    public ResponseEntity<Map<Long, Double>> getFaturaPorCartao(@RequestParam("mesAno") String mesAno) {
        log.info("Requisição GET em /api/v1/lancamentos-cartao/fatura-por-cartao, getFaturaPorCartao");
        List<LancamentoCartao> lancamentos = lancamentoCartaoService.listarLancamentos();
        Map<Long, Double> faturasPorCartao = lancamentos.stream()
                .filter(l -> l.getMesAnoFatura().equals(mesAno))
                .collect(Collectors.groupingBy(
                        l -> l.getCartaoCredito().getId(),
                        Collectors.summingDouble(l -> l.getValorTotal().doubleValue())
                ));
        return ResponseEntity.ok(faturasPorCartao);
    }

    @GetMapping("/faturas-ano")
    public List<FaturaCartaoAnualDTO> getFaturasPorAno(@RequestParam int ano) {
        log.info("Requisição GET em /api/v1/lancamentos-cartao/faturas-ano, ano: {}", ano);
        return lancamentoCartaoService.getFaturasAnuais(ano);
    }

    @PatchMapping("/{id}/pago-por-terceiro")
    public ResponseEntity<LancamentoCartao> atualizarStatusPagamento(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload) {
        log.info("Requisição PATCH em /api/v1/lancamentos-cartao/{id}/pago-por-terceiro, atualizarStatusPagamento");
        log.info("ID: {}, Status: {}", id, payload.get("pagoPorTerceiro"));

        Boolean pagoPorTerceiro = payload.get("pagoPorTerceiro");
        if (pagoPorTerceiro == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(lancamentoCartaoService.atualizarStatusPagamento(id, pagoPorTerceiro));
    }
}