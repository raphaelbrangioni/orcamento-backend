package com.example.orcamento.controller;

import com.example.orcamento.dto.ContaCorrenteSaldoDiaResponseDTO;
import com.example.orcamento.dto.FecharSaldoDiaRequestDTO;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.service.ContaCorrenteSaldoDiaService;
import com.example.orcamento.service.ContaCorrenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/contas-corrente")
@RequiredArgsConstructor
public class ContaCorrenteController {

    private final ContaCorrenteService contaCorrenteService;
    private final ContaCorrenteSaldoDiaService contaCorrenteSaldoDiaService;

    @PostMapping
    public ResponseEntity<ContaCorrente> criarConta(@RequestBody ContaCorrente conta) {
        ContaCorrente novaConta = contaCorrenteService.salvar(conta);
        return ResponseEntity.ok(novaConta);
    }

    @GetMapping
    public ResponseEntity<List<ContaCorrente>> listarContas() {
        List<ContaCorrente> contas = contaCorrenteService.listarTodos();
        return ResponseEntity.ok(contas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContaCorrente> buscarContaPorId(@PathVariable Long id) {
        return contaCorrenteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarConta(@PathVariable Long id) {
        contaCorrenteService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContaCorrente> atualizarConta(@PathVariable Long id, @RequestBody ContaCorrente contaAtualizada) {
        return ResponseEntity.ok(contaCorrenteService.atualizarConta(id, contaAtualizada));
    }

    @GetMapping("/{id}/saldo-dia/periodo")
    public ResponseEntity<List<ContaCorrenteSaldoDiaResponseDTO>> listarSaldoDiaPorPeriodo(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return ResponseEntity.ok(contaCorrenteSaldoDiaService.listarPorPeriodo(id, dataInicio, dataFim));
    }

    @PostMapping("/{id}/saldo-dia/fechar")
    public ResponseEntity<ContaCorrenteSaldoDiaResponseDTO> fecharSaldoDia(
            @PathVariable Long id,
            @RequestBody FecharSaldoDiaRequestDTO request
    ) {
        return ResponseEntity.ok(contaCorrenteSaldoDiaService.fecharDia(id, request));
    }
}
