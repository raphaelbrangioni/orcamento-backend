// CartaoCreditoRepository.java
package com.example.orcamento.repository;

import com.example.orcamento.model.CartaoCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartaoCreditoRepository extends JpaRepository<CartaoCredito, Long> {

    // Consulta para buscar todos os IDs de cartões de crédito
    @Query("SELECT c.id FROM CartaoCredito c")
    List<Long> findAllCartaoIds();

    List<CartaoCredito> findByTenantId(String tenantId);

    Optional<CartaoCredito> findByIdAndTenantId(Long id, String tenantId);

    @Modifying
    @Query("DELETE FROM CartaoCredito c WHERE c.id = :id AND c.tenantId = :tenantId")
    void deleteByIdAndTenantId(@Param("id") Long id, @Param("tenantId") String tenantId);
}