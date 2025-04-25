package com.example.orcamento.repository;

import com.example.orcamento.model.TipoDespesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoDespesaRepository extends JpaRepository<TipoDespesa, Long> {
    boolean existsByNome(String nome);
}
