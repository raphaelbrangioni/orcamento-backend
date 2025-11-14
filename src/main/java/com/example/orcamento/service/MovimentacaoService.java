// src/main/java/com/example/orcamento/service/MovimentacaoService.java
package com.example.orcamento.service;

import com.example.orcamento.model.Movimentacao;
import com.example.orcamento.model.TipoMovimentacao;
import com.example.orcamento.repository.MovimentacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovimentacaoService {

    private final MovimentacaoRepository movimentacaoRepository;
    private final ContaCorrenteService contaCorrenteService;

    @Transactional
    public void registrarMovimentacao(Movimentacao movimentacao) {
        log.info("Registrando movimentação: {}", movimentacao);
        movimentacao.setTenantId(com.example.orcamento.security.TenantContext.getTenantId());
        // Atualiza o saldo da conta corrente
        boolean isEntrada = movimentacao.getTipo() == TipoMovimentacao.ENTRADA;
        log.info("Entrada: {}", isEntrada);

        contaCorrenteService.atualizarSaldo(
                movimentacao.getContaCorrente().getId(),
                movimentacao.getValor(),
                isEntrada,
                movimentacao.getDataCadastro()
        );

        // Salva a movimentação
        movimentacaoRepository.save(movimentacao);
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