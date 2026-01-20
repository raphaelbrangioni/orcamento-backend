package com.example.orcamento.repository;

import com.example.orcamento.model.CompraTerceiro;
import com.example.orcamento.model.CompraTerceiroId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompraTerceiroRepository extends JpaRepository<CompraTerceiro, CompraTerceiroId> {
}
