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
        salarioPrevisto.setTenantId(com.example.orcamento.security.TenantContext.getTenantId());
        return repository.save(salarioPrevisto);
    }

    // Retorna salários previstos por ano como um mapa (mês -> valor)
    public Map<String, Double> getSalariosPrevistosPorAno(int ano) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<SalarioPrevisto> salarios = repository.findByAnoAndTenantId(ano, tenantId);
        Map<String, Double> resultado = new HashMap<>();
        for (SalarioPrevisto salario : salarios) {
            resultado.put(salario.getMes(), salario.getValorPrevisto());
        }
        return resultado;
    }

    // Retorna salários previstos por ano como um mapa (mês -> valor)
    public List<SalarioPrevisto> listarSalariosPrevistos() {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return repository.findByTenantId(tenantId);
    }

    @Transactional
    public SalarioPrevisto atualizarSalario(Long id, SalarioPrevisto salarioPrevisto) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        SalarioPrevisto salarioPrev = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salário Previsto não encontrada: " + id));
        if (!tenantId.equals(salarioPrev.getTenantId())) {
            throw new SecurityException("Acesso negado ao salário previsto de outro tenant.");
        }
        salarioPrev.setAno(salarioPrevisto.getAno());
        salarioPrev.setMes(salarioPrevisto.getMes());
        salarioPrev.setValorPrevisto(salarioPrevisto.getValorPrevisto());
        return repository.save(salarioPrev);
    }

    @Transactional
    public void deletarSalario(Long id) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        SalarioPrevisto salarioPrev = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Salário Previsto não encontrada: " + id));
        if (!tenantId.equals(salarioPrev.getTenantId())) {
            throw new SecurityException("Acesso negado ao salário previsto de outro tenant.");
        }
        repository.delete(salarioPrev);
    }
}
