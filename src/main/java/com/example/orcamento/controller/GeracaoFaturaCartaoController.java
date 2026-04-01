package com.example.orcamento.controller;

import com.example.orcamento.dto.GeracaoFaturaCartaoPatchRequestDTO;
import com.example.orcamento.dto.GeracaoFaturaCartaoResponseDTO;
import com.example.orcamento.service.GeracaoFaturaCartaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/geracoes-fatura")
@RequiredArgsConstructor
public class GeracaoFaturaCartaoController {

    private final GeracaoFaturaCartaoService geracaoFaturaCartaoService;

    @PostMapping("/{cartaoCreditoId}/{ano}/{mes}")
    public ResponseEntity<GeracaoFaturaCartaoResponseDTO> gerarFatura(
            @PathVariable Long cartaoCreditoId,
            @PathVariable int ano,
            @PathVariable int mes
    ) {
        return ResponseEntity.ok(geracaoFaturaCartaoService.gerarFatura(cartaoCreditoId, ano, mes));
    }

    @GetMapping("/{cartaoCreditoId}/{ano}/{mes}")
    public ResponseEntity<GeracaoFaturaCartaoResponseDTO> buscarGeracao(
            @PathVariable Long cartaoCreditoId,
            @PathVariable int ano,
            @PathVariable int mes
    ) {
        return geracaoFaturaCartaoService.buscarGeracao(cartaoCreditoId, ano, mes)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<GeracaoFaturaCartaoResponseDTO>> listarGeracoes(
            @RequestParam int ano,
            @RequestParam int mes
    ) {
        return ResponseEntity.ok(geracaoFaturaCartaoService.listarGeracoes(ano, mes));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GeracaoFaturaCartaoResponseDTO> ajustarGeracao(
            @PathVariable Long id,
            @RequestBody GeracaoFaturaCartaoPatchRequestDTO request
    ) {
        return ResponseEntity.ok(geracaoFaturaCartaoService.ajustarGeracao(id, request));
    }
}
