package com.example.orcamento.service;

import com.example.orcamento.model.HorasTrabalhadas;
import com.example.orcamento.repository.HorasTrabalhadasRepository;
import com.example.orcamento.security.TenantContext;
import com.example.orcamento.specification.HorasTrabalhadasSpecification;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class HorasTrabalhadasService {

    private final HorasTrabalhadasRepository repository;

    @Transactional
    public HorasTrabalhadas registrar(HorasTrabalhadas horasTrabalhadas) {
        log.info("Registrando novas horas trabalhadas: {}", horasTrabalhadas);
        String tenantId = TenantContext.getTenantId();
        horasTrabalhadas.setTenantId(tenantId);
        return repository.save(horasTrabalhadas);
    }

    public List<HorasTrabalhadas> listar(Map<String, Object> filtros) {
        log.info("Listando horas trabalhadas com filtros: {}", filtros);
        String tenantId = TenantContext.getTenantId();
        filtros.put("tenantId", tenantId);
        return repository.findAll(HorasTrabalhadasSpecification.comFiltros(filtros));
    }

    @Transactional
    public HorasTrabalhadas atualizar(Long id, HorasTrabalhadas horasParaAtualizar) {
        log.info("Atualizando registro de horas com id: {} para: {}", id, horasParaAtualizar);
        String tenantId = TenantContext.getTenantId();
        HorasTrabalhadas horasExistentes = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro de horas não encontrado com o id: " + id));

        if (!horasExistentes.getTenantId().equals(tenantId)) {
            throw new SecurityException("Acesso negado para atualizar este registro.");
        }

        // Atualiza os campos
        horasExistentes.setInicio(horasParaAtualizar.getInicio());
        horasExistentes.setFim(horasParaAtualizar.getFim());
        horasExistentes.setDuracaoMinutos(horasParaAtualizar.getDuracaoMinutos());
        horasExistentes.setDetalhes(horasParaAtualizar.getDetalhes());
        horasExistentes.setClienteId(horasParaAtualizar.getClienteId());
        horasExistentes.setProjetoId(horasParaAtualizar.getProjetoId());
        horasExistentes.setValorHora(horasParaAtualizar.getValorHora());

        return repository.save(horasExistentes);
    }

    @Transactional
    public void excluir(Long id) {
        log.info("Excluindo registro de horas com id: {}", id);
        String tenantId = TenantContext.getTenantId();
        repository.findById(id).ifPresent(horasExistentes -> {
            if (!horasExistentes.getTenantId().equals(tenantId)) {
                throw new SecurityException("Acesso negado para excluir este registro.");
            }
            repository.delete(horasExistentes);
        });
    }
}
