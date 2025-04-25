// src/main/java/com/example/orcamento/controller/ReceitaController.java
package com.example.orcamento.controller;

import com.example.orcamento.model.Receita;
import com.example.orcamento.repository.ReceitaRepository;
import com.example.orcamento.service.ReceitaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/receitas")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost")
@Slf4j
public class ReceitaController {

    private final ReceitaService receitaService;
    private final ReceitaRepository receitaRepository;

    @GetMapping
    public ResponseEntity<List<Receita>> listarReceitas() {
        return ResponseEntity.ok(receitaService.listarReceitas());
    }

    @PostMapping
    public ResponseEntity<Receita> cadastrarReceita(@RequestBody Receita receita) {
        return ResponseEntity.ok(receitaService.salvarReceita(receita));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Receita> atualizarReceita(@PathVariable Long id, @RequestBody Receita receita) {
        log.info("Requisição PUT em /api/v1/receitas/{id}");
        log.info("ID informado para ser atualizado: {}", id);
        return ResponseEntity.ok(receitaService.atualizarReceita(id, receita));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirReceita(@PathVariable Long id) {
        log.info("Requisição DELETE em /api/v1/receitas/{id}");
        log.info("ID informado para ser deletado: {}", id);
        receitaService.excluirReceita(id);
        return ResponseEntity.noContent().build();
    }

    // src/main/java/com/example/orcamento/controller/ReceitaController.java
    @GetMapping("/por-mes/{ano}")
    public ResponseEntity<Map<String, Map<String, BigDecimal>>> listarReceitasPorMes(
            @PathVariable int ano,
            @RequestParam(required = false) String tipo) {
        log.info("Requisição GET em: /api/v1/receitas//por-mes/{ano}");

        Map<String, Map<String, BigDecimal>> receitasPorMes = receitaService.listarReceitasPorMes(ano);
        if (tipo != null && !tipo.isEmpty()) {
            receitasPorMes.entrySet().forEach(entry ->
                    entry.getValue().replaceAll((k, v) ->
                            receitasPorMes.get(entry.getKey()).get(k).multiply(
                                    receitaRepository.findAll().stream()
                                            .filter(r -> r.getDataRecebimento().getYear() == ano)
                                            .filter(r -> r.getTipo().equals(tipo))
                                            .map(Receita::getValor)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                                            .divide(receitasPorMes.get(entry.getKey()).get(k), RoundingMode.HALF_UP)
                            )
                    )
            );
        }
        log.info("Retorno: {}", receitasPorMes);
        return ResponseEntity.ok(receitasPorMes);
    }
    @PutMapping("/{id}/efetivar")
    public ResponseEntity<Receita> efetivarReceita(@PathVariable Long id) {
        log.info("Requisição PUT em /api/v1/receitas/{id}/efetivar");
        log.info("ID informado para efetivar: {}", id);
        return ResponseEntity.ok(receitaService.efetivarReceita(id));
    }
}