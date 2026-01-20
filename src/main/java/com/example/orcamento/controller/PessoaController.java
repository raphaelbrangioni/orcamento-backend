package com.example.orcamento.controller;

import com.example.orcamento.dto.PessoaDTO;
import com.example.orcamento.model.Pessoa;
import com.example.orcamento.service.PessoaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/pessoas")
@RequiredArgsConstructor
public class PessoaController {

    private final PessoaService service;

    // Mapper methods (idealmente em uma classe @Mapper separada)
    private PessoaDTO toDto(Pessoa pessoa) {
        PessoaDTO dto = new PessoaDTO();
        dto.setId(pessoa.getId());
        dto.setNome(pessoa.getNome());
        dto.setObservacao(pessoa.getObservacao());
        dto.setAtivo(pessoa.isAtivo());
        return dto;
    }

    private Pessoa toEntity(PessoaDTO dto) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(dto.getId());
        pessoa.setNome(dto.getNome());
        pessoa.setObservacao(dto.getObservacao());
        pessoa.setAtivo(dto.isAtivo());
        return pessoa;
    }

    @GetMapping
    public ResponseEntity<List<PessoaDTO>> getAll() {
        List<PessoaDTO> pessoas = service.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pessoas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PessoaDTO> getById(@PathVariable Long id) {
        Pessoa pessoa = service.findById(id);
        return ResponseEntity.ok(toDto(pessoa));
    }

    @PostMapping
    public ResponseEntity<PessoaDTO> create(@RequestBody PessoaDTO pessoaDTO) {
        Pessoa pessoa = toEntity(pessoaDTO);
        Pessoa novaPessoa = service.save(pessoa);
        return new ResponseEntity<>(toDto(novaPessoa), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PessoaDTO> update(@PathVariable Long id, @RequestBody PessoaDTO pessoaDTO) {
        Pessoa pessoa = toEntity(pessoaDTO);
        Pessoa pessoaAtualizada = service.update(id, pessoa);
        return ResponseEntity.ok(toDto(pessoaAtualizada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/inativar")
    public ResponseEntity<PessoaDTO> inativar(@PathVariable Long id) {
        Pessoa pessoaInativada = service.inativar(id);
        return ResponseEntity.ok(toDto(pessoaInativada));
    }
}
