package com.example.orcamento.controller;

import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.MetaEconomia;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.service.MetaEconomiaService;
import com.example.orcamento.dto.MetaEconomiaRequestDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/metas-economia")
@Slf4j
public class MetaEconomiaController {

    @Autowired
    private MetaEconomiaService service;

    @Autowired
    DespesaRepository despesaRepository;

    @GetMapping
    public List<MetaEconomia> listarMetas() {
        return service.listarMetas();
    }

    @PostMapping
    public ResponseEntity<MetaEconomia> criarMeta(@RequestBody MetaEconomiaRequestDTO metaDto) {
        MetaEconomia novaMeta = service.salvarMeta(metaDto);
        return ResponseEntity.status(201).body(novaMeta);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MetaEconomia> atualizarMeta(@PathVariable Long id, @RequestBody MetaEconomiaRequestDTO metaDto) {

        log.info("Requisição PUT em /api/v1/metas-economia/{id}, atualizarMeta");
        log.info("Meta a ser atualizada: {}", metaDto);

        MetaEconomia atualizada = service.atualizarMeta(id, metaDto);

        log.info("Meta atualizada: {}", atualizada);

        return ResponseEntity.ok(atualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirMeta(@PathVariable Long id) {
        service.excluirMeta(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/bitcoin")
    public ResponseEntity<MetaEconomia> atualizarFracaoBitcoin(
            @PathVariable Long id,
            @RequestBody Map<String, Double> request) {
        Double fracaoBitcoin = request.get("fracaoBitcoin");
        MetaEconomia meta = service.atualizarFracaoBitcoin(id, fracaoBitcoin);
        return ResponseEntity.ok(meta);
    }

    @PostMapping("/{id}/desassociar-despesas")
    public ResponseEntity<Map<String, Object>> desassociarDespesas(@PathVariable Long id) {
        int count = service.desassociarDespesas(id);
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("message", "Despesas desassociadas com sucesso");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/com-despesas")
    public ResponseEntity<Void> excluirMetaComDespesas(@PathVariable Long id) {
        service.excluirMetaComDespesas(id);
        return ResponseEntity.noContent().build();
    }
}