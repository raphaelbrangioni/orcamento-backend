package com.example.orcamento.service;

import com.example.orcamento.model.TipoDespesa;
import com.example.orcamento.repository.TipoDespesaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoDespesaService {

    private final TipoDespesaRepository tipoDespesaRepository;

    public List<TipoDespesa> listarTipos() {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return tipoDespesaRepository.findByTenantId(tenantId);
    }

    public TipoDespesa cadastrarTipo(TipoDespesa tipo) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        if (tipoDespesaRepository.existsByNomeAndTenantId(tipo.getNome(), tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de despesa já existe!");
        }
        tipo.setTenantId(tenantId);
        return tipoDespesaRepository.save(tipo);
    }

    public void excluirTipo(Long id) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        tipoDespesaRepository.deleteByIdAndTenantId(id, tenantId);
    }

    public TipoDespesa atualizarTipoDespesa(Long id, TipoDespesa tipoDespesaAtualizado) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        TipoDespesa tipoDespesaExistente = tipoDespesaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de despesa não encontrado para o ID: " + id + " do tenant atual."));

        // Atualiza os campos permitidos
        tipoDespesaExistente.setNome(tipoDespesaAtualizado.getNome());

        // Salva as alterações
        return tipoDespesaRepository.save(tipoDespesaExistente);
    }
}
