package com.example.orcamento.controller;

import com.example.orcamento.model.CategoriaDespesa;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.model.TipoDespesa;
import com.example.orcamento.repository.CategoriaDespesaRepository;
import com.example.orcamento.repository.SubcategoriaDespesaRepository;
import com.example.orcamento.repository.TipoDespesaRepository;
import com.example.orcamento.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tipos-despesa")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost")
public class CategoriaDespesaController {
    private final CategoriaDespesaRepository categoriaDespesaRepository;
    private final SubcategoriaDespesaRepository subcategoriaDespesaRepository;
    private final TipoDespesaRepository tipoDespesaRepository;

    @GetMapping
    public ResponseEntity<List<CategoriaDespesa>> listarCategorias() {
        log.info("GET /api/v1/categorias");
        return ResponseEntity.ok(categoriaDespesaRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<CategoriaDespesa> criarCategoria(@RequestBody @Valid CategoriaDespesa categoria) {
        log.info("POST /api/v1/categorias");
        return ResponseEntity.ok(categoriaDespesaRepository.save(categoria));
    }

    @PostMapping("/{categoriaId}/subcategorias")
    public ResponseEntity<SubcategoriaDespesa> criarSubcategoria(@PathVariable Long categoriaId, @RequestBody @Valid SubcategoriaDespesa subcategoria) {
        log.info("POST /api/v1/categorias/{}/subcategorias", categoriaId);
        log.info("Subcategoria: {}", subcategoria);

        String tenantId = TenantContext.getTenantId();

        CategoriaDespesa categoria = categoriaDespesaRepository.findById(categoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));

        // Buscar maior ID existente para essa categoria
        Long maiorId = subcategoriaDespesaRepository.findMaxIdByCategoriaId(categoriaId);
        Long novoId = (maiorId == null) ? (categoriaId * 100 + 1) : (maiorId + 1);
        subcategoria.setId(novoId);
        subcategoria.setCategoria(categoria);
        subcategoria.setTenantId(tenantId); // Definir o tenantId na subcategoria
        SubcategoriaDespesa savedSubcat = subcategoriaDespesaRepository.save(subcategoria);

        // Criar TipoDespesa correspondente para compatibilidade
        TipoDespesa tipo = new TipoDespesa();
        tipo.setId(savedSubcat.getId());
        tipo.setNome(savedSubcat.getNome());
        tipo.setSubcategoria(savedSubcat);
        tipo.setTenantId(tenantId); // Definir o tenantId no tipo de despesa
        tipoDespesaRepository.save(tipo);

        return ResponseEntity.ok(savedSubcat);
    }

    @GetMapping("/{categoriaId}/subcategorias")
    public ResponseEntity<List<SubcategoriaDespesa>> listarSubcategorias(@PathVariable Long categoriaId) {
        log.info("GET /api/v1/categorias/{}/subcategorias", categoriaId);
        CategoriaDespesa categoria = categoriaDespesaRepository.findById(categoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));
        return ResponseEntity.ok(categoria.getSubcategorias());
    }
}
