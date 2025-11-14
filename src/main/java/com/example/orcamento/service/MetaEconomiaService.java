package com.example.orcamento.service;

import com.example.orcamento.dto.MetaEconomiaRequestDTO;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.MetaEconomia;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.MetaEconomiaRepository;
import com.example.orcamento.repository.SubcategoriaDespesaRepository;
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

    @Autowired
    private SubcategoriaDespesaRepository subcategoriaRepository;

    public List<MetaEconomia> listarMetas() {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return repository.findByTenantId(tenantId);
    }

    public MetaEconomia salvarMeta(MetaEconomia meta) {
        meta.setTenantId(com.example.orcamento.security.TenantContext.getTenantId());
        return repository.save(meta);
    }

    @Transactional
    public MetaEconomia salvarMeta(MetaEconomiaRequestDTO dto) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();

        SubcategoriaDespesa tipoInvestimento = subcategoriaRepository.findById(dto.getTipoInvestimentoId())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de investimento (Subcategoria) não encontrado com ID: " + dto.getTipoInvestimentoId()));

        MetaEconomia meta = new MetaEconomia();
        meta.setNome(dto.getNome());
        meta.setValor(dto.getValor());
        meta.setDataFinal(dto.getDataFinal());
        meta.setValorEconomizado(dto.getValorEconomizado());
        meta.setFracaoCripto(dto.getFracaoBitcoin());
        meta.setSimboloCripto(dto.getSimboloCripto());
        meta.setTipoInvestimento(tipoInvestimento);
        meta.setTenantId(tenantId);

        return repository.save(meta);
    }

    @Transactional
    public MetaEconomia atualizarMeta(Long id, MetaEconomiaRequestDTO dto) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();

        MetaEconomia metaExistente = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Meta não encontrada com ID: " + id));

        SubcategoriaDespesa tipoInvestimento = subcategoriaRepository.findById(dto.getTipoInvestimentoId())
                .orElseThrow(() -> new EntityNotFoundException("Tipo de investimento (Subcategoria) não encontrado com ID: " + dto.getTipoInvestimentoId()));

        metaExistente.setNome(dto.getNome());
        metaExistente.setValor(dto.getValor());
        metaExistente.setDataFinal(dto.getDataFinal());
        metaExistente.setValorEconomizado(dto.getValorEconomizado());
        metaExistente.setFracaoCripto(dto.getFracaoBitcoin());
        metaExistente.setSimboloCripto(dto.getSimboloCripto());
        metaExistente.setTipoInvestimento(tipoInvestimento);
        metaExistente.setFracaoCripto(dto.getFracaoCripto());

        return repository.save(metaExistente);
    }

    public void excluirMeta(Long id) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        repository.deleteByIdAndTenantId(id, tenantId);
    }

    public Optional<MetaEconomia> buscarPorId(Long id) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return repository.findByIdAndTenantId(id, tenantId);
    }

    @Transactional
    public MetaEconomia atualizarFracaoBitcoin(Long id, Double fracaoBitcoin) {
        MetaEconomia meta = repository.findByIdAndTenantId(id, com.example.orcamento.security.TenantContext.getTenantId())
                .orElseThrow(() -> new EntityNotFoundException("Meta não encontrada: " + id));
        meta.setFracaoCripto(fracaoBitcoin);
        return repository.save(meta);
    }

    // No MetaEconomiaService.java
    @Transactional
    public int desassociarDespesas(Long metaId) {
        // Verificar se a meta existe
        MetaEconomia meta = buscarPorId(metaId)
                .orElseThrow(() -> new EntityNotFoundException("Meta não encontrada com ID: " + metaId));

        // Buscar todas as despesas associadas a esta meta
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<Despesa> despesas = despesaRepository.findByMetaEconomiaId(tenantId, metaId);

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
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<Despesa> despesas = despesaRepository.findByMetaEconomiaId(tenantId, metaId);

        // Remover a associação
        for (Despesa despesa : despesas) {
            despesa.setMetaEconomia(null);
            despesaRepository.save(despesa);
        }

        // Excluir a meta
        repository.deleteByIdAndTenantId(metaId, tenantId);
    }
}