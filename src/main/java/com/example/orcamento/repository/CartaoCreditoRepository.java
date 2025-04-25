// CartaoCreditoRepository.java
package com.example.orcamento.repository;

import com.example.orcamento.model.CartaoCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CartaoCreditoRepository extends JpaRepository<CartaoCredito, Long> {

    // Consulta para buscar todos os IDs de cartões de crédito
    @Query("SELECT c.id FROM CartaoCredito c")
    List<Long> findAllCartaoIds();
}