package com.example.orcamento.controller;

import com.example.orcamento.dto.CompraDTO;
import com.example.orcamento.mapper.CompraMapper;
import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.Compra;
import com.example.orcamento.model.CompraTerceiro;
import com.example.orcamento.model.CompraTerceiroId;
import com.example.orcamento.model.Pessoa;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.model.TipoClassificacaoDespesa;
import com.example.orcamento.model.TipoVariabilidadeDespesa;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.PessoaRepository;
import com.example.orcamento.repository.SubcategoriaDespesaRepository;
import com.example.orcamento.security.TenantContext;
import com.example.orcamento.service.CompraService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;

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
        log.info("Requisicao POST em /api/v1/compras/parceladas, compra: {}, mesPrimeiraParcela: {}, numeroParcelas: {}", compraDTO, mesPrimeiraParcela, numeroParcelas);
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
        log.info("Requisicao GET em /api/v1/compras/ultimas, page: {}, size: {}, descricao: {}, cartaoId: {}, subcategoriaId: {}", page, size, descricao, cartaoId, subcategoriaId);
        Page<Compra> ultimasCompras = compraService.listarCompras(page, size, descricao, cartaoId, subcategoriaId);
        return ResponseEntity.ok(ultimasCompras.map(compraMapper::toDto));
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirCompra(@PathVariable Long id) {
        compraService.excluirCompra(id);
        return ResponseEntity.noContent().build();
    }

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
        String tenantId = TenantContext.getTenantId();

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

        CartaoCredito cartao = cartaoCreditoRepository.findByIdAndTenantId(dto.getCartaoCreditoId(), tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Cartao de credito nao encontrado"));
        compra.setCartaoCredito(cartao);

        SubcategoriaDespesa subcategoria = subcategoriaDespesaRepository.findByIdAndTenantId(dto.getSubcategoriaId(), tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Subcategoria nao encontrada"));
        compra.setSubcategoria(subcategoria);

        if (dto.getTerceiros() != null) {
            compra.setTerceiros(new HashSet<>());
            dto.getTerceiros().forEach(terceiroDTO -> {
                Pessoa pessoa = pessoaRepository.findByIdAndTenantId(terceiroDTO.getPessoaId(), tenantId)
                        .orElseThrow(() -> new EntityNotFoundException("Pessoa nao encontrada: " + terceiroDTO.getPessoaId()));

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
