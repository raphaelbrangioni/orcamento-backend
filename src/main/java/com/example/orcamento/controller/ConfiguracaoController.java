package com.example.orcamento.controller;

import com.example.orcamento.dto.ConfiguracaoDTO;
import com.example.orcamento.service.ConfiguracaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/configuracoes")
@RequiredArgsConstructor
@Slf4j
public class ConfiguracaoController {

    private final ConfiguracaoService configuracaoService;

    @GetMapping
    public ResponseEntity<ConfiguracaoDTO> getConfiguracoes() {
        log.info("Buscando configurações");
        return ResponseEntity.ok(configuracaoService.getConfiguracoes());
    }

    @PostMapping
    public ResponseEntity<ConfiguracaoDTO> salvarConfiguracoes(@RequestBody ConfiguracaoDTO configuracaoDTO) {
        log.info("Salvando configurações: {}", configuracaoDTO);
        return ResponseEntity.ok(configuracaoService.salvarConfiguracoes(configuracaoDTO));
    }
}
