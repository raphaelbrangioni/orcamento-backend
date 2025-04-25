// src/main/java/com/example/orcamento/repository/ReceitaRepository.java
package com.example.orcamento.repository;

import com.example.orcamento.model.Receita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReceitaRepository extends JpaRepository<Receita, Long> {
    List<Receita> findByDataRecebimentoBetween(LocalDate inicio, LocalDate fim);
}