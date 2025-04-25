package com.example.orcamento.repository;

import com.example.orcamento.model.Limite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LimiteRepository extends JpaRepository<Limite, Long> {
    List<Limite> findTop10ByOrderByIdDesc();
    List<Limite> findByTipoDespesaId(Long tipoDespesaId); // Novo método para filtrar por tipo de despesa
}