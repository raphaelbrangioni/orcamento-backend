package com.example.orcamento.repository;

import com.example.orcamento.model.Configuracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracaoRepository extends JpaRepository<Configuracao, Long> {
    // Método para buscar a configuração ativa (assumindo que teremos apenas uma)
    Configuracao findFirstByOrderByIdAsc();
}