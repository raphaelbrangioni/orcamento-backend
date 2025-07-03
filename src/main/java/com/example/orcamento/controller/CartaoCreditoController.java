package com.example.orcamento.controller;

import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.service.CartaoCreditoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cartoes-credito")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:8080")
@CrossOrigin(origins = "http://localhost")
@Slf4j
public class CartaoCreditoController {
    private final CartaoCreditoService cartaoCreditoService;

    @GetMapping
    public ResponseEntity<List<CartaoCredito>> listarCartoes() {
        log.info("Requisição GET em /api/v1/cartoes-credito, listarCartoes");
        return ResponseEntity.ok(cartaoCreditoService.listarCartoes());
    }

    @PostMapping
    public ResponseEntity<CartaoCredito> cadastrarCartao(@RequestBody CartaoCredito cartao) {
        log.info("Requisição POST em /api/v1/cartoes-credito, cadastrarCartao");
        return ResponseEntity.ok(cartaoCreditoService.salvarCartao(cartao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirCartao(@PathVariable Long id) {
        log.info("Requisição DELETE em /api/v1/cartoes-credito/{id}, excluirCartao");
        cartaoCreditoService.excluirCartao(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartaoCredito> atualizarCartao(@PathVariable Long id, @RequestBody CartaoCredito cartaoAtualizado) {
        log.info("Requisição PUT em /api/v1/cartoes-credito/{id}, atualizarCartao");
        return ResponseEntity.ok(cartaoCreditoService.atualizarCartao(id, cartaoAtualizado));
    }
}