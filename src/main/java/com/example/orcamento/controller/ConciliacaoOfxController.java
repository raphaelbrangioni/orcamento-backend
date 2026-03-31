package com.example.orcamento.controller;

import com.example.orcamento.dto.conciliacao.ConciliacaoOfxProcessamentoDTO;
import com.example.orcamento.dto.conciliacao.ConciliacaoOfxRelatorioDTO;
import com.example.orcamento.service.ConciliacaoOfxProcessamentoService;
import com.example.orcamento.service.ConciliacaoOfxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/conciliacao/ofx")
@RequiredArgsConstructor
public class ConciliacaoOfxController {

    private final ConciliacaoOfxService conciliacaoOfxService;
    private final ConciliacaoOfxProcessamentoService conciliacaoOfxProcessamentoService;

    @GetMapping("/processamentos")
    public ResponseEntity<List<ConciliacaoOfxProcessamentoDTO>> listarProcessamentos(
            @RequestParam(required = false) Long contaCorrenteId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate processadoDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate processadoAte
    ) {
        return ResponseEntity.ok(conciliacaoOfxProcessamentoService.listarProcessamentos(
                contaCorrenteId,
                status,
                conciliacaoOfxProcessamentoService.inicioDoDia(processadoDe),
                conciliacaoOfxProcessamentoService.fimDoDia(processadoAte)
        ));
    }

    @PostMapping(value = "/despesas-pagas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConciliacaoOfxRelatorioDTO> conciliarDespesasPagas(
            @RequestParam Long contaCorrenteId,
            @RequestParam(required = false, defaultValue = "2") Integer toleranciaDias,
            @RequestParam(required = false, defaultValue = "0.00") BigDecimal toleranciaValor,
            @RequestParam(required = false, defaultValue = "1000.00") BigDecimal toleranciaValorMinimo,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(conciliacaoOfxService.conciliar(
                contaCorrenteId,
                toleranciaDias,
                toleranciaValor,
                toleranciaValorMinimo,
                file
        ));
    }

    @PostMapping(value = "/extrato", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConciliacaoOfxRelatorioDTO> conciliarExtrato(
            @RequestParam Long contaCorrenteId,
            @RequestParam(required = false, defaultValue = "2") Integer toleranciaDias,
            @RequestParam(required = false, defaultValue = "0.00") BigDecimal toleranciaValor,
            @RequestParam(required = false, defaultValue = "1000.00") BigDecimal toleranciaValorMinimo,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(conciliacaoOfxService.conciliar(
                contaCorrenteId,
                toleranciaDias,
                toleranciaValor,
                toleranciaValorMinimo,
                file
        ));
    }
}
