package com.example.orcamento.controller;

import com.example.orcamento.model.HorasTrabalhadas;
import com.example.orcamento.service.HorasTrabalhadasService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/horas-trabalhadas")
@AllArgsConstructor
@Slf4j
public class HorasTrabalhadasController {

    private final HorasTrabalhadasService service;

    @PostMapping
    public ResponseEntity<HorasTrabalhadas> registrar(@RequestBody HorasTrabalhadas horasTrabalhadas) {
        log.info("Recebida requisição para registrar horas: {}", horasTrabalhadas);
        HorasTrabalhadas horasSalvas = service.registrar(horasTrabalhadas);
        return new ResponseEntity<>(horasSalvas, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<HorasTrabalhadas>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long projetoId) {

        log.info("Recebida requisição para listar horas com filtros: inicio={}, fim={}, clienteId={}, projetoId={}", inicio, fim, clienteId, projetoId);

        Map<String, Object> filtros = new HashMap<>();
        if (inicio != null) filtros.put("inicio", inicio);
        if (fim != null) filtros.put("fim", fim);
        if (clienteId != null) filtros.put("clienteId", clienteId);
        if (projetoId != null) filtros.put("projetoId", projetoId);

        List<HorasTrabalhadas> resultado = service.listar(filtros);
        return ResponseEntity.ok(resultado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HorasTrabalhadas> atualizar(@PathVariable Long id, @RequestBody HorasTrabalhadas horasTrabalhadas) {
        log.info("Recebida requisição para atualizar horas com id: {} para: {}", id, horasTrabalhadas);
        HorasTrabalhadas horasAtualizadas = service.atualizar(id, horasTrabalhadas);
        return ResponseEntity.ok(horasAtualizadas);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        log.info("Recebida requisição para excluir horas com id: {}", id);
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
