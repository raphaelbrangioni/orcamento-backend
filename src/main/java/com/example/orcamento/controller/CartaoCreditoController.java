package com.example.orcamento.controller;

import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.service.CartaoCreditoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cartoes-credito")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:8080")
@CrossOrigin(origins = "http://localhost")
public class CartaoCreditoController {
    private final CartaoCreditoService cartaoCreditoService;

    @GetMapping
    public ResponseEntity<List<CartaoCredito>> listarCartoes() {
        return ResponseEntity.ok(cartaoCreditoService.listarCartoes());
    }

    @PostMapping
    public ResponseEntity<CartaoCredito> cadastrarCartao(@RequestBody CartaoCredito cartao) {
        return ResponseEntity.ok(cartaoCreditoService.salvarCartao(cartao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirCartao(@PathVariable Long id) {
        cartaoCreditoService.excluirCartao(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartaoCredito> atualizarCartao(@PathVariable Long id, @RequestBody CartaoCredito cartaoAtualizado) {
        return ResponseEntity.ok(cartaoCreditoService.atualizarCartao(id, cartaoAtualizado));
    }
}