package com.example.orcamento.repository;

import com.example.orcamento.model.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PessoaRepository extends JpaRepository<Pessoa, Long> {
    List<Pessoa> findByTenantId(String tenantId);
    Optional<Pessoa> findByIdAndTenantId(Long id, String tenantId);
}
