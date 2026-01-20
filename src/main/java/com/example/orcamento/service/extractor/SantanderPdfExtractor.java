package com.example.orcamento.service.extractor;

import com.example.orcamento.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SantanderPdfExtractor implements PdfCartaoExtractor {

    private static final Pattern TRANSACTION_PATTERN = Pattern.compile(
            "^(\\d{2}/\\d{2})\\s+(.+?)\\s+((?:\\d{2}/\\d{2})\\s+)?([\\d,.]+)"
    );

    @Override
    public List<Transaction> extrair(InputStream pdfInputStream) {
        List<Transaction> transacoes = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfInputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String textoExtraido = stripper.getText(document);
            String[] linhas = textoExtraido.split("\\r?\\n");

            boolean inTransactionSection = false;

            for (String linha : linhas) {
                String linhaLimpa = linha.trim().replaceAll("\\s+", " ");

                if (linhaLimpa.contains("Detalhamento da Fatura")) {
                    inTransactionSection = true;
                    continue;
                }

                if (linhaLimpa.contains("Resumo da Fatura")) {
                    inTransactionSection = false;
                    break; 
                }

                if (inTransactionSection) {
                    Transaction t = parseTransaction(linhaLimpa);
                    if (t != null) {
                        transacoes.add(t);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Erro ao processar PDF do Santander", e);
        }
        log.info("==== TRANSAÇÕES ENCONTRADAS SANTANDER ====");
        for (Transaction t : transacoes) {
            log.info("Data: {}, Estabelecimento: {}, Valor: {}, Parcela: {}", t.getDataCompra(), t.getEstabelecimento(), t.getValor(), t.getParcela());
        }
        log.info("========================================");
        return transacoes;
    }

    private Transaction parseTransaction(String linha) {
        // Formatos esperados:
        // 25/07 ANUIDADE DIFERENCIADA 01/12 113,33
        // 19/06 MP*LOTERIASONLINELKSA 37,00
        // @ 22/06 PG *A I H K COMER 01/05 115,58
        Pattern p = Pattern.compile("^(?:.*?)?(\\d{2}/\\d{2})\\s+(.*?)(?:\\s+(\\d{2}/\\d{2}))?\\s+([\\d.,]+)$");
        Matcher m = p.matcher(linha);

        if (m.find()) {
            String data = m.group(1);
            String estabelecimento = m.group(2).trim();
            String parcela = m.group(3);
            String valorStr = m.group(4).replace(".", "").replace(",", ".");

            log.info("PARSE SANTANDER | linha: '{}' | data: '{}' | est: '{}' | parcela: '{}' | valor: '{}'", linha, data, estabelecimento, parcela, valorStr);
            return new Transaction(data, estabelecimento, parcela, valorStr);
        }
        return null;
    }
}
