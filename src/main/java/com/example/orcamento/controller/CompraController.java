// src/main/java/com/example/orcamento/controller/CompraController.java
package com.example.orcamento.controller;

import com.example.orcamento.model.Compra;
import com.example.orcamento.service.CompraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/compras")
public class CompraController {
    private final CompraService compraService;

    @Autowired
    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping("/parceladas")
    public ResponseEntity<Compra> cadastrarCompraParcelada(
            @RequestBody Compra compra,
            @RequestParam String mesPrimeiraParcela,
            @RequestParam Integer numeroParcelas) {
        Compra novaCompra = compraService.cadastrarCompraParcelada(compra, mesPrimeiraParcela, numeroParcelas);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaCompra);
    }

//    @GetMapping("/ultimas")
//    public ResponseEntity<List<Compra>> listarUltimasCompras() {
//        List<Compra> ultimasCompras = compraService.listarUltimasCompras(5); // Últimas 5 compras
//        return ResponseEntity.ok(ultimasCompras);
//    }

    // Novo endpoint para editar
    @PutMapping("/{id}")
    public ResponseEntity<Compra> editarCompra(
            @PathVariable Long id,
            @RequestBody Compra compraAtualizada,
            @RequestParam String mesPrimeiraParcela,
            @RequestParam Integer numeroParcelas) {
        Compra compra = compraService.editarCompra(id, compraAtualizada, mesPrimeiraParcela, numeroParcelas);
        return ResponseEntity.ok(compra);
    }

    // Novo endpoint para excluir
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirCompra(@PathVariable Long id) {
        compraService.excluirCompra(id);
        return ResponseEntity.noContent().build();
    }

    // Endpoint ajustado para suportar filtros e paginação
    @GetMapping("/ultimas")
    public ResponseEntity<Page<Compra>> listarCompras(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) Long cartaoId,
            @RequestParam(required = false) Long tipoDespesaId) {
        Page<Compra> compras = compraService.listarCompras(page, size, descricao, cartaoId, tipoDespesaId);
        return ResponseEntity.ok(compras);
    }
}