package com.example.orcamento.controller;

import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.service.ContaCorrenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contas-corrente")
public class ContaCorrenteController {

    @Autowired
    private ContaCorrenteService contaCorrenteService;

    @PostMapping
    public ResponseEntity<ContaCorrente> criarConta(@RequestBody ContaCorrente conta) {
        ContaCorrente novaConta = contaCorrenteService.salvar(conta);
        return ResponseEntity.ok(novaConta);
    }

    @GetMapping
    public ResponseEntity<List<ContaCorrente>> listarContas() {
        List<ContaCorrente> contas = contaCorrenteService.listarTodos();
        return ResponseEntity.ok(contas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContaCorrente> buscarContaPorId(@PathVariable Long id) {
        return contaCorrenteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarConta(@PathVariable Long id) {
        contaCorrenteService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContaCorrente> atualizarConta(@PathVariable Long id, @RequestBody ContaCorrente contaAtualizada) {
        return ResponseEntity.ok(contaCorrenteService.atualizarConta(id, contaAtualizada));
    }
}