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
public class ZaffariCardPdfExtractor implements PdfCartaoExtractor {

    @Override
    public List<Transaction> extrair(InputStream pdfInputStream) {
        List<Transaction> transacoes = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfInputStream)) {
            log.info("Carregando documento PDF do Zaffari Card.");
            PDFTextStripper stripper = new PDFTextStripper();
            String textoExtraido = stripper.getText(document);

            List<String> linhas = List.of(textoExtraido.split("\r?\n"));
            log.info("Linhas extraídas do PDF Zaffari: {}", linhas.size());

            boolean inMovimentacoes = false;
            for (String linha : linhas) {
                String linhaLimpa = linha.trim().replaceAll("\s+", " ");

                if (linhaLimpa.contains("TRANSAÇÕES REALIZADAS PELO TITULAR")) {
                    inMovimentacoes = true;
                    continue;
                }

                if (inMovimentacoes) {
                    // A seção de transações termina antes dos totais ou outras seções
                    if (linhaLimpa.matches("^\\d{1,3}(?:\\.\\d{3})*,\\d{2}")) { // Linha de total
                        break;
                    }

                    log.info("[DEBUG ZAFFARI] Processando linha: '{}'", linhaLimpa);
                    Transaction t = parseTransactionZaffari(linhaLimpa);
                    if (t != null) {
                        transacoes.add(t);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Erro ao processar o PDF do Zaffari Card.", e);
        }

        log.info("==== TRANSAÇÕES ENCONTRADAS ZAFFARI CARD ====");
        transacoes.forEach(t -> 
            log.info("Data: {}, Estabelecimento: {}, Valor: {}", t.getDataCompra(), t.getEstabelecimento(), t.getValor())
        );
        log.info("==============================================");

        return transacoes;
    }

    private Transaction parseTransactionZaffari(String linha) {
        // Regex para encontrar a data no início e o valor em qualquer lugar.
        Pattern pattern = Pattern.compile("^(\\d{2}/\\d{2})|(\\d{1,3}(?:\\.\\d{3})*,\\d{2})");
        Matcher matcher = pattern.matcher(linha);

        String data = null;
        String valor = null;
        List<String> matches = new ArrayList<>();

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                data = matcher.group(1);
            } else if (matcher.group(2) != null) {
                valor = matcher.group(2);
                matches.add(valor);
            }
        }

        if (data != null && valor != null) {
            // Remove a data e o valor para isolar o texto do estabelecimento/descrição.
            String textoRestante = linha.replace(data, "")
                                        .replace(valor, "")
                                        .trim();

            // Tenta separar o Histórico (geralmente em maiúsculas) da Descrição.
            Pattern historicoPattern = Pattern.compile("^([A-ZÀ-Ú0-9\\s\\.]*?)([A-ZÀ-Ú][a-zà-ú].*)?$");
            Matcher historicoMatcher = historicoPattern.matcher(textoRestante);

            String estabelecimento;
            if (historicoMatcher.find()) {
                String historico = historicoMatcher.group(1).trim();
                String descricao = (historicoMatcher.group(2) != null) ? historicoMatcher.group(2).trim() : "";

                // Se o histórico (ex: ZAFFARI) existir, use-o. Senão, use a descrição (ex: Juros de Mora).
                if (!historico.isEmpty()) {
                    estabelecimento = historico;
                } else {
                    estabelecimento = descricao;
                }
            } else {
                estabelecimento = textoRestante; // Fallback
            }

            String valorFinalStr = valor.replace(".", "").replace(",", ".");

            // Pagamentos são créditos (valor negativo no contexto de despesa)
            if (estabelecimento.equalsIgnoreCase("Pagamento") || textoRestante.toLowerCase().contains("pagamento")) {
                valorFinalStr = "-" + valorFinalStr;
            }

            log.info("PARSE ZAFFARI | linha: '{}' | data: '{}' | est: '{}' | valor: '{}'", linha, data, estabelecimento, valorFinalStr);
            return new Transaction(data, estabelecimento, null, valorFinalStr);
        }
        return null;
    }
}
