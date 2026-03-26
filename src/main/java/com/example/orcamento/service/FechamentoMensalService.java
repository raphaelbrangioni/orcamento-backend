package com.example.orcamento.service;

import com.example.orcamento.dto.FechamentoMensalResponseDTO;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.model.ContaCorrenteSaldoDia;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.FechamentoMensal;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.model.Receita;
import com.example.orcamento.model.enums.FormaDePagamento;
import com.example.orcamento.repository.ContaCorrenteSaldoDiaRepository;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.FechamentoMensalRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.ReceitaRepository;
import com.example.orcamento.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FechamentoMensalService {

    private static final Set<MonthDay> FERIADOS_NACIONAIS_FIXOS = Set.of(
            MonthDay.of(1, 1),
            MonthDay.of(4, 21),
            MonthDay.of(5, 1),
            MonthDay.of(9, 7),
            MonthDay.of(10, 12),
            MonthDay.of(11, 2),
            MonthDay.of(11, 15),
            MonthDay.of(11, 20),
            MonthDay.of(12, 25)
    );

    private final FechamentoMensalRepository fechamentoMensalRepository;
    private final ContaCorrenteService contaCorrenteService;
    private final ContaCorrenteSaldoDiaRepository contaCorrenteSaldoDiaRepository;
    private final ReceitaRepository receitaRepository;
    private final DespesaRepository despesaRepository;
    private final LancamentoCartaoRepository lancamentoCartaoRepository;

    @Value("${app.fechamento-mensal.validar-fechamento-diario-desde:}")
    private String validarFechamentoDiarioDesde;

    @Transactional
    public FechamentoMensalResponseDTO fecharMes(int ano, int mes) {
        validarAnoMes(ano, mes);

        YearMonth competencia = YearMonth.of(ano, mes);
        YearMonth atual = YearMonth.from(LocalDate.now());
        if (competencia.isAfter(atual)) {
            throw new IllegalArgumentException("Nao e permitido fechar um mes futuro");
        }

        String tenantId = TenantContext.getTenantId();
        List<ContaCorrente> contasAtivas = contaCorrenteService.listarTodos().stream()
                .filter(ContaCorrente::isContaAtiva)
                .toList();

        if (deveValidarFechamentoDiario(competencia)) {
            LocalDate ultimoDiaUtil = obterUltimoDiaUtilDoMes(competencia);
            validarUltimoDiaUtilFechado(tenantId, contasAtivas, ultimoDiaUtil, ano, mes);
        }

        LocalDate inicioMes = competencia.atDay(1);
        LocalDate fimMes = competencia.atEndOfMonth();

        BigDecimal saldoInicial = obterSaldoInicial(contasAtivas, tenantId, competencia);

        List<Receita> receitasMes = receitaRepository.findByDataRecebimentoBetweenAndTenantId(inicioMes, fimMes, tenantId);
        BigDecimal receitasRealizadas = receitasMes.stream()
                .filter(receita -> !receita.isPrevista())
                .map(Receita::getValor)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Despesa> despesasDoMes = despesaRepository.findByTenantIdAndDataVencimentoBetween(tenantId, inicioMes, fimMes);
        BigDecimal totalDespesasDoMes = despesasDoMes.stream()
                .map(Despesa::getValorPrevisto)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Despesa> despesasPagasMes = despesaRepository.findByAnoAndMes(tenantId, ano, mes).stream()
                .filter(despesa -> despesa.getDataPagamento() != null
                        && despesa.getDataPagamento().getYear() == ano
                        && despesa.getDataPagamento().getMonthValue() == mes
                        && despesa.getValorPago() != null)
                .toList();

        BigDecimal totalDespesasPagas = despesasPagasMes.stream()
                .map(Despesa::getValorPago)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal despesasPagasCartao = despesasPagasMes.stream()
                .filter(despesa -> despesa.getFormaDePagamento() == FormaDePagamento.CREDITO)
                .map(Despesa::getValorPago)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal despesasPagasNoCaixa = despesasPagasMes.stream()
                .filter(despesa -> despesa.getFormaDePagamento() != FormaDePagamento.CREDITO)
                .map(Despesa::getValorPago)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String mesAnoFatura = formatarMesAnoFatura(ano, mes);
        List<LancamentoCartao> faturasMes = lancamentoCartaoRepository.findByMesAnoFaturaAndTenantId(mesAnoFatura, tenantId);

        BigDecimal totalFaturas = faturasMes.stream()
                .map(LancamentoCartao::getValorTotal)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTerceirosFaturas = faturasMes.stream()
                .filter(lancamento -> "Terceiros".equalsIgnoreCase(lancamento.getProprietario()))
                .map(LancamentoCartao::getValorTotal)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoFinal = saldoInicial.add(receitasRealizadas).subtract(despesasPagasNoCaixa);

        FechamentoMensal fechamentoMensal = fechamentoMensalRepository
                .findByTenantIdAndAnoAndMes(tenantId, ano, mes)
                .orElseGet(FechamentoMensal::new);

        fechamentoMensal.setTenantId(tenantId);
        fechamentoMensal.setAno(ano);
        fechamentoMensal.setMes(mes);
        fechamentoMensal.setSaldoInicial(saldoInicial);
        fechamentoMensal.setReceitasRealizadas(receitasRealizadas);
        fechamentoMensal.setDespesasDoMes(totalDespesasDoMes);
        fechamentoMensal.setDespesasPagas(totalDespesasPagas);
        fechamentoMensal.setDespesasPagasCartao(despesasPagasCartao);
        fechamentoMensal.setTotalFaturas(totalFaturas);
        fechamentoMensal.setTotalTerceirosFaturas(totalTerceirosFaturas);
        fechamentoMensal.setSaldoFinal(saldoFinal);
        fechamentoMensal.setCalculadoEm(LocalDateTime.now());

        FechamentoMensal fechamentoMensalSalvo = fechamentoMensalRepository.save(fechamentoMensal);

        log.info(
                "fechamento_mensal.fechado fechamentoMensalId={} tenantId={} ano={} mes={} saldoInicial={} receitasRealizadas={} despesasDoMes={} despesasPagas={} despesasPagasNoCaixa={} despesasPagasCartao={} totalFaturas={} totalTerceirosFaturas={} saldoFinal={}",
                fechamentoMensalSalvo.getId(),
                tenantId,
                ano,
                mes,
                saldoInicial,
                receitasRealizadas,
                totalDespesasDoMes,
                totalDespesasPagas,
                despesasPagasNoCaixa,
                despesasPagasCartao,
                totalFaturas,
                totalTerceirosFaturas,
                saldoFinal
        );

        return toResponseDto(fechamentoMensalSalvo);
    }

    @Transactional(readOnly = true)
    public FechamentoMensalResponseDTO buscarFechamento(int ano, int mes) {
        validarAnoMes(ano, mes);
        String tenantId = TenantContext.getTenantId();
        FechamentoMensal fechamentoMensal = fechamentoMensalRepository.findByTenantIdAndAnoAndMes(tenantId, ano, mes)
                .orElseThrow(() -> new IllegalArgumentException("Fechamento mensal nao encontrado para " + ano + "/" + mes));
        return toResponseDto(fechamentoMensal);
    }

    @Transactional(readOnly = true)
    public List<FechamentoMensalResponseDTO> listarFechamentos(Integer ano) {
        String tenantId = TenantContext.getTenantId();
        List<FechamentoMensal> fechamentos = (ano != null)
                ? fechamentoMensalRepository.findByTenantIdAndAnoOrderByMesDesc(tenantId, ano)
                : fechamentoMensalRepository.findByTenantIdOrderByAnoDescMesDesc(tenantId);

        return fechamentos.stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public void reabrirMes(int ano, int mes) {
        validarAnoMes(ano, mes);
        String tenantId = TenantContext.getTenantId();

        FechamentoMensal fechamentoMensal = fechamentoMensalRepository.findByTenantIdAndAnoAndMes(tenantId, ano, mes)
                .orElseThrow(() -> new IllegalArgumentException("Fechamento mensal nao encontrado para " + ano + "/" + mes));

        fechamentoMensalRepository.deleteByTenantIdAndAnoAndMes(tenantId, ano, mes);
        log.info(
                "fechamento_mensal.reaberto fechamentoMensalId={} tenantId={} ano={} mes={}",
                fechamentoMensal.getId(),
                tenantId,
                ano,
                mes
        );
    }

    private void validarUltimoDiaUtilFechado(String tenantId, List<ContaCorrente> contasAtivas, LocalDate ultimoDiaUtil, int ano, int mes) {
        for (ContaCorrente conta : contasAtivas) {
            boolean fechado = contaCorrenteSaldoDiaRepository
                    .existsByContaCorrenteIdAndTenantIdAndData(conta.getId(), tenantId, ultimoDiaUtil);
            if (!fechado) {
                log.info(
                        "fechamento_mensal.bloqueado tenantId={} ano={} mes={} contaId={} motivo=ultimo_dia_util_nao_fechado data={}",
                        tenantId,
                        ano,
                        mes,
                        conta.getId(),
                        ultimoDiaUtil
                );
                throw new IllegalArgumentException(
                        "Nao e permitido fechar o mes sem fechar o ultimo dia util da conta " + conta.getNomeBanco() + " em " + ultimoDiaUtil
                );
            }
        }
    }

    private BigDecimal obterSaldoInicial(List<ContaCorrente> contasAtivas, String tenantId, YearMonth competencia) {
        YearMonth competenciaAnterior = competencia.minusMonths(1);

        return fechamentoMensalRepository
                .findByTenantIdAndAnoAndMes(tenantId, competenciaAnterior.getYear(), competenciaAnterior.getMonthValue())
                .map(FechamentoMensal::getSaldoFinal)
                .orElse(BigDecimal.ZERO);
    }

    private LocalDate obterUltimoDiaUtilDoMes(YearMonth competencia) {
        LocalDate data = competencia.atEndOfMonth();
        while (!isDiaUtil(data)) {
            data = data.minusDays(1);
        }
        return data;
    }

    private boolean isDiaUtil(LocalDate data) {
        DayOfWeek dayOfWeek = data.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY
                && dayOfWeek != DayOfWeek.SUNDAY
                && !isFeriadoNacional(data);
    }

    private boolean isFeriadoNacional(LocalDate data) {
        if (FERIADOS_NACIONAIS_FIXOS.contains(MonthDay.from(data))) {
            return true;
        }

        LocalDate pascoa = calcularPascoa(data.getYear());
        return data.equals(pascoa.minusDays(48))
                || data.equals(pascoa.minusDays(47))
                || data.equals(pascoa.minusDays(2))
                || data.equals(pascoa)
                || data.equals(pascoa.plusDays(60));
    }

    private LocalDate calcularPascoa(int ano) {
        int a = ano % 19;
        int b = ano / 100;
        int c = ano % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
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

    private void validarAnoMes(int ano, int mes) {
        if (ano < 2000 || ano > 3000) {
            throw new IllegalArgumentException("ano invalido");
        }
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("mes invalido");
        }
    }

    private boolean deveValidarFechamentoDiario(YearMonth competencia) {
        YearMonth referencia = obterReferenciaValidacaoFechamentoDiario();
        return !competencia.isBefore(referencia);
    }

    private YearMonth obterReferenciaValidacaoFechamentoDiario() {
        if (validarFechamentoDiarioDesde != null && !validarFechamentoDiarioDesde.isBlank()) {
            return YearMonth.parse(validarFechamentoDiarioDesde.trim());
        }
        return YearMonth.of(LocalDate.now().getYear(), 3);
    }

    private FechamentoMensalResponseDTO toResponseDto(FechamentoMensal fechamentoMensal) {
        return FechamentoMensalResponseDTO.builder()
                .id(fechamentoMensal.getId())
                .ano(fechamentoMensal.getAno())
                .mes(fechamentoMensal.getMes())
                .saldoInicial(fechamentoMensal.getSaldoInicial())
                .receitasRealizadas(fechamentoMensal.getReceitasRealizadas())
                .despesasDoMes(fechamentoMensal.getDespesasDoMes())
                .despesasPagas(fechamentoMensal.getDespesasPagas())
                .despesasPagasCartao(fechamentoMensal.getDespesasPagasCartao())
                .totalFaturas(fechamentoMensal.getTotalFaturas())
                .totalTerceirosFaturas(fechamentoMensal.getTotalTerceirosFaturas())
                .saldoFinal(fechamentoMensal.getSaldoFinal())
                .calculadoEm(fechamentoMensal.getCalculadoEm())
                .build();
    }
}
