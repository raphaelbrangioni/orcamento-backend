package com.example.orcamento.service;

import com.example.orcamento.model.MetaEconomia;
import com.example.orcamento.repository.MetaEconomiaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MetaEconomiaService {

    @Autowired
    private MetaEconomiaRepository repository;

    public List<MetaEconomia> listarMetas() {
        return repository.findAll();
    }

    public MetaEconomia salvarMeta(MetaEconomia meta) {
        return repository.save(meta);
    }

    public void excluirMeta(Long id) {
        repository.deleteById(id);
    }

    public Optional<MetaEconomia> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public MetaEconomia atualizarFracaoBitcoin(Long id, Double fracaoBitcoin) {
        MetaEconomia meta = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Meta não encontrada: " + id));
        meta.setFracaoBitcoin(fracaoBitcoin);
        return repository.save(meta);
    }
}