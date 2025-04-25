package com.example.orcamento.controller;

import com.example.orcamento.model.Transaction;
import com.example.orcamento.service.PdfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pdf")
@Slf4j
public class PdfController {

    @PostMapping("/analisar")
    public ResponseEntity<List<Transaction>> analisarPdf(@RequestParam("file") MultipartFile file) {
        try {
            // Converte o arquivo MultipartFile em InputStream
            InputStream pdfInputStream = file.getInputStream();

            // Extrai registros do PDF como List<Transaction>
            List<Transaction> transacoes = PdfService.extrairInformacoesDoPdf(pdfInputStream);

            for(Transaction t: transacoes){
                log.info(t.getValor());
            }

            log.info("Transações encontradas: {}", transacoes);
            return ResponseEntity.ok(transacoes);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null); // Ou uma lista vazia, dependendo do caso
        }
    }
}