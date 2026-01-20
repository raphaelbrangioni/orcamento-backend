package com.example.orcamento.service;

import com.example.orcamento.model.Pessoa;
import com.example.orcamento.repository.PessoaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PessoaService {

    private final PessoaRepository repository;

    public List<Pessoa> findAll() {
        return repository.findAll();
    }

    public Pessoa findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada com o id: " + id));
    }

    @Transactional
    public Pessoa save(Pessoa pessoa) {
        return repository.save(pessoa);
    }

    @Transactional
    public Pessoa update(Long id, Pessoa pessoaDetails) {
        Pessoa pessoa = findById(id);
        pessoa.setNome(pessoaDetails.getNome());
        pessoa.setObservacao(pessoaDetails.getObservacao());
        pessoa.setAtivo(pessoaDetails.isAtivo());
        return repository.save(pessoa);
    }

    @Transactional
    public void delete(Long id) {
        Pessoa pessoa = findById(id);
        repository.delete(pessoa);
    }

    @Transactional
    public Pessoa inativar(Long id) {
        Pessoa pessoa = findById(id);
        pessoa.setAtivo(false);
        return repository.save(pessoa);
    }
}
