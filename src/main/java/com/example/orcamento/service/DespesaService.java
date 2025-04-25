// src/main/java/com/example/orcamento/service/DespesaService.java
package com.example.orcamento.service;

import com.example.orcamento.dto.dashboard.DespesasMensaisDTO;
import com.example.orcamento.model.*;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.MovimentacaoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DespesaService {

    private final DespesaRepository despesaRepository;
    private final ContaCorrenteService contaCorrenteService;
    private final MovimentacaoService movimentacaoService;
    private final MovimentacaoRepository movimentacaoRepository;
    private final MetaEconomiaService metaEconomiaService;
    @Autowired
    private LancamentoCartaoRepository lancamentoCartaoRepository;

    public List<Despesa> listarDespesas() {
        return despesaRepository.findAll();
    }

    @Transactional
    public Despesa salvarDespesa(Despesa despesa) {
        if (despesa.getContaCorrente() != null && despesa.getContaCorrente().getId() == null) {
            throw new IllegalArgumentException("Conta corrente inválida para salvar a despesa");
        }
        if (despesa.getValorPrevisto() == null || despesa.getValorPrevisto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor previsto da despesa deve ser maior que zero");
        }

        log.info("Salvando uma despesa: {}", despesa);
        Despesa despesaSalva = despesaRepository.save(despesa);

        // Integração com MetaEconomia
        if ("Investimento".equalsIgnoreCase(despesaSalva.getTipo().getNome()) &&
                despesaSalva.getMetaEconomia() != null &&
                despesaSalva.getValorPago() != null &&
                despesaSalva.getDataPagamento() != null) {
            MetaEconomia meta = metaEconomiaService.buscarPorId(despesaSalva.getMetaEconomia().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Meta não encontrada"));
            meta.setValorEconomizado(meta.getValorEconomizado() + despesaSalva.getValorPago().doubleValue());
            metaEconomiaService.salvarMeta(meta);
        }

        // Se já tiver pagamento, registra a movimentação
        if (despesaSalva.getValorPago() != null && despesaSalva.getDataPagamento() != null && despesaSalva.getContaCorrente() != null) {
            Movimentacao movimentacao = Movimentacao.builder()
                    .tipo(TipoMovimentacao.SAIDA)
                    .valor(despesaSalva.getValorPago())
                    .contaCorrente(despesaSalva.getContaCorrente())
                    .despesa(despesaSalva)
                    .descricao("Pagamento de " + despesaSalva.getNome())
                    .dataRecebimento(despesaSalva.getDataPagamento())
                    .dataCadastro(LocalDateTime.now())
                    .build();
            movimentacaoService.registrarMovimentacao(movimentacao);
        }

        return despesaSalva;
    }

    @Transactional
    public void excluirDespesa(Long id) {
        Despesa despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Despesa não encontrada com ID: " + id));

        log.info("Excluindo a despesa: {}", despesa);

        // Remove as movimentações associadas à despesa
        List<Movimentacao> movimentacoes = movimentacaoRepository.findByDespesa(despesa);
        if (!movimentacoes.isEmpty()) {
            movimentacaoRepository.deleteAll(movimentacoes);
        }

        // Se já foi paga, registra uma movimentação de entrada para "estornar" o saldo
        if (despesa.getValorPago() != null && despesa.getDataPagamento() != null && despesa.getContaCorrente() != null) {
            Movimentacao movimentacao = Movimentacao.builder()
                    .tipo(TipoMovimentacao.ENTRADA)
                    .valor(despesa.getValorPago())
                    .contaCorrente(despesa.getContaCorrente())
                    .descricao("Estorno de despesa: " + despesa.getNome())
                    .dataRecebimento(despesa.getDataPagamento())
                    .dataCadastro(LocalDateTime.now())
                    .build();
            movimentacaoService.registrarMovimentacao(movimentacao);
        }

        despesaRepository.deleteById(id);
    }

    public List<Despesa> listarPorTipo(Long tipoId) {
        return despesaRepository.findByTipoId(tipoId);
    }

    @Transactional
    public Despesa atualizarPagamento(Long id, BigDecimal valorPago, LocalDate dataPagamento, Long contaCorrenteId, Long metaEconomiaId) {
        Despesa despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Despesa não encontrada"));

        if (despesa.getDataPagamento() != null) {
            throw new IllegalStateException("Despesa já foi paga");
        }

        despesa.setValorPago(valorPago);
        despesa.setDataPagamento(dataPagamento);

        if (contaCorrenteId != null) {
            ContaCorrente conta = contaCorrenteService.buscarPorId(contaCorrenteId)
                    .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + contaCorrenteId));
            despesa.setContaCorrente(conta);

            Movimentacao movimentacao = Movimentacao.builder()
                    .tipo(TipoMovimentacao.SAIDA)
                    .valor(valorPago)
                    .contaCorrente(conta)
                    .despesa(despesa)
                    .descricao("Pagamento de " + despesa.getNome())
                    .dataRecebimento(dataPagamento)
                    .dataCadastro(LocalDateTime.now())
                    .build();
            movimentacaoService.registrarMovimentacao(movimentacao);
        }

        // Integração com MetaEconomia
        if ("Investimento".equalsIgnoreCase(despesa.getTipo().getNome()) && metaEconomiaId != null) {
            MetaEconomia meta = metaEconomiaService.buscarPorId(metaEconomiaId)
                    .orElseThrow(() -> new EntityNotFoundException("Meta não encontrada: " + metaEconomiaId));
            despesa.setMetaEconomia(meta);
            meta.setValorEconomizado(meta.getValorEconomizado() + valorPago.doubleValue());
            metaEconomiaService.salvarMeta(meta);
        }

        return despesaRepository.save(despesa);
    }

    @Transactional
    public Despesa atualizarDespesa(Long id, Despesa despesaAtualizada) {
        Despesa despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Despesa não encontrada"));

        log.info("Atualizando despesa: {} para {}", despesa, despesaAtualizada);

        // Verifica se houve mudança no pagamento (valor, data ou conta)
        boolean tinhaPagamento = despesa.getValorPago() != null && despesa.getDataPagamento() != null && despesa.getContaCorrente() != null;
        boolean temPagamento = despesaAtualizada.getValorPago() != null && despesaAtualizada.getDataPagamento() != null && despesaAtualizada.getContaCorrente() != null;

        boolean mudouValor = tinhaPagamento && temPagamento && !despesa.getValorPago().equals(despesaAtualizada.getValorPago());
        boolean mudouConta = tinhaPagamento && temPagamento && !despesa.getContaCorrente().getId().equals(despesaAtualizada.getContaCorrente().getId());

        // Caso 1: Pagamento removido (estorno)
        if (tinhaPagamento && !temPagamento) {
            Movimentacao movimentacaoEntrada = Movimentacao.builder()
                    .tipo(TipoMovimentacao.ENTRADA)
                    .valor(despesa.getValorPago())
                    .contaCorrente(despesa.getContaCorrente())
                    .despesa(despesa)
                    .descricao("Remoção de pagamento (estorno): " + despesa.getNome())
                    .dataRecebimento(despesa.getDataPagamento())
                    .dataCadastro(LocalDateTime.now())
                    .build();
            movimentacaoService.registrarMovimentacao(movimentacaoEntrada);
        }
        // Caso 2: Pagamento adicionado ou alterado
        else if (temPagamento) {
            // Se já tinha pagamento e mudou valor ou conta, faz estorno do antigo
            if (tinhaPagamento && (mudouValor || mudouConta)) {
                Movimentacao movimentacaoEntrada = Movimentacao.builder()
                        .tipo(TipoMovimentacao.ENTRADA)
                        .valor(despesa.getValorPago())
                        .contaCorrente(despesa.getContaCorrente())
                        .despesa(despesa)
                        .descricao("Correção de despesa (estorno): " + despesa.getNome())
                        .dataRecebimento(despesa.getDataPagamento())
                        .dataCadastro(LocalDateTime.now())
                        .build();
                movimentacaoService.registrarMovimentacao(movimentacaoEntrada);
            }
            // Registra o novo pagamento (se novo ou alterado)
            if (!tinhaPagamento || mudouValor || mudouConta) {
                Movimentacao movimentacaoSaida = Movimentacao.builder()
                        .tipo(TipoMovimentacao.SAIDA)
                        .valor(despesaAtualizada.getValorPago())
                        .contaCorrente(despesaAtualizada.getContaCorrente())
                        .despesa(despesa)
                        .descricao("Correção de despesa (pagamento): " + despesaAtualizada.getNome())
                        .dataRecebimento(despesaAtualizada.getDataPagamento())
                        .dataCadastro(LocalDateTime.now())
                        .build();
                movimentacaoService.registrarMovimentacao(movimentacaoSaida);
            }
        }

        // Atualiza os campos da despesa
        despesa.setNome(despesaAtualizada.getNome());
        despesa.setValorPrevisto(despesaAtualizada.getValorPrevisto());
        despesa.setValorPago(despesaAtualizada.getValorPago());
        despesa.setDataVencimento(despesaAtualizada.getDataVencimento());
        despesa.setDataPagamento(despesaAtualizada.getDataPagamento());
        despesa.setParcela(despesaAtualizada.getParcela());
        despesa.setDetalhes(despesaAtualizada.getDetalhes());
        despesa.setTipo(despesaAtualizada.getTipo());
        despesa.setContaCorrente(despesaAtualizada.getContaCorrente());

        return despesaRepository.save(despesa);
    }

    public Map<String, Map<String, BigDecimal>> listarPorMes(int ano, Long tipoId) {
        List<Despesa> despesas = tipoId != null
                ? despesaRepository.findByAnoAndTipoId(ano, tipoId)
                : despesaRepository.findAll().stream()
                .filter(d -> d.getDataVencimento().getYear() == ano)
                .collect(Collectors.toList());

        return despesas.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getDataVencimento().getMonth().toString(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                lista -> {
                                    Map<String, BigDecimal> valores = new HashMap<>();
                                    BigDecimal previsto = lista.stream()
                                            .map(Despesa::getValorPrevisto)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    BigDecimal pago = lista.stream()
                                            .filter(d -> d.getValorPago() != null && d.getDataPagamento() != null)
                                            .map(Despesa::getValorPago)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    valores.put("valorPrevisto", previsto);
                                    valores.put("valorPago", pago.compareTo(BigDecimal.ZERO) > 0 ? pago : null);
                                    return valores;
                                }
                        )
                ));
    }


    public Map<String, Double> calcularGastosPorCategoria(Integer ano, Integer mes) {
        // 1. Gastos das despesas comuns
        List<Despesa> despesas = despesaRepository.findByAnoAndMes(ano, mes);
        Map<String, Double> gastosPorCategoria = new HashMap<>();

        for (Despesa despesa : despesas) {
            String categoria = despesa.getTipo().getNome();
            BigDecimal valor = despesa.getValorPago() != null ? despesa.getValorPago() : despesa.getValorPrevisto();
            double valorDouble = valor.doubleValue();
            gastosPorCategoria.put(categoria, gastosPorCategoria.getOrDefault(categoria, 0.0) + valorDouble);
        }

        // 2. Gastos das faturas de cartão (não lançadas como despesa)
        List<LancamentoCartao> lancamentos = lancamentoCartaoRepository.findByAnoAndMes(ano, mes);
        for (LancamentoCartao lancamento : lancamentos) {
            String categoria = lancamento.getTipoDespesa().getNome(); // Assumindo que LancamentoCartao tem tipoDespesa
            double valor = lancamento.getValorTotal().doubleValue(); // Assumindo que LancamentoCartao tem valor como BigDecimal
            gastosPorCategoria.put(categoria, gastosPorCategoria.getOrDefault(categoria, 0.0) + valor);
        }

        return gastosPorCategoria;
    }

    public List<Despesa> listarProximasEVencidas(LocalDate dataReferencia, LocalDate dataFim) {
        return despesaRepository.findVencidasEProximas(dataReferencia, dataFim);
    }

    public List<Despesa> listarDespesasPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        return despesaRepository.findByDataVencimentoBetween(dataInicio, dataFim);
    }


    public List<DespesasMensaisDTO> buscarDespesasPorAno(int ano) {
        // Busca todas as despesas do ano informado
        List<Despesa> despesas = despesaRepository.findDespesasByAno(ano);

        // Agrupa as despesas por mês (usando Stream API)
        Map<Integer, List<Despesa>> despesasPorMes = despesas.stream()
                .collect(Collectors.groupingBy(d -> d.getDataVencimento().getMonthValue()));

        // Transforma o mapa em uma lista de DTOs
        return despesasPorMes.entrySet().stream()
                .map(entry -> new DespesasMensaisDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DespesasMensaisDTO::getMes)) // Ordena pelo número do mês
                .collect(Collectors.toList());
    }


    @Transactional
    public List<Despesa> salvarMultiplasDespesas(List<Despesa> despesas) {
        List<Despesa> despesasSalvas = new ArrayList<>();
        for (Despesa despesa : despesas) {
            despesasSalvas.add(salvarDespesa(despesa));
        }
        return despesasSalvas;
    }

    @Transactional
    public void excluirDespesasPorDespesaParceladaId(Long despesaParceladaId) {
        // Buscar todas as despesas que são parcelas desta despesa parcelada
        List<Despesa> parcelas = despesaRepository.findByDespesaParceladaId(despesaParceladaId);

        // Excluir cada parcela
        for (Despesa parcela : parcelas) {
            excluirDespesa(parcela.getId());
        }

        log.info("Excluídas {} parcelas associadas à despesa parcelada ID {}", parcelas.size(), despesaParceladaId);
    }
}