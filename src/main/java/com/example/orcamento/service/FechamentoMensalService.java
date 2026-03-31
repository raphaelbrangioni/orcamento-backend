package com.example.orcamento.service;

import com.example.orcamento.dto.FechamentoMensalResponseDTO;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.FechamentoMensal;
import com.example.orcamento.dto.FechamentoMensalHistoricoDTO;
import com.example.orcamento.model.FechamentoMensalHistorico;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.model.Receita;
import com.example.orcamento.model.enums.FormaDePagamento;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.ContaCorrenteSaldoDiaRepository;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.FechamentoMensalHistoricoRepository;
import com.example.orcamento.repository.FechamentoMensalRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.ReceitaRepository;
import com.example.orcamento.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
    private final CartaoCreditoRepository cartaoCreditoRepository;
    private final FechamentoMensalHistoricoRepository fechamentoMensalHistoricoRepository;

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
        String username = obterUsernameAutenticado();
        List<ContaCorrente> contasAtivas = contaCorrenteService.listarTodos().stream()
                .filter(ContaCorrente::isContaAtiva)
                .toList();

        if (deveValidarFechamentoDiario(competencia)) {
            LocalDate ultimoDiaUtil = obterUltimoDiaUtilDoMes(competencia);
            validarUltimoDiaUtilFechado(tenantId, contasAtivas, ultimoDiaUtil, ano, mes);
        }

        LocalDate inicioMes = competencia.atDay(1);
        LocalDate fimMes = competencia.atEndOfMonth();

        ResumoMensal resumoMensal = calcularResumoMensal(competencia, tenantId, contasAtivas, false);

        FechamentoMensal fechamentoMensal = fechamentoMensalRepository
                .findByTenantIdAndAnoAndMes(tenantId, ano, mes)
                .orElseGet(FechamentoMensal::new);
        boolean reprocessamento = fechamentoMensal.getId() != null;
        LocalDateTime agora = LocalDateTime.now();

        fechamentoMensal.setTenantId(tenantId);
        fechamentoMensal.setAno(ano);
        fechamentoMensal.setMes(mes);
        fechamentoMensal.setSaldoInicial(resumoMensal.saldoInicial());
        fechamentoMensal.setReceitasRealizadas(resumoMensal.receitasRealizadas());
        fechamentoMensal.setDespesasDoMes(resumoMensal.despesasDoMes());
        fechamentoMensal.setDespesasPagas(resumoMensal.despesasPagas());
        fechamentoMensal.setDespesasPagasNoCaixa(resumoMensal.despesasPagasNoCaixa());
        fechamentoMensal.setDespesasPagasCartao(resumoMensal.despesasPagasCartao());
        fechamentoMensal.setTotalFaturas(resumoMensal.totalFaturas());
        fechamentoMensal.setTotalTerceirosFaturas(resumoMensal.totalTerceirosFaturas());
        fechamentoMensal.setSaldoFinal(resumoMensal.saldoFinal());
        fechamentoMensal.setCalculadoEm(agora);
        if (!reprocessamento) {
            fechamentoMensal.setFechadoPor(username);
            fechamentoMensal.setFechadoEm(agora);
            fechamentoMensal.setUltimoReprocessamentoPor(null);
            fechamentoMensal.setUltimoReprocessamentoEm(null);
        } else {
            fechamentoMensal.setUltimoReprocessamentoPor(username);
            fechamentoMensal.setUltimoReprocessamentoEm(agora);
        }

        FechamentoMensal fechamentoMensalSalvo = fechamentoMensalRepository.save(fechamentoMensal);
        registrarHistorico(
                tenantId,
                fechamentoMensalSalvo.getId(),
                ano,
                mes,
                reprocessamento ? "REPROCESSADO" : "FECHADO",
                username,
                agora
        );

        log.info(
                "fechamento_mensal.fechado fechamentoMensalId={} tenantId={} ano={} mes={} username={} evento={} saldoInicial={} receitasRealizadas={} despesasDoMes={} despesasPagas={} despesasPagasNoCaixa={} despesasPagasCartao={} totalFaturas={} totalTerceirosFaturas={} saldoFinal={}",
                fechamentoMensalSalvo.getId(),
                tenantId,
                ano,
                mes,
                username,
                reprocessamento ? "REPROCESSADO" : "FECHADO",
                resumoMensal.saldoInicial(),
                resumoMensal.receitasRealizadas(),
                resumoMensal.despesasDoMes(),
                resumoMensal.despesasPagas(),
                resumoMensal.despesasPagasNoCaixa(),
                resumoMensal.despesasPagasCartao(),
                resumoMensal.totalFaturas(),
                resumoMensal.totalTerceirosFaturas(),
                resumoMensal.saldoFinal()
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
    public Optional<FechamentoMensalResponseDTO> buscarFechamentoOpcional(int ano, int mes) {
        validarAnoMes(ano, mes);
        String tenantId = TenantContext.getTenantId();
        return fechamentoMensalRepository.findByTenantIdAndAnoAndMes(tenantId, ano, mes)
                .map(this::toResponseDto);
    }

    @Transactional(readOnly = true)
    public FechamentoMensalResponseDTO obterResumoMensal(int ano, int mes) {
        validarAnoMes(ano, mes);
        String tenantId = TenantContext.getTenantId();
        Optional<FechamentoMensal> fechamentoExistente = fechamentoMensalRepository.findByTenantIdAndAnoAndMes(tenantId, ano, mes);
        if (fechamentoExistente.isPresent()) {
            return toResponseDto(fechamentoExistente.get());
        }

        YearMonth competencia = YearMonth.of(ano, mes);
        List<ContaCorrente> contasAtivas = contaCorrenteService.listarTodos().stream()
                .filter(ContaCorrente::isContaAtiva)
                .toList();

        ResumoMensal resumoMensal = calcularResumoMensal(competencia, tenantId, contasAtivas, true);
        return FechamentoMensalResponseDTO.builder()
                .id(null)
                .ano(ano)
                .mes(mes)
                .fechado(false)
                .saldoInicial(resumoMensal.saldoInicial())
                .receitasRealizadas(resumoMensal.receitasRealizadas())
                .despesasDoMes(resumoMensal.despesasDoMes())
                .despesasPagas(resumoMensal.despesasPagas())
                .despesasPagasNoCaixa(resumoMensal.despesasPagasNoCaixa())
                .despesasPagasCartao(resumoMensal.despesasPagasCartao())
                .totalFaturas(resumoMensal.totalFaturas())
                .totalTerceirosFaturas(resumoMensal.totalTerceirosFaturas())
                .totalFaturasProprias(resumoMensal.totalFaturasProprias())
                .totalFaturasLancadasComoDespesa(resumoMensal.totalFaturasLancadasComoDespesa())
                .totalFaturasNaoLancadas(resumoMensal.totalFaturasNaoLancadas())
                .saldoFinal(resumoMensal.saldoFinal())
                .calculadoEm(LocalDateTime.now())
                .build();
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

    @Transactional(readOnly = true)
    public List<FechamentoMensalHistoricoDTO> listarHistorico(int ano, int mes) {
        validarAnoMes(ano, mes);
        String tenantId = TenantContext.getTenantId();
        return fechamentoMensalHistoricoRepository.findByTenantIdAndAnoAndMesOrderByOcorridoEmDesc(tenantId, ano, mes)
                .stream()
                .map(historico -> FechamentoMensalHistoricoDTO.builder()
                        .id(historico.getId())
                        .fechamentoMensalId(historico.getFechamentoMensalId())
                        .ano(historico.getAno())
                        .mes(historico.getMes())
                        .evento(historico.getEvento())
                        .username(historico.getUsername())
                        .ocorridoEm(historico.getOcorridoEm())
                        .build())
                .toList();
    }

    @Transactional
    public void reabrirMes(int ano, int mes) {
        validarAnoMes(ano, mes);
        String tenantId = TenantContext.getTenantId();
        String username = obterUsernameAutenticado();
        LocalDateTime agora = LocalDateTime.now();

        FechamentoMensal fechamentoMensal = fechamentoMensalRepository.findByTenantIdAndAnoAndMes(tenantId, ano, mes)
                .orElseThrow(() -> new IllegalArgumentException("Fechamento mensal nao encontrado para " + ano + "/" + mes));

        registrarHistorico(
                tenantId,
                fechamentoMensal.getId(),
                ano,
                mes,
                "REABERTO",
                username,
                agora
        );
        fechamentoMensalRepository.deleteByTenantIdAndAnoAndMes(tenantId, ano, mes);
        log.info(
                "fechamento_mensal.reaberto fechamentoMensalId={} tenantId={} ano={} mes={} username={}",
                fechamentoMensal.getId(),
                tenantId,
                ano,
                mes,
                username
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

    private ResumoMensal calcularResumoMensal(
            YearMonth competencia,
            String tenantId,
            List<ContaCorrente> contasAtivas,
            boolean permitirPrevisao
    ) {
        LocalDate inicioMes = competencia.atDay(1);
        LocalDate fimMes = competencia.atEndOfMonth();

        BigDecimal saldoInicial = obterSaldoInicialPreview(contasAtivas, tenantId, competencia, permitirPrevisao);

        List<Receita> receitasMes = receitaRepository.findByDataRecebimentoBetweenAndTenantId(inicioMes, fimMes, tenantId);
        BigDecimal receitasRealizadas = receitasMes.stream()
                .filter(receita -> !receita.isPrevista())
                .map(Receita::getValor)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Despesa> despesasDoMesLista = despesaRepository.findByTenantIdAndDataVencimentoBetween(tenantId, inicioMes, fimMes);
        BigDecimal despesasDoMes = despesasDoMesLista.stream()
                .map(Despesa::getValorPrevisto)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Despesa> despesasPagasMes = despesasDoMesLista.stream()
                .filter(despesa -> despesa.getValorPago() != null)
                .toList();

        BigDecimal despesasPagas = despesasPagasMes.stream()
                .map(Despesa::getValorPago)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal despesasPagasCartao = despesasDoMesLista.stream()
                .filter(despesa -> despesa.getFormaDePagamento() == FormaDePagamento.CREDITO)
                .filter(despesa -> despesa.getValorPago() != null)
                .map(Despesa::getValorPago)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal despesasPagasNoCaixa = despesasPagasMes.stream()
                .filter(despesa -> despesa.getFormaDePagamento() != FormaDePagamento.CREDITO)
                .map(Despesa::getValorPago)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String mesAnoFatura = formatarMesAnoFatura(competencia.getYear(), competencia.getMonthValue());
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

        IndicadoresFatura indicadoresFatura = calcularIndicadoresFatura(
                tenantId,
                competencia,
                despesasDoMesLista,
                totalFaturas,
                totalTerceirosFaturas
        );

        BigDecimal saldoFinal = saldoInicial.add(receitasRealizadas).subtract(despesasPagasNoCaixa);

        return new ResumoMensal(
                saldoInicial,
                receitasRealizadas,
                despesasDoMes,
                despesasPagas,
                despesasPagasNoCaixa,
                despesasPagasCartao,
                totalFaturas,
                totalTerceirosFaturas,
                indicadoresFatura.totalFaturasProprias(),
                indicadoresFatura.totalFaturasLancadasComoDespesa(),
                indicadoresFatura.totalFaturasNaoLancadas(),
                saldoFinal
        );
    }

    private IndicadoresFatura calcularIndicadoresFatura(
            String tenantId,
            YearMonth competencia,
            List<Despesa> despesasDoMesLista,
            BigDecimal totalFaturas,
            BigDecimal totalTerceirosFaturas
    ) {
        BigDecimal totalFaturasProprias = totalFaturas.subtract(totalTerceirosFaturas).max(BigDecimal.ZERO);

        BigDecimal totalFaturasLancadasComoDespesa = despesasDoMesLista.stream()
                .filter(this::isDespesaDeFaturaCartao)
                .map(Despesa::getValorPrevisto)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String mesAnoFatura = formatarMesAnoFatura(competencia.getYear(), competencia.getMonthValue());
        BigDecimal totalFaturasNaoLancadas = cartaoCreditoRepository.findByTenantId(tenantId).stream()
                .filter(cartao -> !despesaRepository.existsByNomeLikeAndMesAndAno(
                        tenantId,
                        "Fatura Cartao " + cartao.getNome(),
                        competencia.getMonthValue(),
                        competencia.getYear()
                ))
                .map(cartao -> {
                    BigDecimal valorFatura = lancamentoCartaoRepository.getFaturaDoMes(cartao.getId(), mesAnoFatura, tenantId);
                    BigDecimal valorTerceiros = lancamentoCartaoRepository.getFaturaDoMesTerceiros(cartao.getId(), mesAnoFatura, tenantId);
                    BigDecimal total = valorFatura != null ? valorFatura : BigDecimal.ZERO;
                    BigDecimal terceiros = valorTerceiros != null ? valorTerceiros : BigDecimal.ZERO;
                    return total.subtract(terceiros).max(BigDecimal.ZERO);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new IndicadoresFatura(
                totalFaturasProprias,
                totalFaturasLancadasComoDespesa,
                totalFaturasNaoLancadas
        );
    }

    private boolean isDespesaDeFaturaCartao(Despesa despesa) {
        if (despesa.getNome() == null || despesa.getNome().isBlank()) {
            return false;
        }
        String normalizado = Normalizer.normalize(despesa.getNome(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        return normalizado.startsWith("FATURA CARTAO ");
    }

    private BigDecimal obterSaldoInicialPreview(
            List<ContaCorrente> contasAtivas,
            String tenantId,
            YearMonth competencia,
            boolean permitirPrevisao
    ) {
        YearMonth competenciaAnterior = competencia.minusMonths(1);

        Optional<FechamentoMensal> fechamentoAnterior = fechamentoMensalRepository
                .findByTenantIdAndAnoAndMes(tenantId, competenciaAnterior.getYear(), competenciaAnterior.getMonthValue());
        if (fechamentoAnterior.isPresent()) {
            return fechamentoAnterior.get().getSaldoFinal();
        }

        if (!permitirPrevisao) {
            return BigDecimal.ZERO;
        }

        YearMonth mesAtual = YearMonth.from(LocalDate.now());
        if (competenciaAnterior.isAfter(mesAtual)) {
            ResumoMensal resumoAnterior = calcularResumoMensal(competenciaAnterior, tenantId, contasAtivas, true);
            return resumoAnterior.saldoFinal();
        }

        return BigDecimal.ZERO;
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
        String tenantId = TenantContext.getTenantId();
        YearMonth competencia = YearMonth.of(fechamentoMensal.getAno(), fechamentoMensal.getMes());
        LocalDate inicioMes = competencia.atDay(1);
        LocalDate fimMes = competencia.atEndOfMonth();
        List<Despesa> despesasDoMesLista = despesaRepository.findByTenantIdAndDataVencimentoBetween(tenantId, inicioMes, fimMes);
        IndicadoresFatura indicadoresFatura = calcularIndicadoresFatura(
                tenantId,
                competencia,
                despesasDoMesLista,
                fechamentoMensal.getTotalFaturas(),
                fechamentoMensal.getTotalTerceirosFaturas()
        );

        return FechamentoMensalResponseDTO.builder()
                .id(fechamentoMensal.getId())
                .ano(fechamentoMensal.getAno())
                .mes(fechamentoMensal.getMes())
                .fechado(true)
                .saldoInicial(fechamentoMensal.getSaldoInicial())
                .receitasRealizadas(fechamentoMensal.getReceitasRealizadas())
                .despesasDoMes(fechamentoMensal.getDespesasDoMes())
                .despesasPagas(fechamentoMensal.getDespesasPagas())
                .despesasPagasNoCaixa(fechamentoMensal.getDespesasPagasNoCaixa())
                .despesasPagasCartao(fechamentoMensal.getDespesasPagasCartao())
                .totalFaturas(fechamentoMensal.getTotalFaturas())
                .totalTerceirosFaturas(fechamentoMensal.getTotalTerceirosFaturas())
                .totalFaturasProprias(indicadoresFatura.totalFaturasProprias())
                .totalFaturasLancadasComoDespesa(indicadoresFatura.totalFaturasLancadasComoDespesa())
                .totalFaturasNaoLancadas(indicadoresFatura.totalFaturasNaoLancadas())
                .saldoFinal(fechamentoMensal.getSaldoFinal())
                .calculadoEm(fechamentoMensal.getCalculadoEm())
                .fechadoPor(fechamentoMensal.getFechadoPor())
                .fechadoEm(fechamentoMensal.getFechadoEm())
                .ultimoReprocessamentoPor(fechamentoMensal.getUltimoReprocessamentoPor())
                .ultimoReprocessamentoEm(fechamentoMensal.getUltimoReprocessamentoEm())
                .build();
    }

    private void registrarHistorico(
            String tenantId,
            Long fechamentoMensalId,
            int ano,
            int mes,
            String evento,
            String username,
            LocalDateTime ocorridoEm
    ) {
        fechamentoMensalHistoricoRepository.save(FechamentoMensalHistorico.builder()
                .tenantId(tenantId)
                .fechamentoMensalId(fechamentoMensalId)
                .ano(ano)
                .mes(mes)
                .evento(evento)
                .username(username)
                .ocorridoEm(ocorridoEm)
                .build());
    }

    private String obterUsernameAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "desconhecido";
        }
        return authentication.getName();
    }

    private record ResumoMensal(
            BigDecimal saldoInicial,
            BigDecimal receitasRealizadas,
            BigDecimal despesasDoMes,
            BigDecimal despesasPagas,
            BigDecimal despesasPagasNoCaixa,
            BigDecimal despesasPagasCartao,
            BigDecimal totalFaturas,
            BigDecimal totalTerceirosFaturas,
            BigDecimal totalFaturasProprias,
            BigDecimal totalFaturasLancadasComoDespesa,
            BigDecimal totalFaturasNaoLancadas,
            BigDecimal saldoFinal
    ) {
    }

    private record IndicadoresFatura(
            BigDecimal totalFaturasProprias,
            BigDecimal totalFaturasLancadasComoDespesa,
            BigDecimal totalFaturasNaoLancadas
    ) {
    }
}
