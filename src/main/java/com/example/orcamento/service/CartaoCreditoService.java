package com.example.orcamento.service;

import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.repository.CartaoCreditoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartaoCreditoService {
    private final CartaoCreditoRepository cartaoCreditoRepository;

    public CartaoCredito salvarCartao(CartaoCredito cartao) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Usuário sem tenant vinculado. Não é possível cadastrar cartão de crédito.");
        }
        cartao.setTenantId(tenantId);
        return cartaoCreditoRepository.save(cartao);
    }

    public List<CartaoCredito> listarCartoes() {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Usuário sem tenant vinculado. Não é possível listar cartões de crédito.");
        }
        return cartaoCreditoRepository.findByTenantId(tenantId);
    }

    @Transactional
    public void excluirCartao(Long id) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Usuário sem tenant vinculado. Não é possível excluir cartão de crédito.");
        }
        CartaoCredito cartao = cartaoCreditoRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Cartão de crédito não encontrado para o tenant atual: " + id));
        cartaoCreditoRepository.delete(cartao);
    }

    @Transactional
    public CartaoCredito atualizarCartao(Long id, CartaoCredito cataoAtualizado) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Usuário sem tenant vinculado. Não é possível atualizar cartão de crédito.");
        }
        CartaoCredito cartaoCredito = cartaoCreditoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Cartão de crédito não encontrada para o tenant atual: " + id));

        cartaoCredito.setNome(cataoAtualizado.getNome());
        cartaoCredito.setLimite(cataoAtualizado.getLimite());
        cartaoCredito.setDiaVencimento(cataoAtualizado.getDiaVencimento());
        cartaoCredito.setStatus(cataoAtualizado.getStatus());
        cartaoCredito.setModeloImportacao(cataoAtualizado.getModeloImportacao());

        return cartaoCreditoRepository.save(cartaoCredito);
    }

    /**
     * Retorna um cartão pelo ID. Lança EntityNotFoundException se não existir.
     */
    public CartaoCredito buscarPorId(Long id) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return cartaoCreditoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Cartão de crédito não encontrado para o tenant atual: " + id));
    }

    /**
     * Retorna um mapa de IDs para nomes dos cartões cadastrados.
     */
    public Map<Long, String> mapearIdParaNome() {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return cartaoCreditoRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(CartaoCredito::getId, CartaoCredito::getNome));
    }
}
