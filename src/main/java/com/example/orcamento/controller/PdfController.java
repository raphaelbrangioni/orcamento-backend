package com.example.orcamento.controller;

import com.example.orcamento.model.Transaction;
import com.example.orcamento.service.CartaoCreditoService;
import com.example.orcamento.service.extractor.PdfCartaoExtractor;
import com.example.orcamento.service.extractor.PdfCartaoExtractorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pdf")
@Slf4j
public class PdfController {

    private CartaoCreditoService cartaoCreditoService;

    @Autowired
    public void setCartaoCreditoService(CartaoCreditoService cartaoCreditoService) {
        this.cartaoCreditoService = cartaoCreditoService;
    }

    @PostMapping("/analisar")
    public ResponseEntity<?> analisarPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("cartaoId") Long cartaoId,
            @RequestParam(value = "modeloImportacao", required = false) String modeloImportacao
    )
    {
        log.info("Analisando pdf");
        log.info("modeloImportacao enviado: {}", modeloImportacao);
        try {
            // Se modeloImportacao não vier no request, busca do cartão
            if (modeloImportacao == null || modeloImportacao.isBlank()) {
                modeloImportacao = cartaoCreditoService.buscarPorId(cartaoId).getModeloImportacao();
            }
            InputStream pdfInputStream = file.getInputStream();
            PdfCartaoExtractor extractor = PdfCartaoExtractorFactory.getExtractor(modeloImportacao);
            List<Transaction> transacoes = extractor.extrair(pdfInputStream);
            BigDecimal total = transacoes.stream()
                    .map(t -> new BigDecimal(t.getValor()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.setScale(2, BigDecimal.ROUND_HALF_UP);
            Map<String, Object> resposta = new HashMap<>();
            resposta.put("transacoes", transacoes);
            resposta.put("total", total.toString());
            resposta.put("quantidade", transacoes.size());
            log.info("Transações encontradas: {}", transacoes.size());
            log.info("Total calculado: R$ {}", total);
            return ResponseEntity.ok(resposta);
        } catch (IOException e) {
            log.error("Erro ao processar arquivo PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("erro", "Falha ao processar o arquivo PDF"));
        }
    }
}