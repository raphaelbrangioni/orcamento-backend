// src/main/java/com/example/orcamento/service/MovimentacaoService.java
package com.example.orcamento.service;

import com.example.orcamento.model.Movimentacao;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.model.TipoMovimentacao;
import com.example.orcamento.repository.MovimentacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovimentacaoService {

    private final MovimentacaoRepository movimentacaoRepository;
    private final ContaCorrenteService contaCorrenteService;

    @Transactional
    public void registrarMovimentacao(Movimentacao movimentacao) {
        log.info("Registrando movimentação: {}", movimentacao);
        registrarMovimentacaoInternal(movimentacao);
    }

    @Transactional
    public List<Movimentacao> transferirEntreContas(Long contaOrigemId,
                                                    Long contaDestinoId,
                                                    BigDecimal valor,
                                                    LocalDate data,
                                                    String descricao) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        if (contaOrigemId == null) {
            throw new IllegalArgumentException("contaOrigemId é obrigatório");
        }
        if (contaDestinoId == null) {
            throw new IllegalArgumentException("contaDestinoId é obrigatório");
        }
        if (contaOrigemId.equals(contaDestinoId)) {
            throw new IllegalArgumentException("Conta origem e conta destino devem ser diferentes");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("valor é obrigatório e deve ser maior que zero");
        }

        LocalDate dataEfetiva = (data != null) ? data : LocalDate.now();

        ContaCorrente contaOrigem = contaCorrenteService.buscarPorId(contaOrigemId)
                .orElseThrow(() -> new IllegalArgumentException("Conta origem não encontrada: " + contaOrigemId));
        ContaCorrente contaDestino = contaCorrenteService.buscarPorId(contaDestinoId)
                .orElseThrow(() -> new IllegalArgumentException("Conta destino não encontrada: " + contaDestinoId));

        LocalDateTime agora = LocalDateTime.now();
        String descricaoEfetiva = (descricao == null || descricao.isBlank()) ? "Transferência entre contas" : descricao;
        String transferenciaId = UUID.randomUUID().toString();

        Movimentacao debito = Movimentacao.builder()
                .tipo(TipoMovimentacao.SAIDA)
                .valor(valor)
                .contaCorrente(contaOrigem)
                .descricao(descricaoEfetiva)
                .transferenciaId(transferenciaId)
                .dataRecebimento(dataEfetiva)
                .dataCadastro(agora)
                .build();

        Movimentacao credito = Movimentacao.builder()
                .tipo(TipoMovimentacao.ENTRADA)
                .valor(valor)
                .contaCorrente(contaDestino)
                .descricao(descricaoEfetiva)
                .transferenciaId(transferenciaId)
                .dataRecebimento(dataEfetiva)
                .dataCadastro(agora)
                .build();

        List<Movimentacao> resultado = new ArrayList<>(2);
        resultado.add(registrarMovimentacaoInternal(debito));
        resultado.add(registrarMovimentacaoInternal(credito));
        log.info(
                "movimentacao.transferencia.registrada transferenciaId={} tenantId={} contaOrigemId={} contaDestinoId={} valor={} data={}",
                transferenciaId,
                tenantId,
                contaOrigemId,
                contaDestinoId,
                valor,
                dataEfetiva
        );
        return resultado;
    }

    @Transactional
    public List<Movimentacao> estornarTransferencia(String transferenciaId) {
        if (transferenciaId == null || transferenciaId.isBlank()) {
            throw new IllegalArgumentException("transferenciaId é obrigatório");
        }

        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();

        List<Movimentacao> jaEstornada = movimentacaoRepository.findByTransferenciaOriginalIdAndTenantId(transferenciaId, tenantId);
        if (jaEstornada != null && !jaEstornada.isEmpty()) {
            throw new IllegalStateException("Transferência já estornada: " + transferenciaId);
        }

        List<Movimentacao> originais = movimentacaoRepository.findByTransferenciaIdAndTenantId(transferenciaId, tenantId);
        if (originais == null || originais.isEmpty()) {
            throw new IllegalArgumentException("Transferência não encontrada: " + transferenciaId);
        }
        if (originais.size() != 2) {
            throw new IllegalStateException("Transferência inválida. Esperado 2 movimentações, encontrado: " + originais.size());
        }

        Movimentacao debitoOriginal = originais.stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.SAIDA)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Transferência inválida: não foi encontrado débito (SAIDA)"));

        Movimentacao creditoOriginal = originais.stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.ENTRADA)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Transferência inválida: não foi encontrado crédito (ENTRADA)"));

        if (debitoOriginal.getContaCorrente() == null || debitoOriginal.getContaCorrente().getId() == null) {
            throw new IllegalStateException("Transferência inválida: conta origem ausente");
        }
        if (creditoOriginal.getContaCorrente() == null || creditoOriginal.getContaCorrente().getId() == null) {
            throw new IllegalStateException("Transferência inválida: conta destino ausente");
        }

        BigDecimal valor = debitoOriginal.getValor();
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Transferência inválida: valor ausente ou inválido");
        }
        if (creditoOriginal.getValor() == null || creditoOriginal.getValor().compareTo(valor) != 0) {
            throw new IllegalStateException("Transferência inválida: valores de débito e crédito não batem");
        }

        LocalDate dataEfetiva = LocalDate.now();
        LocalDateTime agora = LocalDateTime.now();
        String transferenciaEstornoId = UUID.randomUUID().toString();

        String descricaoBase = (debitoOriginal.getDescricao() == null || debitoOriginal.getDescricao().isBlank())
                ? "Estorno de transferência"
                : "Estorno de transferência: " + debitoOriginal.getDescricao();

        Movimentacao estornoNaOrigem = Movimentacao.builder()
                .tipo(TipoMovimentacao.ENTRADA)
                .valor(valor)
                .contaCorrente(debitoOriginal.getContaCorrente())
                .descricao(descricaoBase)
                .transferenciaId(transferenciaEstornoId)
                .transferenciaOriginalId(transferenciaId)
                .dataRecebimento(dataEfetiva)
                .dataCadastro(agora)
                .build();

        Movimentacao estornoNoDestino = Movimentacao.builder()
                .tipo(TipoMovimentacao.SAIDA)
                .valor(valor)
                .contaCorrente(creditoOriginal.getContaCorrente())
                .descricao(descricaoBase)
                .transferenciaId(transferenciaEstornoId)
                .transferenciaOriginalId(transferenciaId)
                .dataRecebimento(dataEfetiva)
                .dataCadastro(agora)
                .build();

        List<Movimentacao> resultado = new ArrayList<>(2);
        resultado.add(registrarMovimentacaoInternal(estornoNaOrigem));
        resultado.add(registrarMovimentacaoInternal(estornoNoDestino));
        log.info(
                "movimentacao.transferencia.estornada transferenciaId={} tenantId={} contaOrigemId={} contaDestinoId={} valor={}",
                transferenciaId,
                tenantId,
                debitoOriginal.getContaCorrente().getId(),
                creditoOriginal.getContaCorrente().getId(),
                valor
        );
        return resultado;
    }

    private Movimentacao registrarMovimentacaoInternal(Movimentacao movimentacao) {
        validarDataRecebimentoDiaUtil(movimentacao);
        movimentacao.setTenantId(com.example.orcamento.security.TenantContext.getTenantId());

        boolean isEntrada = movimentacao.getTipo() == TipoMovimentacao.ENTRADA;
        log.info("Entrada: {}", isEntrada);

        contaCorrenteService.atualizarSaldo(
                movimentacao.getContaCorrente().getId(),
                movimentacao.getValor(),
                isEntrada,
                movimentacao.getDataCadastro()
        );

        return movimentacaoRepository.save(movimentacao);
    }

    private void validarDataRecebimentoDiaUtil(Movimentacao movimentacao) {
        if (movimentacao == null) {
            throw new IllegalArgumentException("movimentacao é obrigatória");
        }
        if (movimentacao.getDataRecebimento() == null) {
            throw new IllegalArgumentException("dataRecebimento é obrigatória");
        }

        DayOfWeek dayOfWeek = movimentacao.getDataRecebimento().getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            Long contaCorrenteId = (movimentacao.getContaCorrente() != null) ? movimentacao.getContaCorrente().getId() : null;
            String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
            log.info("Movimentação bloqueada: dataRecebimento em final de semana. tenantId={}, contaCorrenteId={}, dataRecebimento={}",
                    tenantId, contaCorrenteId, movimentacao.getDataRecebimento());
            throw new IllegalArgumentException("dataRecebimento deve ser um dia útil (segunda a sexta)");
        }
    }

    public List<Movimentacao> listarMovimentacoesPorConta(Long contaCorrenteId, LocalDate dataInicio, LocalDate dataFim) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();

        boolean hasDateFilter = dataInicio != null && dataFim != null;
        boolean hasContaFilter = contaCorrenteId != null;

        if (hasDateFilter && hasContaFilter) {
            return movimentacaoRepository.findByContaCorrenteIdAndTenantIdAndDataRecebimentoBetween(contaCorrenteId, tenantId, dataInicio, dataFim);
        } else if (hasDateFilter) {
            return movimentacaoRepository.findByDataRecebimentoBetweenAndTenantId(dataInicio, dataFim, tenantId);
        } else if (hasContaFilter) {
            return movimentacaoRepository.findByContaCorrenteIdAndTenantId(contaCorrenteId, tenantId);
        } else {
            return movimentacaoRepository.findByTenantId(tenantId);
        }
    }

}
