package com.example.orcamento.controller;

import com.example.orcamento.dto.conciliacao.ConciliacaoOfxRelatorioDTO;
import com.example.orcamento.service.ConciliacaoOfxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/conciliacao/ofx")
@RequiredArgsConstructor
public class ConciliacaoOfxController {

    private final ConciliacaoOfxService conciliacaoOfxService;

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
