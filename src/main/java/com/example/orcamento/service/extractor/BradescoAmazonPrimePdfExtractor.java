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
public class BradescoAmazonPrimePdfExtractor implements PdfCartaoExtractor {
    @Override
    public List<Transaction> extrair(InputStream pdfInputStream) {
        List<Transaction> transacoes = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfInputStream)) {
            log.info("Documento PDF carregado : {} ", document.toString());
            PDFTextStripper stripper = new PDFTextStripper();
            String textoExtraido = stripper.getText(document);

            List<String> linhas = List.of(textoExtraido.split("\n"));
            log.info("linhas extraídas: {}", linhas.size());

            boolean inMovimentacoes = false;
            for (String linha : linhas) {
                String linhaLimpa = linha.trim().replaceAll("\s+", " ");
                if (linhaLimpa.startsWith("Movimentações da conta")) {
                    inMovimentacoes = true;
                    continue;
                }
                if (inMovimentacoes && linhaLimpa.startsWith("Resumo dos encargos financeiros")) {
                    break; // Fim da seção de movimentações
                }
                if (inMovimentacoes) {
                    Transaction t = parseTransactionBradesco(linhaLimpa);
                    if (t != null) {
                        transacoes.add(t);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Erro ao processar PDF", e);
        }
        log.info("==== TRANSAÇÕES ENCONTRADAS BRADESCO AMAZON PRIME ====");
        for (Transaction t : transacoes) {
            log.info("Data: {}, Estabelecimento: {}, Valor: {}, Parcela: {}", t.getDataCompra(), t.getEstabelecimento(), t.getValor(), t.getParcela());
        }
        log.info("====================================================");
        return transacoes;
    }

    private Transaction parseTransactionBradesco(String linha) {
        // Exemplo: 31/05 AMAZONMKTPLC*SPLINCOME SAO PAULO BRA 49,90 ou 11/06 AMAZONMKTPLC*COMERCIOC SAO PAULO(01/02) 39,50
        Pattern p = Pattern.compile("^(\\d{2}/\\d{2}) (.+) (\\d+,\\d{2})$");
        Matcher m = p.matcher(linha);
        if (m.find()) {
            String data = m.group(1);
            String estabelecimento = m.group(2).trim();
            String valor = m.group(3).replace(".", "").replace(",", ".");
            log.info("PARSE BRADESCO | linha: '{}' | data: '{}' | est: '{}' | valor: '{}'", linha, data, estabelecimento, valor);
            return new Transaction(data, estabelecimento, null, valor);
        }
        return null;
    }

    // Removido método getModeloImportacao() pois não faz parte da interface
}
