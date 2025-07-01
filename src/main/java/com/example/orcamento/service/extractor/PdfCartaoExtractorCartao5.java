package com.example.orcamento.service.extractor;

import com.example.orcamento.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class PdfCartaoExtractorCartao5 implements PdfCartaoExtractor {
    @Override
    public List<Transaction> extrair(InputStream pdfInputStream) {
        List<Transaction> transacoes = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfInputStream)) {
            log.info("Documento PDF carregado : {} ", document.toString());
            PDFTextStripper stripper = new PDFTextStripper();
            String textoExtraido = stripper.getText(document);
           // log.info("textoExtraido: {} ", textoExtraido);

            List<String> linhas = Arrays.asList(textoExtraido.split("\n"));
            log.info("linhas: {} ", linhas);

            // Regex para linha padrão (com valor)
            Pattern padraoComValor = Pattern.compile("^(\\d{2} de \\w+\\. \\d{4}) (.+?)( \\(Parcela \\d{2} de \\d{2}\\))? ?- R\\$ ([\\d.,-]+)");
            // Regex para linha multi-linha (sem valor)
            Pattern padraoSemValor = Pattern.compile("^(\\d{2} de \\w+\\. \\d{4}) (.+?)( \\(Parcela \\d{2} de \\d{2}\\))?$");
            boolean[] valorUsado = new boolean[linhas.size()];
            for (int i = 0; i < linhas.size(); i++) {
                String linha = linhas.get(i).trim();
                // Ignorar pagamentos/créditos
                if (linha.contains("PAGAMENTO") || linha.contains("+ R$") || linha.contains("VALOR ANTECIPADO")) continue;
                // Ignorar linhas secundárias de transação multi-linha
                if (linha.startsWith("Principal") || linha.startsWith("Juros")) continue;
                Matcher matcherComValor = padraoComValor.matcher(linha);
                if (matcherComValor.find()) {
                    String data = matcherComValor.group(1);
                    String estabelecimento = matcherComValor.group(2).trim();
                    String parcela = matcherComValor.group(3) != null ? matcherComValor.group(3).replace("(Parcela ", "").replace(")", "").trim() : null;
                    String valor = matcherComValor.group(4).replace("-", "").trim();
                    valor = valor.replace(".", "").replace(",", ".");
                    Transaction t = new Transaction(data, estabelecimento, parcela, valor);
                    transacoes.add(t);
                    continue;
                }
                // Caso não tenha valor na linha, tente casar multi-linha
                Matcher matcherSemValor = padraoSemValor.matcher(linha);
                if (matcherSemValor.find()) {
                    String data = matcherSemValor.group(1);
                    String estabelecimento = matcherSemValor.group(2).trim();
                    String parcela = matcherSemValor.group(3) != null ? matcherSemValor.group(3).replace("(Parcela ", "").replace(")", "").trim() : null;
                    String valor = "";
                    int valorIdx = -1;
                    for (int j = 1; j <= 2 && i + j < linhas.size(); j++) {
                        String prox = linhas.get(i + j).trim();
                        if ((prox.startsWith("- R$") || prox.startsWith("-R$")) && !valorUsado[i + j]) {
                            valor = prox.replace("- R$", "").replace("-R$", "").replace("-", "").trim();
                            valor = valor.replace(".", "").replace(",", ".");
                            valorIdx = i + j;
                            break;
                        }
                    }
                    if (!valor.isEmpty()) {
                        if (valorIdx != -1) valorUsado[valorIdx] = true;
                        Transaction t = new Transaction(data, estabelecimento, parcela, valor);
                        transacoes.add(t);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Erro ao processar PDF", e);
        }
        log.info("==== TRANSAÇÕES ENCONTRADAS NO CARTÃO 5 ====");
        for (Transaction t : transacoes) {
            log.info("Data: {}, Estabelecimento: {}, Valor: {}, Parcela: {}", t.getDataCompra(), t.getEstabelecimento(), t.getValor(), t.getParcela());
        }
        log.info("================================================");
        return transacoes;
    }
}
