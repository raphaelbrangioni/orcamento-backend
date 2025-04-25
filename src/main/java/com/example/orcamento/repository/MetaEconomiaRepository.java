package com.example.orcamento.repository;

import com.example.orcamento.model.MetaEconomia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetaEconomiaRepository extends JpaRepository<MetaEconomia, Long> {
}