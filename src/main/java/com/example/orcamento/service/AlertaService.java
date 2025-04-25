package com.example.orcamento.service;

import com.example.orcamento.model.Limite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AlertaService {

    @Autowired
    private DespesaService despesaService;

    @Autowired
    private LimiteService limiteService;

    public List<String> verificarLimites() {
        List<String> alertas = new ArrayList<>();

        // Obter todos os limites cadastrados
        List<Limite> limites = limiteService.listarUltimos10Limites(); // Ou listarTodosLimites(), se preferir

        // Obter gastos por categoria do mês atual
        int ano = LocalDate.now().getYear();
        int mes = LocalDate.now().getMonthValue();
        Map<String, Double> gastosPorCategoria = despesaService.calcularGastosPorCategoria(ano, mes);

        // Verificar limites
        for (Limite limite : limites) {
            String categoria = limite.getTipoDespesa().getNome();
            double limiteValor = limite.getValor();
            double gastoAtual = gastosPorCategoria.getOrDefault(categoria, 0.0);

            if (gastoAtual > limiteValor) {
                String mensagem = String.format(
                        "Gasto em %s excedeu o limite de R$%.2f: R$%.2f",
                        categoria, limiteValor, gastoAtual
                );
                alertas.add(mensagem);
            }
        }

        return alertas;
    }
}