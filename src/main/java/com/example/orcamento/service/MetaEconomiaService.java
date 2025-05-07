package com.example.orcamento.service;

import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.MetaEconomia;
import com.example.orcamento.repository.DespesaRepository;
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

    @Autowired
    private DespesaRepository despesaRepository;


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


    // No MetaEconomiaService.java
    @Transactional
    public int desassociarDespesas(Long metaId) {
        // Verificar se a meta existe
        MetaEconomia meta = buscarPorId(metaId)
                .orElseThrow(() -> new EntityNotFoundException("Meta não encontrada com ID: " + metaId));

        // Buscar todas as despesas associadas a esta meta
        List<Despesa> despesas = despesaRepository.findByMetaEconomiaId(metaId);

        // Remover a associação com a meta
        for (Despesa despesa : despesas) {
            despesa.setMetaEconomia(null);
            despesaRepository.save(despesa);
        }

        return despesas.size();
    }

    @Transactional
    public void excluirMetaComDespesas(Long metaId) {
        // Buscar todas as despesas associadas a esta meta
        List<Despesa> despesas = despesaRepository.findByMetaEconomiaId(metaId);

        // Remover a associação
        for (Despesa despesa : despesas) {
            despesa.setMetaEconomia(null);
            despesaRepository.save(despesa);
        }

        // Excluir a meta
        repository.deleteById(metaId);
    }
}