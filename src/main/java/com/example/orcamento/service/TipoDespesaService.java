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
        return tipoDespesaRepository.findAll();
    }

    public TipoDespesa cadastrarTipo(TipoDespesa tipo) {
        if (tipoDespesaRepository.existsByNome(tipo.getNome())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de despesa já existe!");
        }
        return tipoDespesaRepository.save(tipo);
    }

    public void excluirTipo(Long id) {
        tipoDespesaRepository.deleteById(id);
    }

    public TipoDespesa atualizarTipoDespesa(Long id, TipoDespesa tipoDespesaAtualizado) {
        // Busca o tipo de despesa no banco, ou lança exceção se não encontrado
        TipoDespesa tipoDespesaExistente = tipoDespesaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de despesa não encontrado para o ID: " + id));

        // Atualiza os campos permitidos
        tipoDespesaExistente.setNome(tipoDespesaAtualizado.getNome());

        // Salva as alterações
        return tipoDespesaRepository.save(tipoDespesaExistente);
    }
}
