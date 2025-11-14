package com.example.orcamento.controller;

import com.example.orcamento.dto.*;
import com.example.orcamento.dto.dashboard.DespesasMensaisDTO;
import com.example.orcamento.model.*;
import com.example.orcamento.model.enums.FormaDePagamento;
import com.example.orcamento.service.DespesaService;
import com.example.orcamento.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/despesas")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost")
@Slf4j
public class DespesaController {

    private final DespesaService despesaService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<List<DespesaResponseDTO>> listarDespesas(
            @RequestParam(required = false) String dataMin,
            @RequestParam(required = false) String dataMax) {
        log.info("Requisição GET em /api/v1/despesas, dataMin: {}, dataMax: {}", dataMin, dataMax);
        List<Despesa> despesas;
        if (dataMin != null && dataMax != null) {
            LocalDate inicio = LocalDate.parse(dataMin);
            LocalDate fim = LocalDate.parse(dataMax);
            despesas = despesaService.listarDespesasPorPeriodo(inicio, fim);
        } else {
            despesas = despesaService.listarDespesas();
        }
        List<DespesaResponseDTO> resposta = despesas.stream()
                .map(this::toDespesaResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resposta);
    }

    @PostMapping
    public ResponseEntity<List<DespesaResponseDTO>> cadastrarDespesa(@RequestBody Despesa despesa) {
        log.info("Requisição POST em /api/v1/despesas, cadastrarDespesa");
        Despesa salva = despesaService.salvarDespesa(despesa);
        return ResponseEntity.ok(List.of(toDespesaResponseDTO(salva)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirDespesa(@PathVariable Long id) {
        log.info("Requisição DELETE em /api/v1/despesas/{id}, excluirDespesa");
        log.info("ID informado para exclusão: {}", id);
        despesaService.excluirDespesa(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/pagar")
    public ResponseEntity<DespesaResponseDTO> pagarDespesa(@PathVariable Long id, @RequestBody PagamentoRequest pagamentoRequest) {
        log.info("Requisição PUT em /api/v1/despesas/{id}/pagar, pagarDespesa");
        log.info("ID informado para pagamento: {}", id);
        log.info("Pagamento Request: {}",pagamentoRequest.toString());
        Despesa despesa = despesaService.atualizarPagamento(
                id,
                pagamentoRequest.getValorPago(),
                pagamentoRequest.getDataPagamento(),
                pagamentoRequest.getContaCorrenteId(),
                pagamentoRequest.getMetaEconomiaId(),
                pagamentoRequest.getFormaDePagamento()
        );
        return ResponseEntity.ok(toDespesaResponseDTO(despesa));
    }

    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
    public ResponseEntity<DespesaResponseDTO> atualizarDespesa(@PathVariable Long id,
                                                               @RequestPart("despesa") Despesa despesaAtualizada,
                                                               @RequestPart(value = "anexo", required = false) MultipartFile anexo) {
        log.info("Requisição PUT em /api/v1/despesas/{id}, atualizarDespesa com anexo");
        log.info("ID informado para atualização: {}", id);
        Despesa despesa = despesaService.atualizarDespesa(id, despesaAtualizada, anexo);
        return ResponseEntity.ok(toDespesaResponseDTO(despesa));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DespesaResponseDTO> atualizarDespesaSemAnexo(@PathVariable Long id,
                                                                       @RequestBody Despesa despesaAtualizada) {
        log.info("Requisição PUT em /api/v1/despesas/{id}, atualizarDespesa sem anexo (JSON)");
        log.info("ID informado para atualização: {}", id);
        Despesa despesa = despesaService.atualizarDespesa(id, despesaAtualizada);
        return ResponseEntity.ok(toDespesaResponseDTO(despesa));
    }

    @GetMapping("/anexos/{fileName:.+}")
    public ResponseEntity<Resource> baixarAnexo(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Não foi possível determinar o tipo do arquivo.");
        }
        if(contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/por-mes/{ano}")
    public ResponseEntity<Map<String, Map<String, BigDecimal>>> listarPorMes(@PathVariable int ano) {
        log.info("Requisição GET em /api/v1/despesas/por-mes/{ano}, listarPorMes");
        return ResponseEntity.ok(despesaService.listarPorMes(ano));
    }

    @GetMapping("/proximas")
    public ResponseEntity<List<DespesaResponseDTO>> listarProximasDespesas(@RequestParam(defaultValue = "7") int dias) {
        log.info("Requisição GET em /api/v1/despesas/proximas, dias: {}", dias);
        LocalDate hoje = LocalDate.now();
        LocalDate dataMaxima = hoje.plusDays(dias);
        List<Despesa> proximas = despesaService.listarProximasEVencidas(hoje, dataMaxima);
        List<DespesaResponseDTO> resposta = proximas.stream()
                .map(this::toDespesaResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resposta);
    }

    static class PagamentoRequest {
        private BigDecimal valorPago;
        private LocalDate dataPagamento;
        private Long contaCorrenteId;
        private Long metaEconomiaId;
        private Long metaId;
        private FormaDePagamento formaDePagamento;

        public BigDecimal getValorPago() { return valorPago; }
        public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }
        public LocalDate getDataPagamento() { return dataPagamento; }
        public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }
        public Long getContaCorrenteId() { return contaCorrenteId; }
        public void setContaCorrenteId(Long contaCorrenteId) { this.contaCorrenteId = contaCorrenteId; }
        public Long getMetaEconomiaId() { return metaEconomiaId != null ? metaEconomiaId : metaId; }
        public void setMetaEconomiaId(Long metaEconomiaId) { this.metaEconomiaId = metaEconomiaId; }
        public Long getMetaId() { return metaId; }
        public void setMetaId(Long metaId) { this.metaId = metaId; }
        public FormaDePagamento getFormaDePagamento() { return formaDePagamento; }
    }

    @GetMapping("/ano")
    public ResponseEntity<List<DespesasMensaisDTO>> buscarDespesasPorAno(@RequestParam int ano) {
        log.info("Requisição GET em /api/v1/despesas/ano, ano: {}", ano);
        List<DespesasMensaisDTO> despesasAgrupadas = despesaService.buscarDespesasPorAno(ano);
        return ResponseEntity.ok(despesasAgrupadas);
    }

    @PutMapping("/{id}/estornar-pagamento")
    public ResponseEntity<DespesaDTO> estornarPagamento(@PathVariable Long id) {
        log.info("Requisição PUT em /api/v1/despesas/{id}/estornar-pagamento");
        log.info("ID informado para estorno: {}", id);
        Despesa despesa = despesaService.estornarPagamento(id);
        return ResponseEntity.ok(new DespesaDTO(despesa));
    }

    @GetMapping("/filtrar-dinamico")
    public ResponseEntity<List<DespesaResponseDTO>> filtrarDinamico(@RequestParam Map<String, String> params) {
        List<Despesa> despesas = despesaService.filtrarDinamico(params);
        List<DespesaResponseDTO> resposta = despesas.stream()
                .map(this::toDespesaResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resposta);
    }

    private DespesaResponseDTO toDespesaResponseDTO(Despesa despesa) {
        SubcategoriaDespesa subcat = despesa.getSubcategoria();
        CategoriaDespesa cat = subcat != null ? subcat.getCategoria() : null;
        ContaCorrente conta = despesa.getContaCorrente();
        MetaEconomia meta = despesa.getMetaEconomia();

        String anexoUrl = null;
        if (despesa.getAnexo() != null && !despesa.getAnexo().isEmpty()) {
            anexoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/despesas/anexos/")
                    .path(despesa.getAnexo())
                    .toUriString();
        }

        DespesaResponseDTO dto = new DespesaResponseDTO();
        dto.setId(despesa.getId());
        dto.setNome(despesa.getNome());
        dto.setTenantId(despesa.getTenantId());
        dto.setValorPrevisto(despesa.getValorPrevisto());
        dto.setValorPago(despesa.getValorPago());
        dto.setDataVencimento(despesa.getDataVencimento());
        dto.setDataPagamento(despesa.getDataPagamento());
        dto.setParcela(despesa.getParcela());
        dto.setDetalhes(despesa.getDetalhes());
        dto.setClassificacao(despesa.getClassificacao() != null ? despesa.getClassificacao().name() : null);
        dto.setVariabilidade(despesa.getVariabilidade() != null ? despesa.getVariabilidade().name() : null);
        dto.setFormaDePagamento(despesa.getFormaDePagamento());
        dto.setCategoria(cat != null ? new CategoriaDespesaDTO(cat.getId(), cat.getNome(), subcat != null ? new SubcategoriaDespesaDTO(subcat.getId(), subcat.getNome()) : null) : null);
        dto.setConta(conta != null && despesa.getValorPago() != null ? new ContaCorrenteDTO(
                conta.getId(),
                conta.getAgencia(),
                conta.getNumeroConta(),
                conta.getBanco(),
                conta.getNomeBanco(),
                conta.getSaldo()
        ) : null);
        dto.setAnexo(anexoUrl);

        if (meta != null) {
            dto.setMetaEconomia(new MetaEconomiaDTO(meta.getId(), meta.getNome(), BigDecimal.valueOf(meta.getValor())));
        }

        return dto;
    }
}