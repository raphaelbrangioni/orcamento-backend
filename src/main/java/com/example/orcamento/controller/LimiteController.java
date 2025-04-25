package com.example.orcamento.controller;

import com.example.orcamento.model.Limite;
import com.example.orcamento.service.LimiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/limites")
public class LimiteController {

    @Autowired
    private LimiteService limiteService;

    @PostMapping
    public ResponseEntity<Limite> cadastrarLimite(@RequestBody Limite limite) {
        Limite novoLimite = limiteService.salvarLimite(limite);
        return ResponseEntity.ok(novoLimite);
    }

    @GetMapping
    public ResponseEntity<List<Limite>> listarLimites(@RequestParam(required = false) Long tipoDespesaId) {
        List<Limite> limites;
        if (tipoDespesaId != null) {
            limites = limiteService.listarPorTipoDespesa(tipoDespesaId);
        } else {
            limites = limiteService.listarUltimos10Limites();
        }
        return ResponseEntity.ok(limites);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Limite> atualizarLimite(@PathVariable Long id, @RequestBody Limite limite) {
        Limite existente = limiteService.salvarLimite(limite);
        existente.setId(id);
        Limite atualizado = limiteService.salvarLimite(existente);
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarLimite(@PathVariable Long id) {
        limiteService.deletarLimite(id);
        return ResponseEntity.noContent().build();
    }
}