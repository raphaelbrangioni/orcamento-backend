package com.example.orcamento.service.extractor;

import com.example.orcamento.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PdfCartaoExtractorCartao1 implements PdfCartaoExtractor {
    @Override
    public List<Transaction> extrair(InputStream pdfInputStream) {
        List<Transaction> transacoes = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfInputStream)) {
            log.info("Documento PDF carregado : {} ", document.toString());
            PDFTextStripper stripper = new PDFTextStripper();
            String textoExtraido = stripper.getText(document);
            log.info("textoExtraido: {} ", textoExtraido);

            List<String> linhas = Arrays.asList(textoExtraido.split("\n"));
            log.info("linhas: {} ", linhas);
            boolean temString = false;
            if (linhas.contains("00 01290 VK045 28/03/2025")) {
                temString = true;
            }
            log.info("temString: {} ", temString);
            if (!temString) {
                transacoes = processarRegistros(linhas);
            }
        } catch (IOException e) {
            log.error("Erro ao processar PDF", e);
        }
        return transacoes;
    }

    private static List<Transaction> processarRegistros(List<String> linhas) {
        List<Transaction> todasTransacoes = new ArrayList<>();
        for (String linha : linhas) {
            String linhaLimpa = linha.trim().replaceAll("\\s+", " ");
            // Fim do bloco: próximas faturas
            if (linhaLimpa.toUpperCase().contains("COMPRAS PARCELADAS - PRÓXIMAS FATURAS")) {
                break;
            }
            if (linhaLimpa.matches("^\\d{1,2}/\\d{1,2}.*")) {
                Transaction transacao = parseTransaction(linhaLimpa);
                if (transacao != null) {
                    todasTransacoes.add(transacao);
                }
            }
        }
        // Agrupa por data, estabelecimento e valor, mantendo apenas a menor parcela
        List<Transaction> transacoesComParcelaNula = todasTransacoes.stream()
                .filter(t -> t.getParcela() == null)
                .collect(Collectors.toList());

        List<Transaction> transacoesComParcela = todasTransacoes.stream()
                .filter(t -> t.getParcela() != null)
                .collect(Collectors.groupingBy(
                        t -> String.join("|",
                                t.getDataCompra() != null ? t.getDataCompra() : "",
                                t.getEstabelecimento() != null ? t.getEstabelecimento() : "",
                                normalizarValor(t.getValor())),
                        Collectors.toList()
                ))
                .values().stream()
                .map(grupo -> grupo.stream().min((t1, t2) -> {
                    int parcelaT1 = Integer.parseInt(t1.getParcela().split("/")[0]);
                    int parcelaT2 = Integer.parseInt(t2.getParcela().split("/")[0]);
                    return Integer.compare(parcelaT1, parcelaT2);
                }).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Transaction> resultado = new ArrayList<>();
        resultado.addAll(transacoesComParcelaNula);
        resultado.addAll(transacoesComParcela);

        // LOG DETALHADO PARA DEBUG
        log.info("==== TRANSAÇÕES AGRUPADAS E CONSIDERADAS NA FATURA ====");
        for (Transaction t : resultado) {
            log.info("Data: {}, Estabelecimento: {}, Valor: {}, Parcela: {}", t.getDataCompra(), t.getEstabelecimento(), t.getValor(), t.getParcela());
        }
        log.info("======================================================");

        return resultado;
    }

    private static Transaction parseTransaction(String linha) {
        String[] tokens = linha.split(" ");
        if (tokens.length < 3) return null;
        String data = tokens[0];
        String valor = "";
        String parcela = null;
        String estabelecimento = "";

        // Novo regex para valor com ponto de milhar
        for (int i = tokens.length - 1; i >= 0; i--) {
            if (tokens[i].matches("\\d{1,3}(?:\\.\\d{3})*,\\d{2}")) { // Ex.: "1.265,53" ou "191,62"
                valor = tokens[i];
                // Verificar se é negativo
                if (i > 0 && tokens[i - 1].equals("-")) {
                    valor = "-" + valor;
                    i--;
                }
                // Procurar parcela
                for (int j = i - 1; j > 0; j--) {
                    if (tokens[j].matches("\\d{2}/\\d{2}")) {
                        parcela = tokens[j];
                        estabelecimento = String.join(" ", Arrays.copyOfRange(tokens, 1, j));
                        break;
                    }
                }
                // Se não encontrou parcela, o estabelecimento é tudo entre a data e o valor
                if (parcela == null) {
                    String restoLinha = String.join(" ", Arrays.copyOfRange(tokens, 1, i));
                    parcela = extrairParcelaDeTexto(restoLinha);
                    if (parcela != null) {
                        estabelecimento = restoLinha.replace(parcela, "").trim();
                    } else {
                        estabelecimento = restoLinha;
                    }
                }
                break;
            }
        }
        if (valor.isEmpty()) return null;
        log.info("PARSE DEBUG | linha: '{}' | data: '{}' | est: '{}' | parcela: '{}' | valor: '{}'", linha, data, estabelecimento, parcela, valor);
        return new Transaction(data, estabelecimento, parcela, normalizarValor(valor));
    }

    private static String extrairParcelaDeTexto(String texto) {
        String padraoParcela = "\\d{2}/\\d{2}";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(padraoParcela).matcher(texto);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private static String normalizarValor(String valor) {
        if (valor == null) return null;
        // Remove pontos de milhar e troca vírgula decimal por ponto
        String normalizado = valor.replace(".", "").replace(",", ".");
        return normalizado;
    }
}
