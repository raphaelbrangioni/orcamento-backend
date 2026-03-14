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
        log.info("[HTTP] Início GET /api/v1/lancamentos-cartao (listarLancamentos)");
        log.info("[HTTP] Chamando service: lancamentoCartaoService.listarLancamentos()");
        List<LancamentoCartaoResponseDTO> resposta = lancamentoCartaoService.listarLancamentos().stream()
            .map(this::toLancamentoCartaoResponseDTO)
            .collect(Collectors.toList());
        log.info("[HTTP] Sucesso GET /api/v1/lancamentos-cartao: quantidadeRetornada={}", resposta != null ? resposta.size() : 0);
        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/com-compra")
    public ResponseEntity<List<LancamentoCartaoComCompraDTO>> listarLancamentosComCompra() {
        log.info("[HTTP] Início GET /api/v1/lancamentos-cartao/com-compra (listarLancamentosComCompra)");
        log.info("[HTTP] Chamando service: lancamentoCartaoService.listarLancamentosComCompra()");
        List<LancamentoCartaoComCompraDTO> resposta = lancamentoCartaoService.listarLancamentosComCompra();
        log.info("[HTTP] Sucesso GET /api/v1/lancamentos-cartao/com-compra: quantidadeRetornada={}", resposta != null ? resposta.size() : 0);
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
        log.info("[HTTP] Início GET /api/v1/lancamentos-cartao/filtrar (listarLancamentosPorFiltros): cartaoCreditoId={}, mesAnoFatura={}", cartaoCreditoId, mesAnoFatura);
        log.info("[HTTP] Chamando service: lancamentoCartaoService.listarLancamentosPorFiltros(cartaoCreditoId, mesAnoFatura)");

        List<LancamentoCartao> lancamentos = lancamentoCartaoService.listarLancamentosPorFiltros(cartaoCreditoId, mesAnoFatura);
        log.info("[HTTP] Sucesso GET /api/v1/lancamentos-cartao/filtrar: quantidadeRetornada={}", lancamentos != null ? lancamentos.size() : 0);
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
        log.info("[HTTP] Início GET /api/v1/lancamentos-cartao/filtrar-dinamico (listarLancamentosPorFiltrosDinamicos)");

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

        log.info("[HTTP] Filtros aplicados (quantidadeCampos)={}", filtros.size());
        log.info("[HTTP] Chamando service: lancamentoCartaoService.listarLancamentosPorFiltrosDinamicos(filtros)");

        List<LancamentoCartao> lancamentos = lancamentoCartaoService.listarLancamentosPorFiltrosDinamicos(filtros);
        log.info("[HTTP] Sucesso GET /api/v1/lancamentos-cartao/filtrar-dinamico: quantidadeRetornada={}", lancamentos != null ? lancamentos.size() : 0);
        return ResponseEntity.ok(lancamentos);
    }

    @PostMapping
    public ResponseEntity<LancamentoCartao> cadastrarLancamento(@RequestBody LancamentoCartao lancamento) {
        log.info("[HTTP] Início POST /api/v1/lancamentos-cartao (cadastrarLancamento)");
        log.info(
                "[HTTP] Payload (resumo): id={}, cartaoCreditoId={}, mesAnoFatura={}, valorTotal={}, dataCompra={}",
                lancamento != null ? lancamento.getId() : null,
                (lancamento != null && lancamento.getCartaoCredito() != null) ? lancamento.getCartaoCredito().getId() : null,
                lancamento != null ? lancamento.getMesAnoFatura() : null,
                lancamento != null ? lancamento.getValorTotal() : null,
                lancamento != null ? lancamento.getDataCompra() : null
        );
        log.info("[HTTP] Chamando service: lancamentoCartaoService.cadastrarLancamento(lancamento)");
        LancamentoCartao salvo = lancamentoCartaoService.cadastrarLancamento(lancamento);
        log.info("[HTTP] Sucesso POST /api/v1/lancamentos-cartao: idGerado={}", salvo != null ? salvo.getId() : null);
        return ResponseEntity.ok(salvo);
    }

    @PostMapping("/multiplos")
    public ResponseEntity<List<LancamentoCartao>> cadastrarMultiplosLancamentos(@RequestBody List<LancamentoCartao> lancamentos) {
        log.info("[HTTP] Início POST /api/v1/lancamentos-cartao/multiplos (cadastrarMultiplosLancamentos)");
        log.info("[HTTP] Payload (resumo): quantidadeRecebida={}", lancamentos != null ? lancamentos.size() : 0);
        log.info("[HTTP] Chamando service: lancamentoCartaoService.cadastrarMultiplosLancamentos(lancamentos)");
        List<LancamentoCartao> salvos = lancamentoCartaoService.cadastrarMultiplosLancamentos(lancamentos);
        log.info("[HTTP] Sucesso POST /api/v1/lancamentos-cartao/multiplos: quantidadeSalva={}", salvos != null ? salvos.size() : 0);
        return ResponseEntity.ok(salvos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LancamentoCartao> atualizarLancamento(@PathVariable Long id, @RequestBody LancamentoCartao lancamento) {
        log.info("[HTTP] Início PUT /api/v1/lancamentos-cartao/{} (atualizarLancamento)", id);
        log.info(
                "[HTTP] Payload (resumo): idPath={}, idBody={}, cartaoCreditoId={}, mesAnoFatura={}, valorTotal={}, dataCompra={}",
                id,
                lancamento != null ? lancamento.getId() : null,
                (lancamento != null && lancamento.getCartaoCredito() != null) ? lancamento.getCartaoCredito().getId() : null,
                lancamento != null ? lancamento.getMesAnoFatura() : null,
                lancamento != null ? lancamento.getValorTotal() : null,
                lancamento != null ? lancamento.getDataCompra() : null
        );
        log.info("[HTTP] Chamando service: lancamentoCartaoService.atualizarLancamento(id, lancamento)");
        LancamentoCartao atualizado = lancamentoCartaoService.atualizarLancamento(id, lancamento);
        log.info("[HTTP] Sucesso PUT /api/v1/lancamentos-cartao/{}: idRetornado={}", id, atualizado != null ? atualizado.getId() : null);
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirLancamento(@PathVariable Long id) {
        log.info("[HTTP] Início DELETE /api/v1/lancamentos-cartao/{} (excluirLancamento)", id);
        log.info("[HTTP] Chamando service: lancamentoCartaoService.excluirLancamento(id)");
        lancamentoCartaoService.excluirLancamento(id);
        log.info("[HTTP] Sucesso DELETE /api/v1/lancamentos-cartao/{} (204 No Content)", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/detalhes-compra")
    public ResponseEntity<LancamentoCartaoDetalhadoDTO> getLancamentoComDetalhesDaCompra(@PathVariable Long id) {
        log.info("[HTTP] Início GET /api/v1/lancamentos-cartao/{}/detalhes-compra (getLancamentoComDetalhesDaCompra)", id);
        log.info("[HTTP] Chamando service: lancamentoCartaoService.buscarLancamentoComCompra(id)");
        LancamentoCartaoDetalhadoDTO dto = lancamentoCartaoService.buscarLancamentoComCompra(id);
        log.info("[HTTP] Sucesso GET /api/v1/lancamentos-cartao/{}/detalhes-compra", id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/faturas-ano")
    public List<FaturaCartaoAnualDTO> getFaturasAnuais(@RequestParam int ano) {
        log.info("[HTTP] Início GET /api/v1/lancamentos-cartao/faturas-ano (getFaturasAnuais): ano={}", ano);
        log.info("[HTTP] Chamando service: lancamentoCartaoService.getFaturasAnuais(ano)");
        List<FaturaCartaoAnualDTO> resposta = lancamentoCartaoService.getFaturasAnuais(ano);
        log.info("[HTTP] Sucesso GET /api/v1/lancamentos-cartao/faturas-ano: quantidadeRetornada={}", resposta != null ? resposta.size() : 0);
        return resposta;
    }

    @GetMapping("/fatura-por-cartao")
    public ResponseEntity<Map<Long, Double>> getFaturaPorCartao(@RequestParam("mesAno") String mesAno) {
        log.info("[HTTP] Início GET /api/v1/lancamentos-cartao/fatura-por-cartao (getFaturaPorCartao): mesAno={}", mesAno);
        log.info("[HTTP] Chamando service: lancamentoCartaoService.listarLancamentos()");
        List<LancamentoCartao> lancamentos = lancamentoCartaoService.listarLancamentos();
        log.info("[HTTP] Lançamentos obtidos para cálculo de fatura: quantidade={}", lancamentos != null ? lancamentos.size() : 0);
        Map<Long, Double> faturasPorCartao = lancamentos.stream()
                .filter(l -> l.getMesAnoFatura().equals(mesAno))
                .collect(Collectors.groupingBy(
                        l -> l.getCartaoCredito().getId(),
                        Collectors.summingDouble(l -> l.getValorTotal().doubleValue())
                ));
        log.info("[HTTP] Sucesso GET /api/v1/lancamentos-cartao/fatura-por-cartao: cartoesEncontrados={}", faturasPorCartao != null ? faturasPorCartao.size() : 0);
        return ResponseEntity.ok(faturasPorCartao);
    }

    @GetMapping("/terceiros")
    public ResponseEntity<List<LancamentoCartao>> listarLancamentosTerceiros(
            @RequestParam(required = false) String mesAnoFatura) {
        log.info("[HTTP] Início GET /api/v1/lancamentos-cartao/terceiros (listarLancamentosTerceiros): mesAnoFatura={}", mesAnoFatura);
        log.info("[HTTP] Chamando service: lancamentoCartaoService.listarLancamentosTerceiros(mesAnoFatura)");
        List<LancamentoCartao> lancamentos = lancamentoCartaoService.listarLancamentosTerceiros(mesAnoFatura);
        log.info("[HTTP] Sucesso GET /api/v1/lancamentos-cartao/terceiros: quantidadeRetornada={}", lancamentos != null ? lancamentos.size() : 0);
        return ResponseEntity.ok(lancamentos);
    }
}