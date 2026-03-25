// src/main/java/com/example/orcamento/controller/TransacaoFinanceiraController.java
package com.example.orcamento.controller;

import com.example.orcamento.dto.TransacaoFinanceiraDTO;
import com.example.orcamento.service.DespesaService;
import com.example.orcamento.service.LancamentoCartaoService;
import com.example.orcamento.service.TransacaoFinanceiraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/transacoes")
@RequiredArgsConstructor
@Slf4j
public class TransacaoFinanceiraController {

    private final DespesaService despesaService;
    private final LancamentoCartaoService lancamentoCartaoService;
    private final TransacaoFinanceiraService transacaoFinanceiraService;

    @GetMapping
    public ResponseEntity<List<TransacaoFinanceiraDTO>> listarTransacoes() {
        log.info("Requisição GET em /api/v1/transacoes, listarTransacoes");

        // O service já faz o filtro multi-tenant
        List<TransacaoFinanceiraDTO> transacoes = transacaoFinanceiraService.filtrarTransacoesDinamico(new HashMap<>());
        return ResponseEntity.ok(transacoes);
    }

    @GetMapping("/filtrar-dinamico")
    public ResponseEntity<List<TransacaoFinanceiraDTO>> listarTransacoesPorFiltrosDinamicos(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) BigDecimal valor,
            @RequestParam(required = false) String detalhes,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long subcategoriaId,
            @RequestParam(required = false) String classificacao,
            @RequestParam(required = false) String variabilidade,
            @RequestParam(required = false) Integer parcela,
            @RequestParam(required = false) Integer totalParcelas,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim,
            @RequestParam(required = false, defaultValue = "false") Boolean busPorFatura,
            @RequestParam(required = false) String origem) {
        log.info("Requisição GET em /api/v1/transacoes/filtrar-dinamico com filtros");



        Map<String, Object> filtros = new HashMap<>();
        if (id != null) filtros.put("id", id);
        if (descricao != null) filtros.put("descricao", descricao);
        if (valor != null) filtros.put("valor", valor);
        if (detalhes != null) filtros.put("detalhes", detalhes);
        if (categoriaId != null) filtros.put("categoriaId", categoriaId);
        if (subcategoriaId != null) filtros.put("subcategoriaId", subcategoriaId);
        if (classificacao != null) filtros.put("classificacao", classificacao);
        if (variabilidade != null) filtros.put("variabilidade", variabilidade);
        if (parcela != null) filtros.put("parcela", parcela);
        if (totalParcelas != null) filtros.put("totalParcelas", totalParcelas);
        if (dataInicio != null) filtros.put("dataInicio", dataInicio);
        if (dataFim != null) filtros.put("dataFim", dataFim);
        if (origem != null) filtros.put("origem", origem);

        if (Boolean.TRUE.equals(busPorFatura) && dataInicio != null && dataFim != null) {
            LocalDate inicio = parseDate(dataInicio, "dataInicio");
            LocalDate fim = parseDate(dataFim, "dataFim");
            if (fim.isBefore(inicio)) {
                throw new IllegalArgumentException("dataFim não pode ser menor que dataInicio");
            }
            filtros.put("mesAnoFaturaList", gerarMesesFatura(inicio, fim));
        } else {
            if (dataInicio != null) {
                filtros.put("dataCompraInicial", parseDate(dataInicio, "dataInicio"));
            }
            if (dataFim != null) {
                filtros.put("dataCompraFinal", parseDate(dataFim, "dataFim"));
            }
        }

        List<TransacaoFinanceiraDTO> transacoes = transacaoFinanceiraService.filtrarTransacoesDinamico(filtros);
        return ResponseEntity.ok(transacoes);
    }

    private List<String> gerarMesesFatura(LocalDate inicio, LocalDate fim) {
        List<String> meses = new ArrayList<>();
        YearMonth atual = YearMonth.from(inicio);
        YearMonth ultimo = YearMonth.from(fim);
        Locale localePtBr = new Locale("pt", "BR");

        while (!atual.isAfter(ultimo)) {
            String nomeMes = atual.getMonth().getDisplayName(TextStyle.FULL, localePtBr).toUpperCase(localePtBr);
            meses.add(nomeMes + "/" + atual.getYear());
            atual = atual.plusMonths(1);
        }

        return meses;
    }

    private LocalDate parseDate(String valor, String campo) {
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    String.format("Data inválida para %s: %s", campo, valor), ex);
        }
    }
}
