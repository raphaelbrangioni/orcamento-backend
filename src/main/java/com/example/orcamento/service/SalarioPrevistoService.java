package com.example.orcamento.service;


import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.model.SalarioPrevisto;
import com.example.orcamento.repository.SalarioPrevistoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalarioPrevistoService {

    @Autowired
    private SalarioPrevistoRepository repository;

    // Salva um novo salário previsto
    public SalarioPrevisto salvar(SalarioPrevisto salarioPrevisto) {
        return repository.save(salarioPrevisto);
    }

    // Retorna salários previstos por ano como um mapa (mês -> valor)
    public Map<String, Double> getSalariosPrevistosPorAno(int ano) {
        List<SalarioPrevisto> salarios = repository.findByAno(ano);
        Map<String, Double> resultado = new HashMap<>();
        for (SalarioPrevisto salario : salarios) {
            resultado.put(salario.getMes(), salario.getValorPrevisto());
        }
        return resultado;
    }

    // Retorna salários previstos por ano como um mapa (mês -> valor)
    public List<SalarioPrevisto> listarSalariosPrevistos() {
        List<SalarioPrevisto> salarios = repository.findAll();

        return salarios;
    }

    @Transactional
    public SalarioPrevisto atualizarSalario(Long id, SalarioPrevisto salarioPrevisto) {
        SalarioPrevisto salarioPrev = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salário Previsto não encontrada: " + id));

        salarioPrev.setAno(salarioPrevisto.getAno());
        salarioPrev.setMes(salarioPrevisto.getMes());
        salarioPrev.setValorPrevisto(salarioPrevisto.getValorPrevisto());

        return repository.save(salarioPrev);
    }

    @Transactional
    public void deletarSalario(Long id) {
        SalarioPrevisto salarioPrev = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salário Previsto não encontrada: " + id));


        repository.deleteById(id);
    }
}
