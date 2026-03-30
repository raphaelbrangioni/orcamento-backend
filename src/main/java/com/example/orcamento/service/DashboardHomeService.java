package com.example.orcamento.service;

import com.example.orcamento.dto.FechamentoMensalResponseDTO;
import com.example.orcamento.dto.dashboard.DashboardHomeCartaoDTO;
import com.example.orcamento.dto.dashboard.DashboardHomeContasDTO;
import com.example.orcamento.dto.dashboard.DashboardHomeDTO;
import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DashboardHomeService {

    private final FechamentoMensalService fechamentoMensalService;
    private final ContaCorrenteService contaCorrenteService;
    private final CartaoCreditoRepository cartaoCreditoRepository;
    private final LancamentoCartaoRepository lancamentoCartaoRepository;
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

        return DashboardHomeDTO.builder()
                .ano(competencia.getYear())
                .mes(competencia.getMonthValue())
                .fechamentoMensal(fechamentoMensal)
                .contas(contas)
                .totalFaturasCartoes(totalFaturasCartoes)
                .cartoes(cartoes)
                .build();
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
