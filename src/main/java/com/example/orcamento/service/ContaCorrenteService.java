// src/main/java/com/example/orcamento/service/ContaCorrenteService.java
package com.example.orcamento.service;

import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.repository.ContaCorrenteRepository;
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
        return contaCorrenteRepository.save(conta);
    }

    public List<ContaCorrente> listarTodos() {
        return contaCorrenteRepository.findAll();
    }

    public Optional<ContaCorrente> buscarPorId(Long id) {
        return contaCorrenteRepository.findById(id);
    }

    @Transactional
    public void atualizarSaldo(Long contaId, BigDecimal valor, boolean isEntrada, LocalDateTime data) {
        ContaCorrente conta = contaCorrenteRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta corrente não encontrada com ID: " + contaId));
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
        contaCorrenteRepository.deleteById(id);
    }

    @Transactional
    public ContaCorrente atualizarConta(Long id, ContaCorrente contaAtualizada) {
        ContaCorrente conta = contaCorrenteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Conta corrente não encontrada: " + id));

        conta.setNomeBanco(contaAtualizada.getNomeBanco());
        conta.setAgencia(contaAtualizada.getAgencia());
        conta.setNumeroConta(contaAtualizada.getNumeroConta());
        conta.setSaldo(contaAtualizada.getSaldo());

        return contaCorrenteRepository.save(conta);
    }


}