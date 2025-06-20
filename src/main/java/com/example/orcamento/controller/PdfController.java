package com.example.orcamento.controller;

import com.example.orcamento.model.Transaction;
import com.example.orcamento.service.PdfService;
import com.example.orcamento.service.extractor.PdfCartaoExtractorService;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pdf")
@Slf4j
public class PdfController {

    @Autowired
    private PdfCartaoExtractorService pdfCartaoExtractorService;

    @PostMapping("/analisar")
    public ResponseEntity<?> analisarPdf(@RequestParam("file") MultipartFile file, @RequestParam("cartaoId") Long cartaoId) {

        if(cartaoId == 7) {
            cartaoId = 5L;
        }

//        1	Mastercard Person Multiplo Black Pontos
//        2	Latam Pass Itaú Black
//        3	Visa Personalité Infinite
//        4	ZAFFARI CARD
//        5	Cartão Inter
//        6	Cartão C&A Pay
//        7	Cartão Inter - Amor
//        8	SANTANDER-ELITE CASHBACK SIGNATURE
//        9	Amazon Prime Mastercard

        try {
            // Converte o arquivo MultipartFile em InputStream
            InputStream pdfInputStream = file.getInputStream();

            List<Transaction> transacoes;
            // Nova abordagem: delega para o service especializado
            transacoes = (List<Transaction>) pdfCartaoExtractorService.extrair(cartaoId, pdfInputStream);

            // Calcular o total das transações
            double total = transacoes.stream()
                    .mapToDouble(t -> {
                        try {
                            String valorFormatado = t.getValor().replace(".", "").replace(",", ".");
                            return Double.parseDouble(valorFormatado);
                        } catch (NumberFormatException e) {
                            log.error("Erro ao converter valor: {}", t.getValor(), e);
                            return 0.0;
                        }
                    })
                    .sum();

            // Criar resposta com transações e total
            Map<String, Object> resposta = new HashMap<>();
            resposta.put("transacoes", transacoes);
            resposta.put("total", String.format("%.2f", total));
            resposta.put("quantidade", transacoes.size());

            log.info("Transações encontradas: {}", transacoes.size());
            log.info("Total calculado: R$ {}", String.format("%.2f", total));

            return ResponseEntity.ok(resposta);

        } catch (IOException e) {
            log.error("Erro ao processar arquivo PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("erro", "Falha ao processar o arquivo PDF"));
        }
    }
}