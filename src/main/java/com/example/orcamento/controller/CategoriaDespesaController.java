package com.example.orcamento.controller;

import com.example.orcamento.dto.TipoDespesaCategoriaResponseDTO;
import com.example.orcamento.model.CategoriaDespesa;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.model.TipoDespesa;
import com.example.orcamento.repository.CategoriaDespesaRepository;
import com.example.orcamento.repository.SubcategoriaDespesaRepository;
import com.example.orcamento.repository.TipoDespesaRepository;
import com.example.orcamento.security.TenantContext;
import com.example.orcamento.service.TipoDespesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tipos-despesa")
@RequiredArgsConstructor
@Slf4j
public class CategoriaDespesaController {
    private final CategoriaDespesaRepository categoriaDespesaRepository;
    private final SubcategoriaDespesaRepository subcategoriaDespesaRepository;
    private final TipoDespesaRepository tipoDespesaRepository;
    private final TipoDespesaService tipoDespesaService;

    @GetMapping
    public ResponseEntity<List<TipoDespesaCategoriaResponseDTO>> listarCategorias() {
        log.info("GET /api/v1/categorias");
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(tipoDespesaService.listarCategoriasComSubcategoriasPorTenant(tenantId));
    }

    @PostMapping
    public ResponseEntity<CategoriaDespesa> criarCategoria(@RequestBody @Valid CategoriaDespesa categoria) {
        log.info("POST /api/v1/categorias");
        categoria.setTenantId(TenantContext.getTenantId());
        return ResponseEntity.ok(categoriaDespesaRepository.save(categoria));
    }

    @PostMapping("/{categoriaId}/subcategorias")
    public ResponseEntity<SubcategoriaDespesa> criarSubcategoria(@PathVariable Long categoriaId, @RequestBody @Valid SubcategoriaDespesa subcategoria) {
        log.info("POST /api/v1/categorias/{}/subcategorias", categoriaId);
        log.info("Subcategoria: {}", subcategoria);

        String tenantId = TenantContext.getTenantId();

        CategoriaDespesa categoria = categoriaDespesaRepository.findByIdAndTenantId(categoriaId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria nao encontrada"));

        Long maiorId = subcategoriaDespesaRepository.findMaxIdByCategoriaId(categoriaId);
        Long novoId = (maiorId == null) ? (categoriaId * 100 + 1) : (maiorId + 1);
        subcategoria.setId(novoId);
        subcategoria.setCategoria(categoria);
        subcategoria.setTenantId(tenantId);
        SubcategoriaDespesa savedSubcat = subcategoriaDespesaRepository.save(subcategoria);

        TipoDespesa tipo = new TipoDespesa();
        tipo.setId(savedSubcat.getId());
        tipo.setNome(savedSubcat.getNome());
        tipo.setSubcategoria(savedSubcat);
        tipo.setTenantId(tenantId);
        tipoDespesaRepository.save(tipo);

        return ResponseEntity.ok(savedSubcat);
    }

    @GetMapping("/{categoriaId}/subcategorias")
    public ResponseEntity<List<SubcategoriaDespesa>> listarSubcategorias(@PathVariable Long categoriaId) {
        log.info("GET /api/v1/categorias/{}/subcategorias", categoriaId);
        String tenantId = TenantContext.getTenantId();
        CategoriaDespesa categoria = categoriaDespesaRepository.findByIdAndTenantId(categoriaId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria nao encontrada"));
        return ResponseEntity.ok(categoria.getSubcategorias());
    }
}
