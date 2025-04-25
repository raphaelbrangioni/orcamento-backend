package com.example.orcamento.controller;

import com.example.orcamento.model.SalarioPrevisto;
import com.example.orcamento.service.SalarioPrevistoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/salarios-previstos")
public class SalarioPrevistoController {

    @Autowired
    private SalarioPrevistoService service;

    @Operation(summary = "Cadastrar Salário Previsto",
            description = "Cadastra um novo salário previsto no sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Salário cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro na requisição")
    })
    @PostMapping
    public ResponseEntity<SalarioPrevisto> cadastrarSalario(@RequestBody SalarioPrevisto salarioPrevisto) {
        SalarioPrevisto salvo = service.salvar(salarioPrevisto);
        return ResponseEntity.ok(salvo);
    }

    @Operation(summary = "Listar Salários Previsto por Ano",
            description = "Lista todos os salários previstos por ano.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de salários retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ano não encontrado")
    })
    @GetMapping("/por-ano/{ano}")
    public ResponseEntity<Map<String, Double>> listarPorAno(
            @Parameter(description = "Ano para listar os salários previstos", example = "2024")
            @PathVariable int ano) {
        Map<String, Double> salarios = service.getSalariosPrevistosPorAno(ano);
        return ResponseEntity.ok(salarios);
    }

    @Operation(summary = "Listar Todos os Salários Previstos",
            description = "Retorna todos os salários previstos registrados no sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de salários prevista retornada com sucesso")
    })
    @GetMapping
    public ResponseEntity<List<SalarioPrevisto>> listarSalarioPrevisto() {
        List<SalarioPrevisto> salarios = service.listarSalariosPrevistos();
        return ResponseEntity.ok(salarios);
    }

    @Operation(summary = "Atualizar Salário Previsto",
            description = "Atualiza as informações de um salário previsto existente no sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Salário atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Salário previsto não encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<SalarioPrevisto> atualizarConta(
            @Parameter(description = "ID do salário previsto a ser atualizado", example = "1")
            @PathVariable Long id,
            @RequestBody SalarioPrevisto salarioAtualizado) {
        return ResponseEntity.ok(service.atualizarSalario(id, salarioAtualizado));
    }

    @Operation(summary = "Deletar um Salário Previsto",
            description = "Deleta um salário previsto existente no sistema.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarSalario(
            @Parameter(description = "ID do salário previsto a ser deletado", example = "1")
            @PathVariable Long id) {


        service.deletarSalario(id);
        return ResponseEntity.noContent().build();
    }
}