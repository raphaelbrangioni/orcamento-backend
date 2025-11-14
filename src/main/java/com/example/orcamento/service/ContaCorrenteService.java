// src/main/java/com/example/orcamento/service/ContaCorrenteService.java
package com.example.orcamento.service;

import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.repository.ContaCorrenteRepository;
import com.example.orcamento.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContaCorrenteService {

    private final ContaCorrenteRepository contaCorrenteRepository;

    @Transactional
    public ContaCorrente salvar(ContaCorrente conta) {
        conta.setTenantId(TenantContext.getTenantId());
        return contaCorrenteRepository.save(conta);
    }

    public List<ContaCorrente> listarTodos() {
        String tenantId = TenantContext.getTenantId();
        return contaCorrenteRepository.findByTenantId(tenantId);
    }

    public Optional<ContaCorrente> buscarPorId(Long id) {
        String tenantId = TenantContext.getTenantId();
        return contaCorrenteRepository.findByIdAndTenantId(id, tenantId);
    }

    @Transactional
    public void atualizarSaldo(Long contaId, BigDecimal valor, boolean isEntrada, LocalDateTime data) {
        String tenantId = TenantContext.getTenantId();
        ContaCorrente conta = contaCorrenteRepository.findByIdAndTenantId(contaId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Conta corrente não encontrada com ID: " + contaId + " para o tenant atual."));
        log.info("Conta corrente atualizada com ID: " + conta.toString());

        BigDecimal novoSaldo = isEntrada
                ? conta.getSaldo().add(valor)
                : conta.getSaldo().subtract(valor);
        conta.setSaldo(novoSaldo);
        log.info("Novo saldo : " + novoSaldo);

        contaCorrenteRepository.save(conta);
    }

    @Transactional
    public void deletar(Long id) {
        String tenantId = TenantContext.getTenantId();
        contaCorrenteRepository.deleteByIdAndTenantId(id, tenantId);
    }

    @Transactional
    public ContaCorrente atualizarConta(Long id, ContaCorrente contaAtualizada) {
        String tenantId = TenantContext.getTenantId();
        ContaCorrente conta = contaCorrenteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Conta corrente não encontrada para o tenant atual: " + id));

        conta.setNomeBanco(contaAtualizada.getNomeBanco());
        conta.setAgencia(contaAtualizada.getAgencia());
        conta.setNumeroConta(contaAtualizada.getNumeroConta());
        conta.setSaldo(contaAtualizada.getSaldo());
        conta.setContaAtiva(contaAtualizada.isContaAtiva());

        return contaCorrenteRepository.save(conta);
    }
}