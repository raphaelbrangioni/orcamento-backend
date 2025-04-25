package com.example.orcamento.service;


import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.repository.CartaoCreditoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartaoCreditoService {
    private final CartaoCreditoRepository cartaoCreditoRepository;

    public CartaoCredito salvarCartao(CartaoCredito cartao) {
        return cartaoCreditoRepository.save(cartao);
    }

    public List<CartaoCredito> listarCartoes() {
        return cartaoCreditoRepository.findAll();
    }

    public void excluirCartao(Long id) {
        cartaoCreditoRepository.deleteById(id);
    }

    @Transactional
    public CartaoCredito atualizarCartao(Long id, CartaoCredito cataoAtualizado) {
        CartaoCredito cartaoCredito = cartaoCreditoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cartão de crédito não encontrada: " + id));

        cartaoCredito.setNome(cataoAtualizado.getNome());
        cartaoCredito.setLimite(cataoAtualizado.getLimite());
        cartaoCredito.setDiaVencimento(cataoAtualizado.getDiaVencimento());


        return cartaoCreditoRepository.save(cartaoCredito);
    }

}
