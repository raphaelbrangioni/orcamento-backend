package com.example.orcamento.controller;

import com.example.orcamento.model.TipoDespesa;
import com.example.orcamento.service.TipoDespesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tipos-despesa")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:8080")
@CrossOrigin(origins = "http://localhost")
@Slf4j
public class TipoDespesaController {

    private final TipoDespesaService tipoDespesaService;

    @GetMapping
    public ResponseEntity<List<TipoDespesa>> listarTipos() {
        log.info("Requisição GET em /api/v1/tipos-despesa, listarTipos");
        return ResponseEntity.ok(tipoDespesaService.listarTipos());
    }

//    @PostMapping
//    public ResponseEntity<TipoDespesa> cadastrarTipo(@RequestBody TipoDespesa tipo) {
//
//        return ResponseEntity.ok(tipoDespesaService.cadastrarTipo(tipo));
//    }

    @PostMapping
    public ResponseEntity<TipoDespesa> cadastrarTipo(@RequestBody TipoDespesa tipo) {
        log.info("Requisição POST em /api/v1/tipos-despesa, cadastrarTipo");
        log.info("Tipo Informado na requisição: {}", tipo.toString());

        TipoDespesa salvo = tipoDespesaService.cadastrarTipo(tipo);

        log.info("Tipo salvo: {}",salvo);
        ResponseEntity<TipoDespesa> response = ResponseEntity.ok(salvo);
       log.info("Retornando resposta: " + response.getStatusCode() + " - " + response.getBody());
        return response;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirTipo(@PathVariable Long id) {
        log.info("Requisição DELETE em /api/v1/tipos-despesa/{id}, excluirTipo");
        tipoDespesaService.excluirTipo(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoDespesa> atualizarTipoDespesa(
            @PathVariable Long id,
            @RequestBody @Valid TipoDespesa tipoDespesaAtualizado) {
        TipoDespesa tipoDespesa = tipoDespesaService.atualizarTipoDespesa(id, tipoDespesaAtualizado);
        return ResponseEntity.ok(tipoDespesa); // Retorna o tipo de despesa atualizado
    }
}

