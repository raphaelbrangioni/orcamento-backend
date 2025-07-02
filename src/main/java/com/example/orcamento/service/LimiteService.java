package com.example.orcamento.service;

import com.example.orcamento.model.Limite;
import com.example.orcamento.repository.LimiteRepository;
import com.example.orcamento.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LimiteService {

    @Autowired
    private LimiteRepository limiteRepository;

    public Limite salvarLimite(Limite limite) {
        limite.setTenantId(TenantContext.getTenantId());
        return limiteRepository.save(limite);
    }

    public List<Limite> listarUltimos10Limites() {
        String tenantId = TenantContext.getTenantId();
        return limiteRepository.findTop10ByTenantIdOrderByIdDesc(tenantId);
    }

    public List<Limite> listarPorTipoDespesa(Long tipoDespesaId) {
        String tenantId = TenantContext.getTenantId();
        return limiteRepository.findByTipoDespesaIdAndTenantId(tipoDespesaId, tenantId);
    }

    public void deletarLimite(Long id) {
        String tenantId = TenantContext.getTenantId();
        limiteRepository.deleteByIdAndTenantId(id, tenantId);
    }
}