package com.example.orcamento.service;

import com.example.orcamento.dto.TransacaoFinanceiraDTO;
import com.example.orcamento.model.Limite;
import com.example.orcamento.service.TransacaoFinanceiraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertaService {

    private static final Logger log = LoggerFactory.getLogger(AlertaService.class);

    @Autowired
    private DespesaService despesaService;

    @Autowired
    private LimiteService limiteService;

    @Autowired
    private TransacaoFinanceiraService transacaoFinanceiraService;

    public List<Map<String, Object>> verificarLimites() {
        List<Map<String, Object>> alertasDetalhados = new ArrayList<>();

        List<Limite> limites = limiteService.listarUltimos10Limites();
        log.info("Limites encontrados: {}", limites.size());
        if (!limites.isEmpty()) {
            for (Limite l : limites) {
                log.info("Limite: categoria={}, valor={}, id={}", l.getTipoDespesa().getNome(), l.getValor(), l.getTipoDespesa().getId());
            }
        }

        // Período: mês atual
        int ano = LocalDate.now().getYear();
        int mes = LocalDate.now().getMonthValue();
        LocalDate dataInicio = LocalDate.of(ano, mes, 1);
        LocalDate dataFim = dataInicio.withDayOfMonth(dataInicio.lengthOfMonth());

        Map<String, Object> filtros = new HashMap<>();
        filtros.put("dataInicio", dataInicio.toString());
        filtros.put("dataFim", dataFim.toString());

        List<TransacaoFinanceiraDTO> transacoes = transacaoFinanceiraService.filtrarTransacoesDinamico(filtros);

        // Agrupa por categoriaId
        Map<Long, Double> gastosPorCategoria = new HashMap<>();
        for (TransacaoFinanceiraDTO t : transacoes) {
            if (t.getCategoria() != null && t.getCategoria().getId() != null) {
                Long categoriaId = t.getCategoria().getId();
                double valor = t.getValor() != null ? t.getValor().doubleValue() : 0.0;
                gastosPorCategoria.put(categoriaId, gastosPorCategoria.getOrDefault(categoriaId, 0.0) + valor);
            }
        }
        log.info("Gastos por categoria (por id) via transacaoFinanceiraService: {}", gastosPorCategoria);

        for (Limite limite : limites) {
            Long categoriaId = limite.getTipoDespesa().getId();
            double limiteValor = limite.getValor();
            double gastoAtual = gastosPorCategoria.getOrDefault(categoriaId, 0.0);
            log.info("CategoriaId: {}, Limite: {}, Gasto: {}", categoriaId, limiteValor, gastoAtual);
            double percentual = (limiteValor > 0) ? (gastoAtual / limiteValor) : 0.0;
            if (percentual >= 0.8) {
                Map<String, Object> alerta = new HashMap<>();
                alerta.put("id", limite.getId());
                alerta.put("categoriaId", categoriaId);
                alerta.put("mensagem", String.format(
                        "Gasto em %s atingiu %.0f%% do limite de R$%.2f: R$%.2f",
                        limite.getTipoDespesa().getNome(), percentual * 100, limiteValor, gastoAtual
                ));
                alerta.put("categoria", limite.getTipoDespesa().getNome());
                alerta.put("percentual", percentual);
                alerta.put("limite", limiteValor);
                alerta.put("gasto", gastoAtual);
                alertasDetalhados.add(alerta);
            }
        }
        return alertasDetalhados;
    }
}