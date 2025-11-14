package com.example.orcamento.service;

import com.example.orcamento.dto.GastoRecorrenteDTO;
import com.example.orcamento.dto.PrevisaoGastoDTO;
import com.example.orcamento.dto.SugestaoEconomiaDTO;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.repository.DespesaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class AnaliseFinanceiraService {

    @Autowired
    private DespesaRepository despesaRepository;

    public List<GastoRecorrenteDTO> analisarGastosRecorrentes(Integer periodo, Long categoriaId) {
        // Define o período de análise, com 6 meses como padrão
        LocalDate dataInicial = LocalDate.now().minusMonths(periodo != null ? periodo : 6);
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<Despesa> despesas = despesaRepository.findDespesasParaAnalise(tenantId, dataInicial);

        // Filtra por categoria, se especificado
        if (categoriaId != null) {
            despesas = despesas.stream()
                    .filter(d -> d.getSubcategoria() != null && d.getSubcategoria().getCategoria() != null &&
                            categoriaId.equals(d.getSubcategoria().getCategoria().getId()))
                    .collect(Collectors.toList());
        }

        // Agrupa por nome e subcategoria
        Map<String, List<Despesa>> despesasAgrupadas = despesas.stream()
                .collect(Collectors.groupingBy(d -> d.getNome() + "#" + (d.getSubcategoria() != null ? d.getSubcategoria().getNome() : "SEM_SUBCATEGORIA")));

        List<GastoRecorrenteDTO> gastosRecorrentes = new ArrayList<>();

        despesasAgrupadas.forEach((key, lista) -> {
            if (lista.size() >= 3) { // Considera recorrente se aparecer em pelo menos 3 meses
                String[] partes = key.split("#");
                String nome = partes[0];
                String categoria = partes[1];

                // Calcula média e frequência
                BigDecimal valorMedio = lista.stream()
                        .map(Despesa::getValorPrevisto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(lista.size()), 2, RoundingMode.HALF_UP);

                List<BigDecimal> valores = lista.stream()
                        .map(Despesa::getValorPrevisto)
                        .collect(Collectors.toList());

                String frequencia = determinarFrequencia(lista);

                GastoRecorrenteDTO dto = new GastoRecorrenteDTO();
                dto.setId(lista.get(0).getId());
                dto.setNome(nome);
                dto.setCategoria(categoria);
                dto.setValorMedio(valorMedio);
                dto.setValores(valores);
                dto.setFrequencia(frequencia);

                gastosRecorrentes.add(dto);
            }
        });

        return gastosRecorrentes;
    }

    public List<SugestaoEconomiaDTO> gerarSugestoes(Integer periodo, Long categoriaId) {
        List<SugestaoEconomiaDTO> sugestoes = new ArrayList<>();
        AtomicLong idCounter = new AtomicLong(1);

        // Analisa variações por categoria, com período padrão de 3 meses
        LocalDate dataInicial = LocalDate.now().minusMonths(periodo != null ? periodo : 3);
        LocalDate dataFinal = LocalDate.now();
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<Despesa> despesas = despesaRepository.findByDataVencimentoBetween(tenantId, dataInicial, dataFinal);

        // Filtra por categoria, se especificado
        if (categoriaId != null) {
            despesas = despesas.stream()
                    .filter(d -> d.getSubcategoria() != null && d.getSubcategoria().getCategoria() != null &&
                            categoriaId.equals(d.getSubcategoria().getCategoria().getId()))
                    .collect(Collectors.toList());
        }

        Map<String, List<Despesa>> despesasPorCategoria = despesas.stream()
                .collect(Collectors.groupingBy(d -> d.getSubcategoria() != null ? d.getSubcategoria().getNome() : "SEM_SUBCATEGORIA"));

        // Calcula total geral para referência
        BigDecimal totalGeral = despesas.stream()
                .map(Despesa::getValorPrevisto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        despesasPorCategoria.forEach((categoria, lista) -> {
            BigDecimal totalCategoria = lista.stream()
                    .map(Despesa::getValorPrevisto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calcula variação percentual mensal
            Map<YearMonth, BigDecimal> totalPorMes = lista.stream()
                    .collect(Collectors.groupingBy(
                            d -> YearMonth.from(d.getDataVencimento()),
                            Collectors.reducing(BigDecimal.ZERO, Despesa::getValorPrevisto, BigDecimal::add)
                    ));

            if (totalPorMes.size() >= 2) {
                List<YearMonth> meses = new ArrayList<>(totalPorMes.keySet());
                Collections.sort(meses);

                YearMonth mesAtual = meses.get(meses.size() - 1);
                YearMonth mesAnterior = meses.get(meses.size() - 2);

                BigDecimal valorAtual = totalPorMes.get(mesAtual);
                BigDecimal valorAnterior = totalPorMes.get(mesAnterior);

                if (valorAnterior.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal variacao = valorAtual.subtract(valorAnterior)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(valorAnterior, 2, RoundingMode.HALF_UP);

                    // 1. Se houve aumento significativo (mais de 20%)
                    if (variacao.compareTo(BigDecimal.valueOf(20)) > 0) {
                        SugestaoEconomiaDTO sugestao = new SugestaoEconomiaDTO();
                        sugestao.setId(idCounter.getAndIncrement());
                        sugestao.setCategoria(categoria);
                        sugestao.setTitulo("Aumento Significativo em " + categoria);
                        sugestao.setDescricao(String.format(
                                "Seus gastos com %s aumentaram %.1f%% no último mês. " +
                                        "Considere revisar estes gastos para identificar oportunidades de redução.",
                                categoria.toLowerCase(), variacao.doubleValue()
                        ));
                        sugestao.setEconomiaPotencial(
                                valorAtual.subtract(valorAnterior).multiply(BigDecimal.valueOf(0.7))
                        );
                        sugestoes.add(sugestao);
                    }
                }

                // 2. Se a categoria representa uma parte significativa do total (mais de 30%)
                BigDecimal percentualDoTotal = totalCategoria
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalGeral, 2, RoundingMode.HALF_UP);

                if (percentualDoTotal.compareTo(BigDecimal.valueOf(30)) > 0) {
                    SugestaoEconomiaDTO sugestao = new SugestaoEconomiaDTO();
                    sugestao.setId(idCounter.getAndIncrement());
                    sugestao.setCategoria(categoria);
                    sugestao.setTitulo("Alto Impacto: " + categoria);
                    sugestao.setDescricao(String.format(
                            "A categoria %s representa %.1f%% do seu gasto total. " +
                                    "Foque em reduzir estes gastos para um impacto significativo no orçamento.",
                            categoria.toLowerCase(), percentualDoTotal.doubleValue()
                    ));
                    sugestao.setEconomiaPotencial(
                            totalCategoria.multiply(BigDecimal.valueOf(0.2)) // Potencial de 20% de economia
                    );
                    sugestoes.add(sugestao);
                }

                // 3. Se há muitas transações na categoria (mais de 5 por mês)
                double transacoesPorMes = (double) lista.size() / totalPorMes.size();
                if (transacoesPorMes > 5) {
                    SugestaoEconomiaDTO sugestao = new SugestaoEconomiaDTO();
                    sugestao.setId(idCounter.getAndIncrement());
                    sugestao.setCategoria(categoria);
                    sugestao.setTitulo("Frequência Alta: " + categoria);
                    sugestao.setDescricao(String.format(
                            "Você tem em média %.1f transações por mês em %s. " +
                                    "Considere consolidar compras ou buscar alternativas mais econômicas.",
                            transacoesPorMes, categoria.toLowerCase()
                    ));
                    sugestao.setEconomiaPotencial(
                            totalCategoria.multiply(BigDecimal.valueOf(0.15)) // Potencial de 15% de economia
                    );
                    sugestoes.add(sugestao);
                }
            }
        });

        // Se não houver sugestões específicas, adiciona dicas gerais
        if (sugestoes.isEmpty()) {
            SugestaoEconomiaDTO dicaGeral = new SugestaoEconomiaDTO();
            dicaGeral.setId(idCounter.getAndIncrement());
            dicaGeral.setCategoria("Geral");
            dicaGeral.setTitulo("Dica de Economia");
            dicaGeral.setDescricao(
                    "Seus gastos estão estáveis, mas você ainda pode economizar: \n" +
                            "1. Revise serviços por assinatura\n" +
                            "2. Compare preços antes de comprar\n" +
                            "3. Planeje compras maiores\n" +
                            "4. Considere alternativas mais econômicas"
            );
            dicaGeral.setEconomiaPotencial(totalGeral.multiply(BigDecimal.valueOf(0.1)));
            sugestoes.add(dicaGeral);
        }

        return sugestoes;
    }

    public List<PrevisaoGastoDTO> gerarPrevisoes(Integer periodo, Long categoriaId) {
        List<PrevisaoGastoDTO> previsoes = new ArrayList<>();

        // Analisa últimos meses para fazer previsão, com período padrão de 3 meses
        LocalDate dataInicial = LocalDate.now().minusMonths(periodo != null ? periodo : 3);
        LocalDate dataFinal = LocalDate.now();
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<Despesa> despesas = despesaRepository.findByDataVencimentoBetween(tenantId, dataInicial, dataFinal);

        // Filtra por categoria, se especificado
        if (categoriaId != null) {
            despesas = despesas.stream()
                    .filter(d -> d.getSubcategoria() != null && d.getSubcategoria().getCategoria() != null &&
                            categoriaId.equals(d.getSubcategoria().getCategoria().getId()))
                    .collect(Collectors.toList());
        }

        Map<String, List<Despesa>> despesasPorCategoria = despesas.stream()
                .collect(Collectors.groupingBy(d -> d.getSubcategoria() != null ? d.getSubcategoria().getNome() : "SEM_SUBCATEGORIA"));

        despesasPorCategoria.forEach((categoria, lista) -> {
            Map<YearMonth, BigDecimal> totalPorMes = lista.stream()
                    .collect(Collectors.groupingBy(
                            d -> YearMonth.from(d.getDataVencimento()),
                            Collectors.reducing(BigDecimal.ZERO, Despesa::getValorPrevisto, BigDecimal::add)
                    ));

            if (totalPorMes.size() >= 2) {
                List<YearMonth> meses = new ArrayList<>(totalPorMes.keySet());
                Collections.sort(meses);

                // Calcula tendência linear simples
                double[] valores = totalPorMes.values().stream()
                        .mapToDouble(BigDecimal::doubleValue)
                        .toArray();

                double tendencia = calcularTendencia(valores);
                BigDecimal ultimoValor = totalPorMes.get(meses.get(meses.size() - 1));

                PrevisaoGastoDTO previsao = new PrevisaoGastoDTO();
                previsao.setCategoria(categoria);
                previsao.setValorPrevisto(ultimoValor.multiply(BigDecimal.valueOf(1 + tendencia)));

                if (tendencia > 0.05) {
                    previsao.setTendencia("AUMENTANDO");
                } else if (tendencia < -0.05) {
                    previsao.setTendencia("DIMINUINDO");
                } else {
                    previsao.setTendencia("ESTAVEL");
                }

                // Calcula confiança baseada na variância dos dados
                previsao.setConfianca(calcularConfianca(valores));

                previsoes.add(previsao);
            }
        });

        return previsoes;
    }

    private String determinarFrequencia(List<Despesa> despesas) {
        // Ordena por data
        List<Despesa> ordenadas = new ArrayList<>(despesas);
        ordenadas.sort(Comparator.comparing(Despesa::getDataVencimento));

        // Calcula intervalo médio entre despesas
        if (ordenadas.size() < 2) return "UNKNOWN";

        long totalDias = 0;
        for (int i = 1; i < ordenadas.size(); i++) {
            totalDias += ordenadas.get(i).getDataVencimento()
                    .toEpochDay() - ordenadas.get(i - 1).getDataVencimento().toEpochDay();
        }

        long mediaIntervaloDias = totalDias / (ordenadas.size() - 1);

        // Determina frequência baseada no intervalo médio
        if (mediaIntervaloDias <= 35) return "MONTHLY";
        if (mediaIntervaloDias <= 70) return "BIMONTHLY";
        if (mediaIntervaloDias <= 100) return "QUARTERLY";
        if (mediaIntervaloDias <= 200) return "SEMIANNUAL";
        return "ANNUAL";
    }

    private double calcularTendencia(double[] valores) {
        if (valores.length < 2) return 0;

        // Calcula variação percentual média
        double somaVariacoes = 0;
        for (int i = 1; i < valores.length; i++) {
            if (valores[i - 1] != 0) {
                somaVariacoes += (valores[i] - valores[i - 1]) / valores[i - 1];
            }
        }

        return somaVariacoes / (valores.length - 1);
    }

    private double calcularConfianca(double[] valores) {
        if (valores.length < 2) return 0;

        // Calcula variância normalizada
        double media = Arrays.stream(valores).average().orElse(0);
        double somaQuadrados = Arrays.stream(valores)
                .map(v -> Math.pow(v - media, 2))
                .sum();
        double variancia = somaQuadrados / (valores.length - 1);

        // Normaliza para um valor entre 0 e 1
        // Quanto menor a variância, maior a confiança
        double varianciaNormalizada = variancia / (media * media);
        return Math.max(0, Math.min(1, 1 - varianciaNormalizada));
    }
}