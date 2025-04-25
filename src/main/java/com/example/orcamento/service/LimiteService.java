package com.example.orcamento.service;

import com.example.orcamento.model.Limite;
import com.example.orcamento.repository.LimiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LimiteService {

    @Autowired
    private LimiteRepository limiteRepository;

    public Limite salvarLimite(Limite limite) {
        return limiteRepository.save(limite);
    }

    public List<Limite> listarUltimos10Limites() {
        return limiteRepository.findTop10ByOrderByIdDesc();
    }

    public List<Limite> listarPorTipoDespesa(Long tipoDespesaId) {
        return limiteRepository.findByTipoDespesaId(tipoDespesaId);
    }

    public void deletarLimite(Long id) {
        limiteRepository.deleteById(id);
    }
}