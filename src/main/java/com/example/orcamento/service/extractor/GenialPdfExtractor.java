package com.example.orcamento.service.extractor;

import com.example.orcamento.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class GenialPdfExtractor implements PdfCartaoExtractor {

    private static final Pattern VALOR_PATTERN = Pattern.compile("\\d{1,3}(?:\\.\\d{3})*,\\d{2}");
    private static final Pattern PARCELA_PATTERN = Pattern.compile("\\b\\d{2}/\\d{2}\\b");
    // Ex.: Mercado*globaldistrib - 1/12 29/10/2025 R$ 606,21 (sem âncoras para permitir múltiplas ocorrências na mesma linha)
    private static final Pattern GENIAL_EST_PAR_DATA_VALOR = Pattern.compile(
            "(?<est>.+?)\\s*-\\s*(?<parc>\\d{1,2}/\\d{1,2})\\s+(?<data>\\d{2}/\\d{2}/\\d{4})\\s+R\\$\\s*(?<valor>\\d{1,3}(?:\\.\\d{3})*,\\d{2})"
    );

    @Override
    public List<Transaction> extrair(InputStream pdfInputStream) {
        List<Transaction> transacoes = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfInputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String textoExtraido = stripper.getText(document);
            List<String> linhas = Arrays.asList(textoExtraido.split("\n"));

            for (String linha : linhas) {
                String linhaLimpa = linha.trim().replaceAll("\\s+", " ");
                if (linhaLimpa.isEmpty()) continue;
                String upper = linhaLimpa.toUpperCase();

                // Primeiro: capturar todas as transações no formato principal, mesmo que estejam na mesma linha do cabeçalho
                boolean encontrouNestaLinha = false;
                Matcher multi = GENIAL_EST_PAR_DATA_VALOR.matcher(linhaLimpa);
                while (multi.find()) {
                    String estabelecimento = multi.group("est").trim();
                    String parcela = normalizarParcela(multi.group("parc"));
                    String data = multi.group("data");
                    String valor = normalizarValor(multi.group("valor"));
                    if ((upper.contains("PAGAMENTO") || upper.contains("CRÉDITO") || upper.contains("CREDITO")) && !valor.startsWith("-")) {
                        valor = "-" + valor;
                    }
                    transacoes.add(new Transaction(data, estabelecimento, parcela, valor));
                    encontrouNestaLinha = true;
                }
                if (encontrouNestaLinha) {
                    continue; // já extraímos todas as ocorrências dessa linha
                }

                // Ignorar pagamentos/créditos e totais para os demais formatos
                if (upper.contains("PAGAMENTO") || upper.contains("CRÉDITO") || upper.contains("CREDITO")
                        || upper.contains("TOTAL DA FATURA") || upper.contains("RESUMO") || upper.contains("ENCARGOS")) {
                    // Ainda assim tentamos identificar como crédito (valor negativo) se aparecer no padrão de transação
                    Transaction t = parseLinha(linhaLimpa, true);
                    if (t != null) {
                        // força negativo para pagamento/crédito
                        if (!t.getValor().startsWith("-")) {
                            t.setValor("-" + t.getValor());
                        }
                        transacoes.add(t);
                    }
                    continue;
                }

                Transaction t = parseLinha(linhaLimpa, false);
                if (t != null) {
                    transacoes.add(t);
                }
            }
        } catch (IOException e) {
            log.error("Erro ao processar PDF do Genial", e);
        }
        log.info("==== TRANSAÇÕES ENCONTRADAS GENIAL ====");
        for (Transaction t : transacoes) {
            log.info("Data: {}, Estabelecimento: {}, Valor: {}, Parcela: {}", t.getDataCompra(), t.getEstabelecimento(), t.getValor(), t.getParcela());
        }
        log.info("======================================");
        return transacoes;
    }

    private Transaction parseLinha(String linha, boolean forcarNegativo) {
        // Padrão principal do Genial: <Estabelecimento> - 1/12 29/10/2025 R$ 606,21
        Matcher mg = GENIAL_EST_PAR_DATA_VALOR.matcher(linha);
        if (mg.find()) {
            String estabelecimento = mg.group("est").trim();
            String parcela = normalizarParcela(mg.group("parc"));
            String data = mg.group("data");
            String valor = normalizarValor(mg.group("valor"));
            if (forcarNegativo && !valor.startsWith("-")) valor = "-" + valor;
            log.debug("GENIAL est-par-data-valor | linha='{}' | data='{}' | est='{}' | parcela='{}' | valor='{}'", linha, data, estabelecimento, parcela, valor);
            return new Transaction(data, estabelecimento, parcela, valor);
        }
        // Tenta formato 1: começa com dd/MM
        if (linha.matches("^\\d{2}/\\d{2}.*")) {
            return parseFormatoDDMM(linha, forcarNegativo);
        }
        // Tenta formato 2: "dd de Mês. yyyy" (ex: 05 de Julho. 2025)
        if (linha.matches("^\\d{2} de [A-Za-zÀ-ÿ]+\\.? \\d{4}.*")) {
            return parseFormatoExtenso(linha, forcarNegativo);
        }
        return null;
    }

    private Transaction parseFormatoDDMM(String linha, boolean forcarNegativo) {
        String[] tokens = linha.split(" ");
        if (tokens.length < 3) return null;
        String data = tokens[0];
        int valorIdx = -1;
        String valor = null;
        for (int i = tokens.length - 1; i >= 0; i--) {
            if (VALOR_PATTERN.matcher(tokens[i]).matches()) {
                valorIdx = i;
                valor = tokens[i];
                // Verifica sinal imediatamente antes
                if (i > 0 && tokens[i - 1].equals("-")) {
                    valor = "-" + valor;
                }
                break;
            }
        }
        if (valorIdx == -1) return null;
        String trechoAntesValor = String.join(" ", Arrays.copyOfRange(tokens, 1, valorIdx)).trim();
        String parcela = extrairParcela(trechoAntesValor);
        String estabelecimento = trechoAntesValor;
        if (parcela != null) {
            estabelecimento = trechoAntesValor.replace(parcela, "").trim();
        }
        String valorNormalizado = normalizarValor(valor);
        if (forcarNegativo && !valorNormalizado.startsWith("-")) {
            valorNormalizado = "-" + valorNormalizado;
        }
        log.debug("GENIAL dd/MM | linha='{}' | data='{}' | est='{}' | parcela='{}' | valor='{}'", linha, data, estabelecimento, parcela, valorNormalizado);
        return new Transaction(data, estabelecimento, parcela, valorNormalizado);
    }

    private Transaction parseFormatoExtenso(String linha, boolean forcarNegativo) {
        // Ex: 12 de Junho. 2025 LOJA XYZ (Parcela 01 de 05) - R$ 123,45
        Pattern pComValor = Pattern.compile("^(\\d{2} de [A-Za-zÀ-ÿ]+\\.? \\d{4}) (.+?)(?: \\((?:Parcela )?(\\d{2}/\\d{2}|\\d{2} de \\d{2})\\))? ?-? ?R\\$ ([\\d.,-]+)");
        Matcher m = pComValor.matcher(linha);
        if (m.find()) {
            String data = m.group(1);
            String estabelecimento = m.group(2).trim();
            String parcelaRaw = m.group(3);
            String parcela = normalizarParcela(parcelaRaw);
            String valorRaw = m.group(4).trim();
            String valor = normalizarValor(valorRaw);
            if (forcarNegativo && !valor.startsWith("-")) valor = "-" + valor;
            log.debug("GENIAL extenso | linha='{}' | data='{}' | est='{}' | parcela='{}' | valor='{}'", linha, data, estabelecimento, parcela, valor);
            return new Transaction(data, estabelecimento, parcela, valor);
        }
        // Fallback: procurar valor em linhas próximas exigiria contexto; aqui retornamos null
        return null;
    }

    private String extrairParcela(String texto) {
        Matcher m = PARCELA_PATTERN.matcher(texto);
        if (m.find()) return m.group();
        // Também aceitar padrão "Parcela 01 de 05"
        Matcher alt = Pattern.compile("Parcela \\d{2} de \\d{2}").matcher(texto);
        if (alt.find()) {
            String[] partes = alt.group().replace("Parcela ", "").split(" de ");
            if (partes.length == 2) return String.format("%s/%s", partes[0], partes[1]);
        }
        return null;
    }

    private String normalizarParcela(String parcelaRaw) {
        if (parcelaRaw == null) return null;
        if (parcelaRaw.contains(" de ")) {
            String[] partes = parcelaRaw.split(" de ");
            if (partes.length == 2) return String.format("%s/%s", partes[0], partes[1]);
        }
        return parcelaRaw;
    }

    private String normalizarValor(String valor) {
        if (valor == null) return null;
        String v = valor.replace(".", "").replace(",", ".").trim();
        // Remover múltiplos sinais e manter apenas um negativo se existir
        if (v.startsWith("--")) v = v.substring(1);
        return v;
    }
}
