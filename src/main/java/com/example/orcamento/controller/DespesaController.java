package com.example.orcamento.controller;

import com.example.orcamento.dto.DespesaDTO;
import com.example.orcamento.dto.dashboard.DespesasMensaisDTO;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.MetaEconomia;
import com.example.orcamento.service.DespesaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public ResponseEntity<List<Despesa>> listarDespesas(
            @RequestParam(required = false) String dataMin,
            @RequestParam(required = false) String dataMax) {
        log.info("Requisição GET em /api/v1/despesas, dataMin: {}, dataMax: {}", dataMin, dataMax);
        if (dataMin != null && dataMax != null) {
            LocalDate inicio = LocalDate.parse(dataMin);
            LocalDate fim = LocalDate.parse(dataMax);
            return ResponseEntity.ok(despesaService.listarDespesasPorPeriodo(inicio, fim));
        }
        return ResponseEntity.ok(despesaService.listarDespesas());
    }

//    @GetMapping
//    public ResponseEntity<List<Despesa>> listarDespesas() {
//        log.info("Requisição GET em /api/v1/despesas, listarDespesas");
//        return ResponseEntity.ok(despesaService.listarDespesas());
//    }

    @PostMapping
    public ResponseEntity<Despesa> cadastrarDespesa(@RequestBody Despesa despesa) {
        log.info("Requisição POST em /api/v1/despesas, cadastrarDespesa");
        return ResponseEntity.ok(despesaService.salvarDespesa(despesa));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirDespesa(@PathVariable Long id) {
        log.info("Requisição DELETE em /api/v1/despesas/{id}, excluirDespesa");
        log.info("ID informado para exclusão: {}", id);
        despesaService.excluirDespesa(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/pagar")
    public ResponseEntity<DespesaDTO> pagarDespesa(@PathVariable Long id, @RequestBody PagamentoRequest pagamentoRequest) {
        log.info("Requisição PUT em /api/v1/despesas/{id}/pagar, pagarDespesa");
        log.info("ID informado para pagamento: {}", id);
        Despesa despesa = despesaService.atualizarPagamento(
                id,
                pagamentoRequest.getValorPago(),
                pagamentoRequest.getDataPagamento(),
                pagamentoRequest.getContaCorrenteId(),
                pagamentoRequest.getMetaEconomiaId()
        );
        return ResponseEntity.ok(new DespesaDTO(despesa));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DespesaDTO> atualizarDespesa(@PathVariable Long id, @RequestBody Despesa despesaAtualizada) {
        log.info("Requisição PUT em /api/v1/despesas/{id}, atualizarDespesa");
        log.info("ID informado para atualização: {}", id);
        Despesa despesa = despesaService.atualizarDespesa(id, despesaAtualizada);
        return ResponseEntity.ok(new DespesaDTO(despesa));
    }

    @GetMapping("/por-mes/{ano}")
    public ResponseEntity<Map<String, Map<String, BigDecimal>>> listarPorMes(
            @PathVariable int ano,
            @RequestParam(required = false) Long tipoId) {
        log.info("Requisição GET em /api/v1/despesas/por-mes/{ano}, listarPorMes");
        return ResponseEntity.ok(despesaService.listarPorMes(ano, tipoId));
    }

    @GetMapping("/proximas")
    public ResponseEntity<List<Despesa>> listarProximasDespesas(
            @RequestParam(defaultValue = "7") int dias) {
        log.info("Requisição GET em /api/v1/despesas/proximas, dias: {}", dias);
        LocalDate hoje = LocalDate.now();
        LocalDate dataMaxima = hoje.plusDays(dias);
        List<Despesa> proximas = despesaService.listarProximasEVencidas(hoje, dataMaxima);
        return ResponseEntity.ok(proximas);
    }

//    static class PagamentoRequest {
//        private BigDecimal valorPago;
//        private LocalDate dataPagamento;
//        private Long contaCorrenteId;
//        private Long metaEconomiaId;
//
//        public BigDecimal getValorPago() { return valorPago; }
//        public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }
//        public LocalDate getDataPagamento() { return dataPagamento; }
//        public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }
//        public Long getContaCorrenteId() { return contaCorrenteId; }
//        public void setContaCorrenteId(Long contaCorrenteId) { this.contaCorrenteId = contaCorrenteId; }
//        public Long getMetaEconomiaId() { return metaEconomiaId; }
//        public void setMetaEconomiaId(Long metaEconomiaId) { this.metaEconomiaId = metaEconomiaId; }
//    }

    static class PagamentoRequest {
        private BigDecimal valorPago;
        private LocalDate dataPagamento;
        private Long contaCorrenteId;
        private Long metaEconomiaId;
        private Long metaId; // Novo campo para compatibilidade com o frontend

        public BigDecimal getValorPago() { return valorPago; }
        public void setValorPago(BigDecimal valorPago) { this.valorPago = valorPago; }
        public LocalDate getDataPagamento() { return dataPagamento; }
        public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }
        public Long getContaCorrenteId() { return contaCorrenteId; }
        public void setContaCorrenteId(Long contaCorrenteId) { this.contaCorrenteId = contaCorrenteId; }

        public Long getMetaEconomiaId() {
            // Se metaEconomiaId for nulo, tenta usar metaId
            return metaEconomiaId != null ? metaEconomiaId : metaId;
        }

        public void setMetaEconomiaId(Long metaEconomiaId) { this.metaEconomiaId = metaEconomiaId; }
        public Long getMetaId() { return metaId; }
        public void setMetaId(Long metaId) { this.metaId = metaId; }
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



    // No MetaEconomiaController.java
    @GetMapping("/{id}/despesas-relacionadas")
    public ResponseEntity<List<DespesaDTO>> listarDespesasRelacionadas(@PathVariable Long id) {
        List<Despesa> despesas = despesaService.buscarDespesasRelacionadas(id);
        List<DespesaDTO> despesasDTO = despesas.stream()
                .map(DespesaDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(despesasDTO);
    }


}