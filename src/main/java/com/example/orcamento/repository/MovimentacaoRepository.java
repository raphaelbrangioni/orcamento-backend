// src/main/java/com/example/orcamento/repository/MovimentacaoRepository.java
package com.example.orcamento.repository;

import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.Movimentacao;
import com.example.orcamento.model.Receita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {
    List<Movimentacao> findByContaCorrenteId(Long contaCorrenteId);

    List<Movimentacao> findByReceita(Receita receita);
    List<Movimentacao> findByDespesa(Despesa despesa);
    List<Movimentacao> findByContaCorrenteIdAndDataRecebimentoBetween(Long contaCorrenteId, LocalDate dataInicio, LocalDate dataFim);
    List<Movimentacao> findByDataRecebimentoBetween(LocalDate dataInicio, LocalDate dataFim);
}