package com.example.orcamento.controller;

import com.example.orcamento.dto.FechamentoMensalResponseDTO;
import com.example.orcamento.service.FechamentoMensalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fechamentos-mensais")
@RequiredArgsConstructor
public class FechamentoMensalController {

    private final FechamentoMensalService fechamentoMensalService;

    @PostMapping("/{ano}/{mes}/fechar")
    public ResponseEntity<FechamentoMensalResponseDTO> fecharMes(@PathVariable int ano, @PathVariable int mes) {
        return ResponseEntity.ok(fechamentoMensalService.fecharMes(ano, mes));
    }

    @GetMapping("/{ano}/{mes}")
    public ResponseEntity<FechamentoMensalResponseDTO> buscarFechamento(@PathVariable int ano, @PathVariable int mes) {
        return ResponseEntity.ok(fechamentoMensalService.buscarFechamento(ano, mes));
    }

    @GetMapping
    public ResponseEntity<List<FechamentoMensalResponseDTO>> listarFechamentos(
            @RequestParam(required = false) Integer ano
    ) {
        return ResponseEntity.ok(fechamentoMensalService.listarFechamentos(ano));
    }

    @DeleteMapping("/{ano}/{mes}")
    public ResponseEntity<Void> reabrirFechamento(@PathVariable int ano, @PathVariable int mes) {
        fechamentoMensalService.reabrirMes(ano, mes);
        return ResponseEntity.noContent().build();
    }
}
