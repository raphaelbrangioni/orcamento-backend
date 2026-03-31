package com.example.orcamento.service;

import com.example.orcamento.dto.FechamentoMensalResponseDTO;
import com.example.orcamento.dto.dashboard.DashboardHomeAlertaDTO;
import com.example.orcamento.dto.dashboard.DashboardHomeCartaoDTO;
import com.example.orcamento.dto.dashboard.DashboardHomeContasDTO;
import com.example.orcamento.dto.dashboard.DashboardHomeDTO;
import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DashboardHomeService {

    private final FechamentoMensalService fechamentoMensalService;
    private final ContaCorrenteService contaCorrenteService;
    private final CartaoCreditoRepository cartaoCreditoRepository;
    private final LancamentoCartaoRepository lancamentoCartaoRepository;
    private final DespesaRepository despesaRepository;
    private final DespesaService despesaService;

    @Transactional(readOnly = true)
    public DashboardHomeDTO obterDashboard(Integer ano, Integer mes) {
        YearMonth competencia = resolverCompetencia(ano, mes);
        String tenantId = TenantContext.getTenantId();

        FechamentoMensalResponseDTO fechamentoMensal = fechamentoMensalService
                .obterResumoMensal(competencia.getYear(), competencia.getMonthValue());

        List<ContaCorrente> contasAtivas = contaCorrenteService.listarTodos().stream()
                .filter(ContaCorrente::isContaAtiva)
                .toList();

        BigDecimal saldoTotal = contasAtivas.stream()
                .map(ContaCorrente::getSaldo)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DashboardHomeContasDTO contas = DashboardHomeContasDTO.builder()
                .quantidadeContasAtivas(contasAtivas.size())
                .saldoTotal(saldoTotal)
                .build();

        String mesAnoFatura = formatarMesAnoFatura(competencia.getYear(), competencia.getMonthValue());
        List<DashboardHomeCartaoDTO> cartoes = cartaoCreditoRepository.findByTenantId(tenantId).stream()
                .map(cartao -> toCartaoDto(cartao, competencia, mesAnoFatura, tenantId))
                .toList();

        BigDecimal totalFaturasCartoes = cartoes.stream()
                .map(DashboardHomeCartaoDTO::getValorFatura)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DashboardHomeAlertaDTO> alertas = montarAlertas(competencia, fechamentoMensal, contasAtivas, cartoes);

        return DashboardHomeDTO.builder()
                .ano(competencia.getYear())
                .mes(competencia.getMonthValue())
                .fechamentoMensal(fechamentoMensal)
                .contas(contas)
                .totalFaturasCartoes(totalFaturasCartoes)
                .cartoes(cartoes)
                .alertas(alertas)
                .build();
    }

    private List<DashboardHomeAlertaDTO> montarAlertas(
            YearMonth competencia,
            FechamentoMensalResponseDTO fechamentoMensal,
            List<ContaCorrente> contasAtivas,
            List<DashboardHomeCartaoDTO> cartoes
    ) {
        List<DashboardHomeAlertaDTO> alertas = new ArrayList<>();

        BigDecimal totalFaturasNaoLancadas = fechamentoMensal.getTotalFaturasNaoLancadas() != null
                ? fechamentoMensal.getTotalFaturasNaoLancadas()
                : BigDecimal.ZERO;
        if (totalFaturasNaoLancadas.compareTo(BigDecimal.ZERO) > 0) {
            int quantidadeCartoesNaoLancados = (int) cartoes.stream()
                    .filter(cartao -> !cartao.isFaturaLancada())
                    .filter(cartao -> valorFaturaPropria(cartao).compareTo(BigDecimal.ZERO) > 0)
                    .count();
            alertas.add(DashboardHomeAlertaDTO.builder()
                    .tipo("FATURAS_NAO_LANCADAS")
                    .nivel("warning")
                    .titulo("Existem faturas proprias nao lancadas")
                    .mensagem("Parte das faturas do mes ainda nao foi lancada como despesa.")
                    .quantidade(quantidadeCartoesNaoLancados)
                    .valor(totalFaturasNaoLancadas)
                    .build());
        }

        if (!competencia.isBefore(YearMonth.from(LocalDate.now()))) {
            int diaReferencia = competencia.equals(YearMonth.from(LocalDate.now()))
                    ? LocalDate.now().getDayOfMonth()
                    : 1;
            List<DashboardHomeCartaoDTO> faturasAVencer = cartoes.stream()
                    .filter(cartao -> cartao.getDiaVencimento() != null && cartao.getDiaVencimento() >= diaReferencia)
                    .filter(cartao -> valorFaturaPropria(cartao).compareTo(BigDecimal.ZERO) > 0)
                    .toList();
            BigDecimal totalAVencer = faturasAVencer.stream()
                    .map(this::valorFaturaPropria)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (!faturasAVencer.isEmpty()) {
                alertas.add(DashboardHomeAlertaDTO.builder()
                        .tipo("FATURAS_A_VENCER")
                        .nivel("warning")
                        .titulo("Existem faturas a vencer")
                        .mensagem("Ha faturas proprias com vencimento futuro na competencia selecionada.")
                        .quantidade(faturasAVencer.size())
                        .valor(totalAVencer)
                        .build());
            }
        }

        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = competencia.atDay(1);
        LocalDate fimConsulta = competencia.atEndOfMonth().isBefore(hoje) ? competencia.atEndOfMonth() : hoje;
        if (!fimConsulta.isBefore(inicioMes)) {
            String tenantId = TenantContext.getTenantId();
            List<Despesa> despesasVencidas = despesaRepository.findByDataVencimentoBetweenAndDataPagamentoIsNull(
                    tenantId,
                    inicioMes,
                    fimConsulta
            );
            BigDecimal totalVencido = despesasVencidas.stream()
                    .map(Despesa::getValorPrevisto)
                    .filter(valor -> valor != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (!despesasVencidas.isEmpty()) {
                alertas.add(DashboardHomeAlertaDTO.builder()
                        .tipo("DESPESAS_VENCIDAS_NAO_PAGAS")
                        .nivel("danger")
                        .titulo("Existem despesas vencidas nao pagas")
                        .mensagem("Ha despesas vencidas no periodo que ainda nao foram pagas.")
                        .quantidade(despesasVencidas.size())
                        .valor(totalVencido)
                        .build());
            }
        }

        List<ContaCorrente> contasNegativas = contasAtivas.stream()
                .filter(conta -> conta.getSaldo() != null && conta.getSaldo().compareTo(BigDecimal.ZERO) < 0)
                .toList();
        BigDecimal totalNegativo = contasNegativas.stream()
                .map(ContaCorrente::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();

        if (!contasNegativas.isEmpty()) {
            alertas.add(DashboardHomeAlertaDTO.builder()
                    .tipo("CONTAS_NEGATIVAS")
                    .nivel("danger")
                    .titulo("Existem contas com saldo negativo")
                    .mensagem("Uma ou mais contas correntes estao negativas.")
                    .quantidade(contasNegativas.size())
                    .valor(totalNegativo)
                    .build());
        }

        return alertas;
    }

    private BigDecimal valorFaturaPropria(DashboardHomeCartaoDTO cartao) {
        BigDecimal valorFatura = cartao.getValorFatura() != null ? cartao.getValorFatura() : BigDecimal.ZERO;
        BigDecimal valorTerceiros = cartao.getValorTerceiros() != null ? cartao.getValorTerceiros() : BigDecimal.ZERO;
        return valorFatura.subtract(valorTerceiros).max(BigDecimal.ZERO);
    }

    private DashboardHomeCartaoDTO toCartaoDto(CartaoCredito cartao, YearMonth competencia, String mesAnoFatura, String tenantId) {
        BigDecimal valorFatura = lancamentoCartaoRepository.getFaturaDoMes(cartao.getId(), mesAnoFatura, tenantId);
        BigDecimal valorTerceiros = lancamentoCartaoRepository.getFaturaDoMesTerceiros(cartao.getId(), mesAnoFatura, tenantId);
        boolean faturaLancada = despesaService.verificarFaturaLancada(
                cartao.getNome(),
                competencia.getMonthValue(),
                competencia.getYear()
        );

        return DashboardHomeCartaoDTO.builder()
                .cartaoId(cartao.getId())
                .nome(cartao.getNome())
                .diaVencimento(cartao.getDiaVencimento())
                .valorFatura(valorFatura != null ? valorFatura : BigDecimal.ZERO)
                .valorTerceiros(valorTerceiros != null ? valorTerceiros : BigDecimal.ZERO)
                .faturaLancada(faturaLancada)
                .build();
    }

    private YearMonth resolverCompetencia(Integer ano, Integer mes) {
        if (ano == null || mes == null) {
            return YearMonth.from(LocalDate.now());
        }
        return YearMonth.of(ano, mes);
    }

    private String formatarMesAnoFatura(int ano, int mes) {
        String mesNome = YearMonth.of(ano, mes)
                .getMonth()
                .getDisplayName(TextStyle.FULL, new Locale("pt", "BR"))
                .toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replace('Ã', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U');
        return mesNome + "/" + ano;
    }
}
