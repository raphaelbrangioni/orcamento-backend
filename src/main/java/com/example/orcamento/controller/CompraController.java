// src/main/java/com/example/orcamento/controller/CompraController.java
package com.example.orcamento.controller;

import com.example.orcamento.dto.CompraDTO;
import com.example.orcamento.dto.CompraTerceiroDTO;
import com.example.orcamento.model.*;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.PessoaRepository;
import com.example.orcamento.mapper.CompraMapper;
import com.example.orcamento.repository.SubcategoriaDespesaRepository;
import com.example.orcamento.service.CompraService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/compras")
@Slf4j
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;
    private final CartaoCreditoRepository cartaoCreditoRepository;
    private final SubcategoriaDespesaRepository subcategoriaDespesaRepository;
    private final PessoaRepository pessoaRepository;
    private final CompraMapper compraMapper;

    @PostMapping("/parceladas")
    public ResponseEntity<CompraDTO> cadastrarCompraParcelada(
            @RequestBody CompraDTO compraDTO,
            @RequestParam String mesPrimeiraParcela,
            @RequestParam Integer numeroParcelas) {
        log.info("Requisição POST em /api/v1/compras/parceladas, compra: {}, mesPrimeiraParcela: {}, numeroParcelas: {}", compraDTO, mesPrimeiraParcela, numeroParcelas);
        Compra compra = toEntity(compraDTO);
        Compra novaCompra = compraService.cadastrarCompraParcelada(compra, mesPrimeiraParcela, numeroParcelas);
        return ResponseEntity.status(HttpStatus.CREATED).body(compraMapper.toDto(novaCompra));
    }

    @GetMapping("/ultimas")
    public ResponseEntity<Page<CompraDTO>> listarUltimasCompras(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) Long cartaoId,
            @RequestParam(required = false) Long subcategoriaId) {
        log.info("Requisição GET em /api/v1/compras/ultimas, page: {}, size: {}, descricao: {}, cartaoId: {}, subcategoriaId: {}", page, size, descricao, cartaoId, subcategoriaId);
        Page<Compra> ultimasCompras = compraService.listarCompras(page, size, descricao, cartaoId, subcategoriaId);
        return ResponseEntity.ok(ultimasCompras.map(compraMapper::toDto));
    }

    // Novo endpoint para editar
    @PutMapping("/{id}")
    public ResponseEntity<CompraDTO> editarCompra(
            @PathVariable Long id,
            @RequestBody CompraDTO compraDTO,
            @RequestParam String mesPrimeiraParcela,
            @RequestParam Integer numeroParcelas) {
        Compra compraAtualizada = toEntity(compraDTO);
        Compra compra = compraService.editarCompra(id, compraAtualizada, mesPrimeiraParcela, numeroParcelas);
        return ResponseEntity.ok(compraMapper.toDto(compra));
    }

    // Novo endpoint para excluir
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirCompra(@PathVariable Long id) {
        compraService.excluirCompra(id);
        return ResponseEntity.noContent().build();
    }

    // Endpoint ajustado para suportar filtros e paginação
    @GetMapping
    public ResponseEntity<Page<CompraDTO>> listarCompras(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) Long cartaoId,
            @RequestParam(required = false) Long subcategoriaId) {
        Page<Compra> comprasPage = compraService.listarCompras(page, size, descricao, cartaoId, subcategoriaId);
        Page<CompraDTO> comprasDtoPage = comprasPage.map(compraMapper::toDto);
        return ResponseEntity.ok(comprasDtoPage);
    }


    private Compra toEntity(CompraDTO dto) {
        Compra compra = new Compra();
        compra.setId(dto.getId());
        compra.setDescricao(dto.getDescricao());
        compra.setValorTotal(dto.getValorTotal());
        compra.setNumeroParcelas(dto.getNumeroParcelas());
        compra.setDataCompra(dto.getDataCompra());
        compra.setProprietario(dto.getProprietario());
        compra.setDetalhes(dto.getDetalhes());

        if (dto.getClassificacao() != null) {
            compra.setClassificacao(TipoClassificacaoDespesa.valueOf(dto.getClassificacao()));
        }
        if (dto.getVariabilidade() != null) {
            compra.setVariabilidade(TipoVariabilidadeDespesa.valueOf(dto.getVariabilidade()));
        }

        CartaoCredito cartao = cartaoCreditoRepository.findById(dto.getCartaoCreditoId())
                .orElseThrow(() -> new EntityNotFoundException("Cartão de crédito não encontrado"));
        compra.setCartaoCredito(cartao);

        SubcategoriaDespesa subcategoria = subcategoriaDespesaRepository.findById(dto.getSubcategoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Subcategoria não encontrada"));
        compra.setSubcategoria(subcategoria);

        if (dto.getTerceiros() != null) {
            compra.setTerceiros(new HashSet<>());
            dto.getTerceiros().forEach(terceiroDTO -> {
                Pessoa pessoa = pessoaRepository.findById(terceiroDTO.getPessoaId())
                        .orElseThrow(() -> new EntityNotFoundException("Pessoa não encontrada: " + terceiroDTO.getPessoaId()));

                CompraTerceiro compraTerceiro = new CompraTerceiro();
                compraTerceiro.setCompra(compra);
                compraTerceiro.setPessoa(pessoa);
                compraTerceiro.setId(new CompraTerceiroId(compra.getId(), pessoa.getId()));
                compraTerceiro.setPercentual(terceiroDTO.getPercentual());

                compra.getTerceiros().add(compraTerceiro);
            });
        }

        return compra;
    }
}