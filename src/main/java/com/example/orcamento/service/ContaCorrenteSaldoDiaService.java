package com.example.orcamento.service;

import com.example.orcamento.dto.ContaCorrenteDTO;
import com.example.orcamento.dto.ContaCorrenteSaldoDiaResponseDTO;
import com.example.orcamento.dto.FecharSaldoDiaRequestDTO;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.model.ContaCorrenteSaldoDia;
import com.example.orcamento.model.Movimentacao;
import com.example.orcamento.model.TipoMovimentacao;
import com.example.orcamento.repository.ContaCorrenteSaldoDiaRepository;
import com.example.orcamento.repository.MovimentacaoRepository;
import com.example.orcamento.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContaCorrenteSaldoDiaService {

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

    private final ContaCorrenteService contaCorrenteService;
    private final ContaCorrenteSaldoDiaRepository contaCorrenteSaldoDiaRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    @Transactional
    public ContaCorrenteSaldoDiaResponseDTO fecharDia(Long contaCorrenteId, FecharSaldoDiaRequestDTO request) {
        String tenantId = TenantContext.getTenantId();
        if (request == null || request.getData() == null) {
            throw new IllegalArgumentException("data e obrigatoria para fechar o dia");
        }

        ContaCorrente contaCorrente = contaCorrenteService.buscarPorId(contaCorrenteId)
                .orElseThrow(() -> new EntityNotFoundException("Conta corrente nao encontrada para o tenant atual: " + contaCorrenteId));

        LocalDate data = request.getData();
        validarDiaUtilParaFechamento(contaCorrenteId, tenantId, data);
        validarSequenciaDeFechamento(contaCorrenteId, tenantId, request);

        BigDecimal saldoAbertura = request.getSaldoAbertura() != null
                ? request.getSaldoAbertura()
                : obterSaldoAbertura(contaCorrenteId, tenantId, data);

        List<Movimentacao> movimentacoes = movimentacaoRepository
                .findByContaCorrenteIdAndTenantIdAndDataRecebimentoBetween(contaCorrenteId, tenantId, data, data);

        BigDecimal totalEntradas = somarPorTipo(movimentacoes, TipoMovimentacao.ENTRADA);
        BigDecimal totalSaidas = somarPorTipo(movimentacoes, TipoMovimentacao.SAIDA);
        BigDecimal saldoFechamento = saldoAbertura.add(totalEntradas).subtract(totalSaidas);

        ContaCorrenteSaldoDia saldoDia = contaCorrenteSaldoDiaRepository
                .findByContaCorrenteIdAndTenantIdAndData(contaCorrenteId, tenantId, data)
                .orElseGet(ContaCorrenteSaldoDia::new);

        saldoDia.setContaCorrente(contaCorrente);
        saldoDia.setTenantId(tenantId);
        saldoDia.setData(data);
        saldoDia.setSaldoAbertura(saldoAbertura);
        saldoDia.setTotalEntradas(totalEntradas);
        saldoDia.setTotalSaidas(totalSaidas);
        saldoDia.setSaldoFechamento(saldoFechamento);
        saldoDia.setCalculadoEm(LocalDateTime.now());

        ContaCorrenteSaldoDia saldoDiaSalvo = contaCorrenteSaldoDiaRepository.save(saldoDia);

        log.info(
                "conta_corrente.saldo_dia_fechado saldoDiaId={} tenantId={} contaId={} data={} saldoAbertura={} entradas={} saidas={} saldoFechamento={}",
                saldoDiaSalvo.getId(),
                tenantId,
                contaCorrenteId,
                data,
                saldoAbertura,
                totalEntradas,
                totalSaidas,
                saldoFechamento
        );

        return toResponseDto(saldoDiaSalvo);
    }

    @Transactional(readOnly = true)
    public List<ContaCorrenteSaldoDiaResponseDTO> listarPorPeriodo(Long contaCorrenteId, LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("dataInicio e dataFim sao obrigatorias");
        }

        String tenantId = TenantContext.getTenantId();
        contaCorrenteService.buscarPorId(contaCorrenteId)
                .orElseThrow(() -> new EntityNotFoundException("Conta corrente nao encontrada para o tenant atual: " + contaCorrenteId));

        return contaCorrenteSaldoDiaRepository
                .findByContaCorrenteIdAndTenantIdAndDataBetweenOrderByDataDesc(contaCorrenteId, tenantId, dataInicio, dataFim)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    private BigDecimal obterSaldoAbertura(Long contaCorrenteId, String tenantId, LocalDate data) {
        return contaCorrenteSaldoDiaRepository
                .findTopByContaCorrenteIdAndTenantIdAndDataLessThanOrderByDataDesc(contaCorrenteId, tenantId, data)
                .map(ContaCorrenteSaldoDia::getSaldoFechamento)
                .orElse(BigDecimal.ZERO);
    }

    private void validarSequenciaDeFechamento(Long contaCorrenteId, String tenantId, FecharSaldoDiaRequestDTO request) {
        LocalDate data = request.getData();
        LocalDate diaUtilAnterior = obterDiaUtilAnterior(data);

        boolean existeFechamentoAnterior = contaCorrenteSaldoDiaRepository
                .existsByContaCorrenteIdAndTenantIdAndDataLessThan(contaCorrenteId, tenantId, data);

        if (!existeFechamentoAnterior) {
            if (request.getSaldoAbertura() == null) {
                log.info(
                        "conta_corrente.saldo_dia_fechamento_bloqueado tenantId={} contaId={} data={} motivo=primeiro_fechamento_sem_saldo_abertura",
                        tenantId,
                        contaCorrenteId,
                        data
                );
                throw new IllegalArgumentException(
                        "Para o primeiro fechamento da conta, informe o saldo de abertura"
                );
            }
            return;
        }

        boolean diaAnteriorFechado = contaCorrenteSaldoDiaRepository
                .existsByContaCorrenteIdAndTenantIdAndData(contaCorrenteId, tenantId, diaUtilAnterior);

        if (!diaAnteriorFechado) {
            log.info(
                    "conta_corrente.saldo_dia_fechamento_bloqueado tenantId={} contaId={} data={} motivo=dia_util_anterior_nao_fechado diaUtilAnterior={}",
                    tenantId,
                    contaCorrenteId,
                    data,
                    diaUtilAnterior
            );
            throw new IllegalArgumentException(
                    "Nao e permitido fechar o dia sem que o ultimo dia util anterior esteja fechado: " + diaUtilAnterior
            );
        }
    }

    private void validarDiaUtilParaFechamento(Long contaCorrenteId, String tenantId, LocalDate data) {
        DayOfWeek dayOfWeek = data.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            log.info(
                    "conta_corrente.saldo_dia_fechamento_bloqueado tenantId={} contaId={} data={} motivo=fim_de_semana",
                    tenantId,
                    contaCorrenteId,
                    data
            );
            throw new IllegalArgumentException("Nao e permitido fechar o dia em sabado ou domingo");
        }

        if (isFeriadoNacional(data)) {
            log.info(
                    "conta_corrente.saldo_dia_fechamento_bloqueado tenantId={} contaId={} data={} motivo=feriado_nacional",
                    tenantId,
                    contaCorrenteId,
                    data
            );
            throw new IllegalArgumentException("Nao e permitido fechar o dia em feriado nacional");
        }
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

    private LocalDate obterDiaUtilAnterior(LocalDate data) {
        LocalDate diaAnterior = data.minusDays(1);
        while (!isDiaUtil(diaAnterior)) {
            diaAnterior = diaAnterior.minusDays(1);
        }
        return diaAnterior;
    }

    private boolean isDiaUtil(LocalDate data) {
        DayOfWeek dayOfWeek = data.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY
                && dayOfWeek != DayOfWeek.SUNDAY
                && !isFeriadoNacional(data);
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

    private BigDecimal somarPorTipo(List<Movimentacao> movimentacoes, TipoMovimentacao tipoMovimentacao) {
        return movimentacoes.stream()
                .filter(movimentacao -> movimentacao.getTipo() == tipoMovimentacao)
                .map(Movimentacao::getValor)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ContaCorrenteSaldoDiaResponseDTO toResponseDto(ContaCorrenteSaldoDia saldoDia) {
        ContaCorrente conta = saldoDia.getContaCorrente();
        ContaCorrenteDTO contaDto = new ContaCorrenteDTO(
                conta.getId(),
                conta.getAgencia(),
                conta.getNumeroConta(),
                conta.getBanco(),
                conta.getNomeBanco(),
                conta.getSaldo()
        );

        return ContaCorrenteSaldoDiaResponseDTO.builder()
                .id(saldoDia.getId())
                .data(saldoDia.getData())
                .saldoAbertura(saldoDia.getSaldoAbertura())
                .totalEntradas(saldoDia.getTotalEntradas())
                .totalSaidas(saldoDia.getTotalSaidas())
                .saldoFechamento(saldoDia.getSaldoFechamento())
                .calculadoEm(saldoDia.getCalculadoEm())
                .contaCorrente(contaDto)
                .build();
    }
}
