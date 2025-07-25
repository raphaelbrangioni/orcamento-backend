package com.example.orcamento.service;

import com.example.orcamento.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PdfService {

    // Método principal para extrair transações do PDF
    public static List<Transaction> extrairInformacoesDoPdf(InputStream pdfInputStream) throws IOException {
        List<Transaction> transacoes = new ArrayList<>();

        // Carregar o documento PDF
        try (PDDocument document = PDDocument.load(pdfInputStream)) {
            log.info("Documento PDF carregado : {} ", document.toString());
            PDFTextStripper stripper = new PDFTextStripper();
            String textoExtraido = stripper.getText(document);
            log.info("textoExtraido: {} ", textoExtraido);

            // Dividir o texto em linhas
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
        }

        return transacoes;
    }

    // Método para Cartão 2 (inicialmente igual ao padrão)
    public static List<Transaction> extrairInformacoesDoPdfCartao2(InputStream pdfInputStream) throws IOException {
        return extrairInformacoesDoPdf(pdfInputStream);
    }

    private static List<Transaction> processarRegistros(List<String> linhas) {
        List<Transaction> todasTransacoes = new ArrayList<>();

        for (String linha : linhas) {
           // log.info(linha);
            // Limpar a linha: remover espaços extras e normalizar
            String linhaLimpa = linha.trim().replaceAll("\\s+", " ");
           // log.info("Linha processada: [" + linhaLimpa + "]"); // Log para depuração

            // Verificar se a linha começa com uma data (ex.: "4/10" ou "04/10")
            if (linhaLimpa.matches("^\\d{1,2}/\\d{1,2}.*")) {
                Transaction transacao = parseTransaction(linhaLimpa);
                if (transacao != null) {
                    todasTransacoes.add(transacao);
                }
            }
        }

        // Agrupar por dataCompra, estabelecimento e valor normalizado e manter a transação com a menor parcela
        return filtrarPorMenorParcela(todasTransacoes);
    }

    // Parsear uma linha em uma transação
    private static Transaction parseTransaction(String linha) {
        // Dividir a linha em tokens
        String[] tokens = linha.split(" ");
        if (tokens.length < 3) return null; // Pelo menos data, estabelecimento e valor

        String data = tokens[0]; // Ex.: "23/08"
        String valor = "";
        String parcela = null;
        String estabelecimento = "";

        // Procurar o valor e a parcela a partir do final
        for (int i = tokens.length - 1; i >= 0; i--) {
            if (tokens[i].matches("\\d+,\\d{2}")) { // Ex.: "191,62"
                valor = tokens[i];
                // Verificar se é negativo
                if (i > 0 && tokens[i - 1].equals("-")) {
                    valor = "-" + valor;
                    i--;
                }
                // Procurar a parcela (dd/dd) antes do valor
                for (int j = i - 1; j > 0; j--) { // Começa antes do valor, após a data
                    if (tokens[j].matches("\\d{2}/\\d{2}")) { // Ex.: "07/12"
                        parcela = tokens[j];
                        // Estabelecimento é tudo entre a data e a parcela
                        estabelecimento = String.join(" ", Arrays.copyOfRange(tokens, 1, j));
                        break;
                    }
                }
                // Se não encontrou parcela, o estabelecimento é tudo entre a data e o valor
                if (parcela == null) {
                    // Buscar parcelas misturadas no nome (ex.: "PARC=112REDLAR HIP07/12")
                    String restoLinha = String.join(" ", Arrays.copyOfRange(tokens, 1, i));
                    parcela = extrairParcelaDeTexto(restoLinha);
                    if (parcela != null) {
                        // Remove a parcela do restante da linha após extraí-la
                        estabelecimento = restoLinha.replace(parcela, "").trim();
                    } else {
                        estabelecimento = restoLinha;
                    }
                }
                break;
            }
        }

        if (valor.isEmpty()) return null;

        log.info("Parsed: data=" + data + ", est=" + estabelecimento + ", parcela=" + parcela + ", valor=" + valor);

        return new Transaction(data, estabelecimento, parcela, valor);
    }

    private static String extrairParcelaDeTexto(String texto) {
        // Procurar pelo padrão de parcela (dd/dd)
        String padraoParcela = "\\d{2}/\\d{2}";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(padraoParcela).matcher(texto);
        if (matcher.find()) {
            return matcher.group(); // Retorna o primeiro match no texto
        }
        return null;
    }

    private static List<Transaction> filtrarPorMenorParcela(List<Transaction> transacoes) {
        // Separar transações com parcela nula e não nula
        List<Transaction> transacoesComParcelaNula = transacoes.stream()
                .filter(t -> t.getParcela() == null)
                .peek(t -> log.warn("Transação com parcela nula detectada: dataCompra={}, estabelecimento={}, valor={}",
                        t.getDataCompra(), t.getEstabelecimento(), t.getValor()))
                .collect(Collectors.toList());

        List<Transaction> transacoesComParcela = transacoes.stream()
                .filter(t -> t.getParcela() != null)
                .peek(t ->
                        log.info("Transação com parcela válida: parcela={}, dataCompra={}, estabelecimento={}, valor={}",
                        t.getParcela(), t.getDataCompra(), t.getEstabelecimento(), t.getValor()))
                .collect(Collectors.groupingBy(
                        t -> String.join("|",
                                t.getDataCompra() != null ? t.getDataCompra() : "",
                                t.getEstabelecimento() != null ? t.getEstabelecimento() : "",
                                normalizarValor(t.getValor())),
                        Collectors.toList()
                ))
                .values().stream()
                .map(grupo -> grupo.stream().min((t1, t2) -> {
                    //log.info("Comparando parcelas: {} e {}", t1.getParcela(), t2.getParcela());
                    int parcelaT1 = Integer.parseInt(t1.getParcela().split("/")[0]);
                    int parcelaT2 = Integer.parseInt(t2.getParcela().split("/")[0]);
                    return Integer.compare(parcelaT1, parcelaT2);
                }).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Combinar transações com parcela nula e com menor parcela
        List<Transaction> resultado = new ArrayList<>();
        resultado.addAll(transacoesComParcelaNula);
        resultado.addAll(transacoesComParcela);

       // log.info("Total de transações retornadas: {} ({} com parcela nula, {} com parcela válida)",
       //         resultado.size(), transacoesComParcelaNula.size(), transacoesComParcela.size());

        return resultado;
    }

    private static String normalizarValor(String valor) {
        if (valor == null) return "";
        try {
            // Substitui vírgula por ponto para conversão
            double valorDouble = Double.parseDouble(valor.replace(",", "."));
            // Trunca para a parte inteira
            long valorInteiro = (long) valorDouble;
            return String.valueOf(valorInteiro);
        } catch (NumberFormatException e) {
            log.warn("Valor inválido para normalização: {}", valor);
            return valor; // Retorna o valor original em caso de erro
        }
    }
}