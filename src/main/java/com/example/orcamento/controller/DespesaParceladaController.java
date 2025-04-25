package com.example.orcamento.controller;

import com.example.orcamento.dto.DespesaParceladaDTO;
import com.example.orcamento.model.DespesaParcelada;
import com.example.orcamento.service.DespesaParceladaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/despesas-parceladas")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost")
@Slf4j
public class DespesaParceladaController {

    private final DespesaParceladaService despesaParceladaService;

    @GetMapping
    public ResponseEntity<Page<DespesaParcelada>> listarDespesasParceladas(
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) Long tipoDespesaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Requisição GET em /api/v1/despesas-parceladas, descricao: {}, tipoDespesaId: {}, page: {}, size: {}",
                descricao, tipoDespesaId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("dataCadastro").descending());
        Page<DespesaParcelada> despesas = despesaParceladaService.listarDespesasParceladas(
                descricao, tipoDespesaId, pageable);

        return ResponseEntity.ok(despesas);
    }

    @PostMapping
    public ResponseEntity<DespesaParcelada> cadastrarDespesaParcelada(@RequestBody DespesaParceladaDTO dto) {
        log.info("Requisição POST em /api/v1/despesas-parceladas, cadastrarDespesaParcelada");
        DespesaParcelada despesaSalva = despesaParceladaService.salvarDespesaParcelada(dto);
        return ResponseEntity.ok(despesaSalva);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DespesaParcelada> atualizarDespesaParcelada(
            @PathVariable Long id, @RequestBody DespesaParceladaDTO dto) {
        log.info("Requisição PUT em /api/v1/despesas-parceladas/{}, atualizarDespesaParcelada", id);
        DespesaParcelada despesaAtualizada = despesaParceladaService.atualizarDespesaParcelada(id, dto);
        return ResponseEntity.ok(despesaAtualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirDespesaParcelada(@PathVariable Long id) {
        log.info("Requisição DELETE em /api/v1/despesas-parceladas/{}, excluirDespesaParcelada", id);
        despesaParceladaService.excluirDespesaParcelada(id);
        return ResponseEntity.noContent().build();
    }
}