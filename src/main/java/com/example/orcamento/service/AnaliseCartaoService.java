package com.example.orcamento.service;

import com.example.orcamento.dto.GastoRecorrenteCartaoDTO;
import com.example.orcamento.dto.PrevisaoGastoCartaoDTO;
import com.example.orcamento.dto.SugestaoEconomiaCartaoDTO;
import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.model.TipoDespesa;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnaliseCartaoService {

    @Autowired
    private CartaoCreditoRepository cartaoCreditoRepository;

    @Autowired
    private LancamentoCartaoRepository lancamentoCartaoRepository;

    public List<GastoRecorrenteCartaoDTO> getGastosRecorrentes() {
        // Buscar lançamentos de cartão dos últimos 6 meses
        LocalDate dataInicio = LocalDate.now().minusMonths(6);
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<LancamentoCartao> lancamentos = lancamentoCartaoRepository.findByDataCompraAfterAndTenantId(dataInicio, tenantId);

        // Agrupar por descrição para identificar gastos recorrentes
        Map<String, List<LancamentoCartao>> lancamentosPorDescricao = lancamentos.stream()
                .collect(Collectors.groupingBy(LancamentoCartao::getDescricao));

        List<GastoRecorrenteCartaoDTO> gastosRecorrentes = new ArrayList<>();

        for (Map.Entry<String, List<LancamentoCartao>> entry : lancamentosPorDescricao.entrySet()) {
            List<LancamentoCartao> lancamentosGrupo = entry.getValue();

            // Considerar recorrente se aparecer em pelo menos 3 meses diferentes
            Set<String> mesesAnoFatura = lancamentosGrupo.stream()
                    .map(LancamentoCartao::getMesAnoFatura)
                    .collect(Collectors.toSet());

            if (mesesAnoFatura.size() >= 3) {
                String descricao = entry.getKey();

                // Calcular valor médio
                BigDecimal valorTotal = lancamentosGrupo.stream()
                        .map(LancamentoCartao::getValorTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal valorMedio = valorTotal.divide(new BigDecimal(lancamentosGrupo.size()), 2, RoundingMode.HALF_UP);

                // Determinar frequência
                String frequencia = determinarFrequencia(mesesAnoFatura);

                // Obter categoria (se disponível)
                String categoria = "Não categorizado";
                if (lancamentosGrupo.get(0).getTipoDespesa() != null) {
                    categoria = lancamentosGrupo.get(0).getTipoDespesa().getNome();
                }

                // Obter cartão
                String cartao = lancamentosGrupo.get(0).getCartaoCredito().getNome();

                gastosRecorrentes.add(new GastoRecorrenteCartaoDTO(
                        (long) gastosRecorrentes.size() + 1,
                        descricao,
                        frequencia,
                        valorMedio,
                        categoria,
                        cartao
                ));
            }
        }

        // Se não houver dados suficientes, gerar exemplos
        if (gastosRecorrentes.isEmpty()) {
            return gerarGastosRecorrentesExemplo();
        }

        return gastosRecorrentes;
    }

    public List<SugestaoEconomiaCartaoDTO> getSugestoesEconomia() {
        // Buscar lançamentos recentes
        LocalDate dataInicio = LocalDate.now().minusMonths(3);
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<LancamentoCartao> lancamentos = lancamentoCartaoRepository.findByDataCompraAfterAndTenantId(dataInicio, tenantId);

        List<SugestaoEconomiaCartaoDTO> sugestoes = new ArrayList<>();

        // Analisar padrões e gerar sugestões

        // 1. Identificar cartões com muitos gastos em categorias não essenciais
        Map<CartaoCredito, List<LancamentoCartao>> lancamentosPorCartao = lancamentos.stream()
                .collect(Collectors.groupingBy(LancamentoCartao::getCartaoCredito));

        for (Map.Entry<CartaoCredito, List<LancamentoCartao>> entry : lancamentosPorCartao.entrySet()) {
            CartaoCredito cartao = entry.getKey();
            List<LancamentoCartao> lancamentosCartao = entry.getValue();

            // Exemplo: Sugerir consolidação de assinaturas se houver muitas
            long contAssinaturas = lancamentosCartao.stream()
                    .filter(l -> l.getTipoDespesa() != null &&
                            l.getTipoDespesa().getNome().toLowerCase().contains("assinatura"))
                    .count();

            if (contAssinaturas >= 3) {
                sugestoes.add(new SugestaoEconomiaCartaoDTO(
                        (long) sugestoes.size() + 1,
                        "Consolidar assinaturas",
                        "Você tem " + contAssinaturas + " assinaturas no cartão " + cartao.getNome() + ". Considere revisar e cancelar as menos utilizadas.",
                        new BigDecimal("30.00").multiply(new BigDecimal(contAssinaturas - 2)),
                        "Assinaturas",
                        cartao.getNome()
                ));
            }

            // Exemplo: Sugerir mudança para cartão com melhor programa de pontos
            if (lancamentosCartao.size() > 10 && !cartao.getNome().toLowerCase().contains("black")) {
                sugestoes.add(new SugestaoEconomiaCartaoDTO(
                        (long) sugestoes.size() + 1,
                        "Considere um cartão com programa de pontos",
                        "Você fez " + lancamentosCartao.size() + " compras no cartão " + cartao.getNome() +
                                " que não tem um bom programa de pontos. Considere transferir para um cartão com melhor programa.",
                        new BigDecimal("200.00"),
                        "Cartões",
                        cartao.getNome()
                ));
            }
            // Adicionar novas análises
            sugestoes.addAll(analisarParcelamentosSimultaneos(lancamentos));
            sugestoes.addAll(analisarAssinaturasNaoUtilizadas(lancamentos));
        }

        // Se não houver sugestões reais, gerar exemplos
        if (sugestoes.isEmpty()) {
            return gerarSugestoesEconomiaExemplo();
        }

        return sugestoes;
    }

    public List<PrevisaoGastoCartaoDTO> getPrevisoes() {
        // Buscar lançamentos dos últimos 6 meses
        LocalDate dataInicio = LocalDate.now().minusMonths(6);
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<LancamentoCartao> lancamentos = lancamentoCartaoRepository.findByDataCompraAfterAndTenantId(dataInicio, tenantId);

        // Agrupar por categoria e cartão
        Map<String, List<LancamentoCartao>> lancamentosPorCategoriaECartao = new HashMap<>();

        for (LancamentoCartao lancamento : lancamentos) {
            String categoria = "Não categorizado";
            if (lancamento.getTipoDespesa() != null) {
                categoria = lancamento.getTipoDespesa().getNome();
            }

            String chave = categoria + "|" + lancamento.getCartaoCredito().getNome();

            if (!lancamentosPorCategoriaECartao.containsKey(chave)) {
                lancamentosPorCategoriaECartao.put(chave, new ArrayList<>());
            }

            lancamentosPorCategoriaECartao.get(chave).add(lancamento);
        }

        List<PrevisaoGastoCartaoDTO> previsoes = new ArrayList<>();

        for (Map.Entry<String, List<LancamentoCartao>> entry : lancamentosPorCategoriaECartao.entrySet()) {
            String[] partes = entry.getKey().split("\\|");
            String categoria = partes[0];
            String cartao = partes[1];

            List<LancamentoCartao> lancamentosGrupo = entry.getValue();

            // Calcular tendência e valor previsto
            String tendencia = calcularTendencia(lancamentosGrupo);
            BigDecimal valorPrevisto = calcularValorPrevisto(lancamentosGrupo);
            double confianca = calcularConfianca(lancamentosGrupo);

            previsoes.add(new PrevisaoGastoCartaoDTO(
                    categoria,
                    tendencia,
                    valorPrevisto,
                    confianca,
                    cartao
            ));
        }

        // Se não houver previsões reais, gerar exemplos
        if (previsoes.isEmpty()) {
            return gerarPrevisoesExemplo();
        }

        return previsoes;
    }

    // Métodos auxiliares

    private String determinarFrequencia(Set<String> mesesAnoFatura) {
        // Lógica para determinar a frequência baseada nos meses em que o gasto aparece
        int quantidadeMeses = mesesAnoFatura.size();

        if (quantidadeMeses >= 6) {
            return "MONTHLY";
        } else if (quantidadeMeses >= 3) {
            return "BIMONTHLY";
        } else {
            return "QUARTERLY";
        }
    }

    private String calcularTendencia(List<LancamentoCartao> lancamentos) {
        // Ordenar por data
        lancamentos.sort(Comparator.comparing(LancamentoCartao::getDataCompra));

        // Dividir em dois períodos
        int meio = lancamentos.size() / 2;
        List<LancamentoCartao> primeiroPeriodo = lancamentos.subList(0, meio);
        List<LancamentoCartao> segundoPeriodo = lancamentos.subList(meio, lancamentos.size());

        // Verificar se os períodos estão vazios para evitar divisão por zero
        if (primeiroPeriodo.isEmpty() || segundoPeriodo.isEmpty()) {
            return "SEM DADOS SUFICIENTES";
        }

        // Calcular média de cada período
        BigDecimal mediaPrimeiro = primeiroPeriodo.stream()
                .map(LancamentoCartao::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(primeiroPeriodo.size()), 2, RoundingMode.HALF_UP);

        BigDecimal mediaSegundo = segundoPeriodo.stream()
                .map(LancamentoCartao::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(segundoPeriodo.size()), 2, RoundingMode.HALF_UP);

        // Determinar tendência
        int comparacao = mediaSegundo.compareTo(mediaPrimeiro);
        if (comparacao > 0) {
            return "AUMENTANDO";
        } else if (comparacao < 0) {
            return "DIMINUINDO";
        } else {
            return "ESTAVEL";
        }
    }

    private BigDecimal calcularValorPrevisto(List<LancamentoCartao> lancamentos) {
        // Calcular média dos últimos 3 lançamentos
        lancamentos.sort(Comparator.comparing(LancamentoCartao::getDataCompra).reversed());

        int tamanho = Math.min(3, lancamentos.size());
        List<LancamentoCartao> ultimosLancamentos = lancamentos.subList(0, tamanho);

        return ultimosLancamentos.stream()
                .map(LancamentoCartao::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(tamanho), 2, RoundingMode.HALF_UP);
    }

    private double calcularConfianca(List<LancamentoCartao> lancamentos) {
        // Quanto mais lançamentos e mais consistentes, maior a confiança
        int quantidade = lancamentos.size();

        // Calcular desvio padrão
        BigDecimal media = lancamentos.stream()
                .map(LancamentoCartao::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(quantidade), 2, RoundingMode.HALF_UP);

        BigDecimal somaQuadrados = lancamentos.stream()
                .map(l -> {
                    BigDecimal diff = l.getValorTotal().subtract(media);
                    return diff.multiply(diff);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variancia = somaQuadrados.divide(new BigDecimal(quantidade), 2, RoundingMode.HALF_UP);
        BigDecimal desvioPadrao = new BigDecimal(Math.sqrt(variancia.doubleValue()));

        // Calcular coeficiente de variação
        BigDecimal coefVar = desvioPadrao.divide(media, 4, RoundingMode.HALF_UP);

        // Converter para confiança (inversamente proporcional ao coeficiente de variação)
        double confiancaBase = 0.5 + (0.5 * Math.min(quantidade, 10) / 10.0);
        double confiancaAjustada = confiancaBase * (1 - Math.min(coefVar.doubleValue(), 1.0));

        return Math.max(0.5, Math.min(0.95, confiancaAjustada));
    }

    // Métodos para gerar dados de exemplo

    private List<GastoRecorrenteCartaoDTO> gerarGastosRecorrentesExemplo() {
        List<GastoRecorrenteCartaoDTO> exemplos = new ArrayList<>();

        exemplos.add(new GastoRecorrenteCartaoDTO(1L, "Netflix", "MONTHLY", new BigDecimal("55.90"), "Assinatura de streaming", "Mastercard Person Multiplo Black Pontos"));
        exemplos.add(new GastoRecorrenteCartaoDTO(2L, "Spotify", "MONTHLY", new BigDecimal("19.90"), "Assinatura de streaming", "Visa Personalité Infinite"));
        exemplos.add(new GastoRecorrenteCartaoDTO(3L, "Amazon Prime", "MONTHLY", new BigDecimal("14.90"), "Assinatura de streaming", "Latam Pass Itaú Black"));
        exemplos.add(new GastoRecorrenteCartaoDTO(4L, "Zaffari Supermercado", "MONTHLY", new BigDecimal("450.75"), "Mercado", "ZAFFARI CARD"));
        exemplos.add(new GastoRecorrenteCartaoDTO(5L, "Academia SmartFit", "MONTHLY", new BigDecimal("99.90"), "Academia", "Cartão Inter"));

        return exemplos;
    }

    private List<SugestaoEconomiaCartaoDTO> gerarSugestoesEconomiaExemplo() {
        List<SugestaoEconomiaCartaoDTO> exemplos = new ArrayList<>();

        exemplos.add(new SugestaoEconomiaCartaoDTO(1L, "Consolidar assinaturas de streaming",
                "Você tem 4 assinaturas de streaming. Considere usar planos familiares ou cancelar as menos utilizadas.",
                new BigDecimal("45.80"), "Assinatura de streaming", "Mastercard Person Multiplo Black Pontos"));

        exemplos.add(new SugestaoEconomiaCartaoDTO(2L, "Transferir compras de supermercado",
                "Suas compras no Zaffari somam R$ 450,75/mês. Considere usar o cartão Latam Pass para acumular mais milhas.",
                new BigDecimal("90.15"), "Mercado", "ZAFFARI CARD"));

        exemplos.add(new SugestaoEconomiaCartaoDTO(3L, "Renegociar plano de academia",
                "Você paga R$ 99,90/mês na SmartFit. Há planos promocionais disponíveis por R$ 79,90.",
                new BigDecimal("240.00"), "Academia", "Cartão Inter"));

        return exemplos;
    }

    private List<PrevisaoGastoCartaoDTO> gerarPrevisoesExemplo() {
        List<PrevisaoGastoCartaoDTO> exemplos = new ArrayList<>();

        exemplos.add(new PrevisaoGastoCartaoDTO("Assinatura de streaming", "AUMENTANDO", new BigDecimal("120.00"), 0.85, "Mastercard Person Multiplo Black Pontos"));
        exemplos.add(new PrevisaoGastoCartaoDTO("Mercado", "ESTAVEL", new BigDecimal("450.75"), 0.92, "ZAFFARI CARD"));
        exemplos.add(new PrevisaoGastoCartaoDTO("Vestuário", "DIMINUINDO", new BigDecimal("180.50"), 0.78, "Latam Pass Itaú Black"));
        exemplos.add(new PrevisaoGastoCartaoDTO("Restaurantes", "AUMENTANDO", new BigDecimal("350.25"), 0.80, "Visa Personalité Infinite"));

        return exemplos;
    }


    /**
     * Analisa parcelamentos simultâneos e sugere otimizações
     */
    private List<SugestaoEconomiaCartaoDTO> analisarParcelamentosSimultaneos(List<LancamentoCartao> lancamentos) {
        List<SugestaoEconomiaCartaoDTO> sugestoes = new ArrayList<>();

        // Filtrar apenas lançamentos parcelados (onde totalParcelas > 1)
        List<LancamentoCartao> lancamentosParcelados = lancamentos.stream()
                .filter(l -> l.getTotalParcelas() != null && l.getTotalParcelas() > 1)
                .collect(Collectors.toList());

        // Agrupar por cartão
        Map<CartaoCredito, List<LancamentoCartao>> parcelamentosPorCartao = lancamentosParcelados.stream()
                .collect(Collectors.groupingBy(LancamentoCartao::getCartaoCredito));

        for (Map.Entry<CartaoCredito, List<LancamentoCartao>> entry : parcelamentosPorCartao.entrySet()) {
            CartaoCredito cartao = entry.getKey();
            List<LancamentoCartao> parcelamentos = entry.getValue();

            // Contar parcelamentos ativos (onde parcelaAtual < totalParcelas)
            List<LancamentoCartao> parcelamentosAtivos = parcelamentos.stream()
                    .filter(p -> p.getParcelaAtual() < p.getTotalParcelas())
                    .collect(Collectors.toList());

            int quantidadeParcelamentos = parcelamentosAtivos.size();

            // Calcular valor total comprometido em parcelas futuras
            BigDecimal valorComprometido = parcelamentosAtivos.stream()
                    .map(p -> {
                        int parcelasRestantes = p.getTotalParcelas() - p.getParcelaAtual();
                        BigDecimal valorParcela = p.getValorTotal().divide(new BigDecimal(p.getTotalParcelas()), 2, RoundingMode.HALF_UP);
                        return valorParcela.multiply(new BigDecimal(parcelasRestantes));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Gerar sugestões baseadas na quantidade de parcelamentos e valor comprometido
            if (quantidadeParcelamentos >= 5) {
                sugestoes.add(new SugestaoEconomiaCartaoDTO(
                        (long) sugestoes.size() + 1,
                        "Reduza parcelamentos simultâneos",
                        "Você tem " + quantidadeParcelamentos + " compras parceladas ativas no cartão " + cartao.getNome() +
                                ", comprometendo R$ " + valorComprometido.setScale(2, RoundingMode.HALF_UP) +
                                " em parcelas futuras. Considere reduzir novas compras parceladas para melhorar seu fluxo de caixa.",
                        valorComprometido.multiply(new BigDecimal("0.05")), // Estimativa de economia: 5% do valor comprometido
                        "Parcelamentos",
                        cartao.getNome()
                ));
            }

            // Verificar se há muitos parcelamentos de longo prazo (mais de 6 parcelas)
            long parcelamentosLongos = parcelamentosAtivos.stream()
                    .filter(p -> p.getTotalParcelas() > 6)
                    .count();

            if (parcelamentosLongos >= 3) {
                sugestoes.add(new SugestaoEconomiaCartaoDTO(
                        (long) sugestoes.size() + 1,
                        "Evite parcelamentos longos",
                        "Você tem " + parcelamentosLongos + " compras com parcelamento longo (mais de 6 meses) no cartão " +
                                cartao.getNome() + ". Parcelamentos longos podem comprometer sua renda futura e dificultar o planejamento financeiro.",
                        new BigDecimal("100.00"), // Valor estimado de economia
                        "Parcelamentos",
                        cartao.getNome()
                ));
            }
        }

        return sugestoes;
    }


    /**
     * Analisa assinaturas potencialmente não utilizadas
     */
    private List<SugestaoEconomiaCartaoDTO> analisarAssinaturasNaoUtilizadas(List<LancamentoCartao> lancamentos) {
        List<SugestaoEconomiaCartaoDTO> sugestoes = new ArrayList<>();

        // Identificar possíveis assinaturas (lançamentos recorrentes com mesmo valor e descrição)
        Map<String, List<LancamentoCartao>> lancamentosPorDescricao = lancamentos.stream()
                .collect(Collectors.groupingBy(LancamentoCartao::getDescricao));

        // Lista de palavras-chave que sugerem assinaturas
        List<String> palavrasChaveAssinaturas = Arrays.asList(
                "netflix", "spotify", "amazon", "prime", "disney", "hbo", "youtube",
                "deezer", "apple", "music", "assinatura", "mensalidade", "signature",
                "subscription", "clube", "club", "premium", "pro", "plus"
        );

        for (Map.Entry<String, List<LancamentoCartao>> entry : lancamentosPorDescricao.entrySet()) {
            String descricao = entry.getKey().toLowerCase();
            List<LancamentoCartao> lancamentosGrupo = entry.getValue();

            // Verificar se a descrição contém palavras-chave de assinaturas
            boolean pareceAssinatura = palavrasChaveAssinaturas.stream()
                    .anyMatch(descricao::contains);

            // Verificar se os valores são consistentes (indicando assinatura)
            boolean valoresConsistentes = lancamentosGrupo.size() >= 3 &&
                    lancamentosGrupo.stream()
                            .map(LancamentoCartao::getValorTotal)
                            .distinct()
                            .count() <= 2; // Permite até 2 valores diferentes (caso haja reajuste)

            if (pareceAssinatura && valoresConsistentes) {
                // Obter o valor mais recente da assinatura
                LancamentoCartao lancamentoMaisRecente = lancamentosGrupo.stream()
                        .max(Comparator.comparing(LancamentoCartao::getDataCompra))
                        .orElse(lancamentosGrupo.get(0));

                BigDecimal valorAssinatura = lancamentoMaisRecente.getValorTotal();
                String cartao = lancamentoMaisRecente.getCartaoCredito().getNome();

                // Calcular economia anual cancelando a assinatura
                BigDecimal economiaAnual = valorAssinatura.multiply(new BigDecimal("12"));

                sugestoes.add(new SugestaoEconomiaCartaoDTO(
                        (long) sugestoes.size() + 1,
                        "Revise assinatura de " + entry.getKey(),
                        "Você paga R$ " + valorAssinatura.setScale(2, RoundingMode.HALF_UP) +
                                " mensalmente pela assinatura de " + entry.getKey() +
                                ". Verifique se você utiliza este serviço com frequência ou se poderia cancelar/fazer downgrade.",
                        economiaAnual,
                        "Assinaturas",
                        cartao
                ));
            }
        }

        return sugestoes;
    }
}