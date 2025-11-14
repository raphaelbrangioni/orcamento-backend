package com.example.orcamento.controller;

import com.example.orcamento.dto.dashboard.FaturaCartaoAnualDTO;
import com.example.orcamento.dto.LancamentoCartaoResponseDTO;
import com.example.orcamento.dto.CategoriaDespesaDTO;
import com.example.orcamento.dto.LancamentoCartaoComCompraDTO;
import com.example.orcamento.dto.LancamentoCartaoDetalhadoDTO;
import com.example.orcamento.dto.SubcategoriaDespesaDTO;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.model.CategoriaDespesa;
import com.example.orcamento.service.LancamentoCartaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/lancamentos-cartao")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost")
@Slf4j
public class LancamentoCartaoController {
    private final LancamentoCartaoService lancamentoCartaoService;

    @GetMapping
    public ResponseEntity<List<LancamentoCartaoResponseDTO>> listarLancamentos() {
        log.info("Requisição GET em /api/v1/lancamentos-cartao, listarLancamentos");
        List<LancamentoCartaoResponseDTO> resposta = lancamentoCartaoService.listarLancamentos().stream()
            .map(this::toLancamentoCartaoResponseDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/com-compra")
    public ResponseEntity<List<LancamentoCartaoComCompraDTO>> listarLancamentosComCompra() {
        log.info("Requisição GET em /api/v1/lancamentos-cartao/com-compra");
        List<LancamentoCartaoComCompraDTO> resposta = lancamentoCartaoService.listarLancamentosComCompra();
        return ResponseEntity.ok(resposta);
    }

    private LancamentoCartaoResponseDTO toLancamentoCartaoResponseDTO(LancamentoCartao lancamento) {
        SubcategoriaDespesa tipo = lancamento.getSubcategoria();
        SubcategoriaDespesa subcat = tipo != null ? lancamento.getSubcategoria() : null;
        CategoriaDespesa cat = subcat != null ? subcat.getCategoria() : null;
        return LancamentoCartaoResponseDTO.builder()
            .id(lancamento.getId())
            .descricao(lancamento.getDescricao())
            .valorTotal(lancamento.getValorTotal())
            .parcelaAtual(lancamento.getParcelaAtual())
            .totalParcelas(lancamento.getTotalParcelas())
            .dataCompra(lancamento.getDataCompra())
            .detalhes(lancamento.getDetalhes())
            .mesAnoFatura(lancamento.getMesAnoFatura())
            .cartaoCreditoId(lancamento.getCartaoCredito() != null ? lancamento.getCartaoCredito().getId() : null)
            .categoria(cat != null ? new CategoriaDespesaDTO(cat.getId(), cat.getNome(), subcat != null ? new SubcategoriaDespesaDTO(subcat.getId(), subcat.getNome()) : null) : null)
            .proprietario(lancamento.getProprietario())
            .tenantId(lancamento.getTenantId())
            .dataRegistro(lancamento.getDataRegistro() != null ? lancamento.getDataRegistro().toLocalDate() : null)
            .pagoPorTerceiro(lancamento.getPagoPorTerceiro())
            .classificacao(lancamento.getClassificacao() != null ? lancamento.getClassificacao().name() : null)
            .variabilidade(lancamento.getVariabilidade() != null ? lancamento.getVariabilidade().name() : null)
            .build();
    }

    @GetMapping("/filtrar")
    public ResponseEntity<List<LancamentoCartao>> listarLancamentosPorFiltros(
            @RequestParam(required = false) Long cartaoCreditoId,
            @RequestParam(required = false) String mesAnoFatura) {
        log.info("Requisição GET em /api/v1/lancamentos-cartao/filtrar com filtros - cartaoId: {}, mesAnoFatura: {}", cartaoCreditoId, mesAnoFatura);

        List<LancamentoCartao> lancamentos = lancamentoCartaoService.listarLancamentosPorFiltros(cartaoCreditoId, mesAnoFatura);
        return ResponseEntity.ok(lancamentos);
    }

    @GetMapping("/filtrar-dinamico")
    public ResponseEntity<List<LancamentoCartao>> listarLancamentosPorFiltrosDinamicos(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) Integer parcelaAtual,
            @RequestParam(required = false) Integer totalParcelas,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCompraInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataCompraFinal,
            @RequestParam(required = false) String mesAnoFatura,
            @RequestParam(required = false) Long cartaoCreditoId,
            @RequestParam(required = false) Long tipoDespesaId,
            @RequestParam(required = false) String proprietario,
            @RequestParam(required = false) String dataRegistro,
            @RequestParam(required = false) Long compraId,
            @RequestParam(required = false) Boolean pagoPorTerceiro,
            @RequestParam(required = false) String classificacao,
            @RequestParam(required = false) String variabilidade) {
        log.info("Requisição GET em /api/v1/lancamentos-cartao/filtrar-dinamico com filtros");

        Map<String, Object> filtros = new HashMap<>();
        if (id != null) filtros.put("id", id);
        if (descricao != null) filtros.put("descricao", descricao);
        if (parcelaAtual != null) filtros.put("parcelaAtual", parcelaAtual);
        if (totalParcelas != null) filtros.put("totalParcelas", totalParcelas);
        if (dataCompraInicial != null) filtros.put("dataCompraInicial", dataCompraInicial);
        if (dataCompraFinal != null) filtros.put("dataCompraFinal", dataCompraFinal);
        if (mesAnoFatura != null) filtros.put("mesAnoFatura", mesAnoFatura);
        if (cartaoCreditoId != null) filtros.put("cartaoCreditoId", cartaoCreditoId);
        if (tipoDespesaId != null) filtros.put("tipoDespesaId", tipoDespesaId);
        if (proprietario != null) filtros.put("proprietario", proprietario);
        if (dataRegistro != null) filtros.put("dataRegistro", dataRegistro);
        if (compraId != null) filtros.put("compraId", compraId);
        if (pagoPorTerceiro != null) filtros.put("pagoPorTerceiro", pagoPorTerceiro);
        if (classificacao != null) filtros.put("classificacao", classificacao);
        if (variabilidade != null) filtros.put("variabilidade", variabilidade);

        List<LancamentoCartao> lancamentos = lancamentoCartaoService.listarLancamentosPorFiltrosDinamicos(filtros);
        return ResponseEntity.ok(lancamentos);
    }

    @PostMapping
    public ResponseEntity<LancamentoCartao> cadastrarLancamento(@RequestBody LancamentoCartao lancamento) {
        log.info("Requisição POST em /api/v1/lancamentos-cartao, cadastrarLancamento");
        log.info("Lancamento: {}", lancamento);
        return ResponseEntity.ok(lancamentoCartaoService.cadastrarLancamento(lancamento));
    }

    @PostMapping("/multiplos")
    public ResponseEntity<List<LancamentoCartao>> cadastrarMultiplosLancamentos(@RequestBody List<LancamentoCartao> lancamentos) {
        log.info("Requisição POST em /api/v1/lancamentos-cartao/multiplos, cadastrarMultiplosLancamentos");
        log.info("Payload recebido: {}", lancamentos);
        return ResponseEntity.ok(lancamentoCartaoService.cadastrarMultiplosLancamentos(lancamentos));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LancamentoCartao> atualizarLancamento(@PathVariable Long id, @RequestBody LancamentoCartao lancamento) {
        log.info("Requisição PUT em /api/v1/lancamentos-cartao/{id}, atualizarLancamento");
        log.info("ID informado {}, lançamento: {}", id, lancamento);
        return ResponseEntity.ok(lancamentoCartaoService.atualizarLancamento(id, lancamento));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirLancamento(@PathVariable Long id) {
        log.info("Requisição DELETE em /api/v1/lancamentos-cartao/{id}, excluirLancamento");
        log.info("ID informado para ser deletado {}", id);
        lancamentoCartaoService.excluirLancamento(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/detalhes-compra")
    public ResponseEntity<LancamentoCartaoDetalhadoDTO> getLancamentoComDetalhesDaCompra(@PathVariable Long id) {
        log.info("Requisição GET em /api/v1/lancamentos-cartao/{}/detalhes-compra", id);
        LancamentoCartaoDetalhadoDTO dto = lancamentoCartaoService.buscarLancamentoComCompra(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/faturas-ano")
    public List<FaturaCartaoAnualDTO> getFaturasAnuais(@RequestParam int ano) {
        log.info("Requisição GET em /api/v1/lancamentos-cartao/faturas-ano, ano: {}", ano);
        return lancamentoCartaoService.getFaturasAnuais(ano);
    }

    @GetMapping("/fatura-por-cartao")
    public ResponseEntity<Map<Long, Double>> getFaturaPorCartao(@RequestParam("mesAno") String mesAno) {
        log.info("Requisição GET em /api/v1/lancamentos-cartao/fatura-por-cartao, getFaturaPorCartao");
        List<LancamentoCartao> lancamentos = lancamentoCartaoService.listarLancamentos();
        Map<Long, Double> faturasPorCartao = lancamentos.stream()
                .filter(l -> l.getMesAnoFatura().equals(mesAno))
                .collect(Collectors.groupingBy(
                        l -> l.getCartaoCredito().getId(),
                        Collectors.summingDouble(l -> l.getValorTotal().doubleValue())
                ));
        return ResponseEntity.ok(faturasPorCartao);
    }

    @GetMapping("/terceiros")
    public ResponseEntity<List<LancamentoCartao>> listarLancamentosTerceiros(
            @RequestParam(required = false) String mesAnoFatura) {
        log.info("Requisição GET em /api/v1/lancamentos-cartao/terceiros, mesAnoFatura: {}", mesAnoFatura);
        List<LancamentoCartao> lancamentos = lancamentoCartaoService.listarLancamentosTerceiros(mesAnoFatura);
        return ResponseEntity.ok(lancamentos);
    }
}