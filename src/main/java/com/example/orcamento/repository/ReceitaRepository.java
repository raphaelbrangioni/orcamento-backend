// src/main/java/com/example/orcamento/repository/ReceitaRepository.java
package com.example.orcamento.repository;

import com.example.orcamento.model.Receita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceitaRepository extends JpaRepository<Receita, Long> {
    List<Receita> findByDataRecebimentoBetween(LocalDate inicio, LocalDate fim);
    List<Receita> findByTenantId(String tenantId);
    List<Receita> findByDataRecebimentoBetweenAndTenantId(LocalDate inicio, LocalDate fim, String tenantId);
    List<Receita> findByContaCorrenteIdAndTenantIdAndDataRecebimentoBetween(Long contaCorrenteId, String tenantId, LocalDate inicio, LocalDate fim);
    Optional<Receita> findByIdAndTenantId(Long id, String tenantId);
    void deleteByIdAndTenantId(Long id, String tenantId);
}
