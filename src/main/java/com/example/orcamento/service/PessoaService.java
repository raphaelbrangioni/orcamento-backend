package com.example.orcamento.service;

import com.example.orcamento.model.Pessoa;
import com.example.orcamento.repository.PessoaRepository;
import com.example.orcamento.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PessoaService {

    private final PessoaRepository repository;

    public List<Pessoa> findAll() {
        return repository.findByTenantId(TenantContext.getTenantId());
    }

    public Pessoa findById(Long id) {
        return repository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new EntityNotFoundException("Pessoa nao encontrada com o id: " + id));
    }

    @Transactional
    public Pessoa save(Pessoa pessoa) {
        String tenantId = TenantContext.getTenantId();
        pessoa.setTenantId(tenantId);
        Pessoa pessoaSalva = repository.save(pessoa);
        log.info("pessoa.criada pessoaId={} tenantId={} nome={} ativa={}",
                pessoaSalva.getId(), tenantId, pessoaSalva.getNome(), pessoaSalva.isAtivo());
        return pessoaSalva;
    }

    @Transactional
    public Pessoa update(Long id, Pessoa pessoaDetails) {
        String tenantId = TenantContext.getTenantId();
        Pessoa pessoa = findById(id);
        pessoa.setNome(pessoaDetails.getNome());
        pessoa.setObservacao(pessoaDetails.getObservacao());
        pessoa.setAtivo(pessoaDetails.isAtivo());
        Pessoa pessoaSalva = repository.save(pessoa);
        log.info("pessoa.atualizada pessoaId={} tenantId={} nome={} ativa={}",
                pessoaSalva.getId(), tenantId, pessoaSalva.getNome(), pessoaSalva.isAtivo());
        return pessoaSalva;
    }

    @Transactional
    public void delete(Long id) {
        Pessoa pessoa = findById(id);
        String tenantId = TenantContext.getTenantId();
        repository.delete(pessoa);
        log.info("pessoa.excluida pessoaId={} tenantId={} nome={}", id, tenantId, pessoa.getNome());
    }

    @Transactional
    public Pessoa inativar(Long id) {
        String tenantId = TenantContext.getTenantId();
        Pessoa pessoa = findById(id);
        pessoa.setAtivo(false);
        Pessoa pessoaSalva = repository.save(pessoa);
        log.info("pessoa.inativada pessoaId={} tenantId={} nome={}",
                pessoaSalva.getId(), tenantId, pessoaSalva.getNome());
        return pessoaSalva;
    }
}
