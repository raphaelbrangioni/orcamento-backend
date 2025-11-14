package com.example.orcamento.controller;

import com.example.orcamento.dto.TipoDespesaRequestDTO;
import com.example.orcamento.dto.TipoDespesaResponseDTO;
import com.example.orcamento.dto.TipoDespesaCategoriaResponseDTO;
import com.example.orcamento.dto.TipoDespesaSubcategoriaResponseDTO;
import com.example.orcamento.dto.TipoDespesaCategoriaRequestDTO;
import com.example.orcamento.model.CategoriaDespesa;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.model.TipoDespesa;
import com.example.orcamento.security.TenantContext;
import com.example.orcamento.service.TipoDespesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tipos-despesa_XX")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:8080")
@CrossOrigin(origins = "http://localhost")
@Slf4j
public class TipoDespesaController {

    private final TipoDespesaService tipoDespesaService;

    @GetMapping
    public ResponseEntity<List<TipoDespesaCategoriaResponseDTO>> listarCategoriasComSubcategorias() {
        log.info("Requisição GET em /api/v1/tipos-despesa, listarCategoriasComSubcategorias");
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<TipoDespesaCategoriaResponseDTO> resposta = tipoDespesaService.listarCategoriasComSubcategoriasPorTenant(tenantId);
        return ResponseEntity.ok(resposta);
    }

    @PostMapping
    public ResponseEntity<TipoDespesaCategoriaResponseDTO> criarCategoria(@Valid @RequestBody TipoDespesaCategoriaRequestDTO dto) {
        log.info("Requisição POST em /api/v1/tipos-despesa, criarCategoria");
        String tenantId = TenantContext.getTenantId();
        TipoDespesaCategoriaResponseDTO resposta = tipoDespesaService.criarCategoriaComSubcategorias(dto, tenantId);
        return ResponseEntity.ok(resposta);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirTipo(@PathVariable Long id) {
        log.info("Requisição DELETE em /api/v1/tipos-despesa/{id}, excluirTipo");
        tipoDespesaService.excluirTipo(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoDespesaResponseDTO> atualizarTipoDespesa(
            @PathVariable Long id,
            @RequestBody @Valid TipoDespesaRequestDTO dto) {
        TipoDespesa atualizado = tipoDespesaService.atualizarTipoDespesaDTO(id, dto);
        return ResponseEntity.ok(tipoDespesaService.toResponseDTO(atualizado));
    }
}
