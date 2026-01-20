package com.example.orcamento.repository;

import com.example.orcamento.model.HorasTrabalhadas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface HorasTrabalhadasRepository extends JpaRepository<HorasTrabalhadas, Long>, JpaSpecificationExecutor<HorasTrabalhadas> {
}
