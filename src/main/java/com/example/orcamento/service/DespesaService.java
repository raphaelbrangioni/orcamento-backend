// src/main/java/com/example/orcamento/service/DespesaService.java
package com.example.orcamento.service;

import com.example.orcamento.dto.ConfiguracaoDTO;
import com.example.orcamento.dto.dashboard.DespesasMensaisDTO;
import com.example.orcamento.model.*;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.MovimentacaoRepository;
import com.example.orcamento.specification.DespesaSpecification;
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
    @Autowired
    private ConfiguracaoService configuracaoService;

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
        ConfiguracaoDTO configuracao = configuracaoService.getConfiguracoes();
        if (configuracao != null &&
                configuracao.getTipoDespesaInvestimentoId() != null &&
                despesaSalva.getTipo() != null &&
                despesaSalva.getTipo().getId().equals(configuracao.getTipoDespesaInvestimentoId()) &&
                despesaSalva.getMetaEconomia() != null &&
                despesaSalva.getValorPago() != null &&
                despesaSalva.getDataPagamento() != null) {

            log.info("Integração com MetaEconomia............ Atualizando: {}", despesa.getMetaEconomia());

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
        ConfiguracaoDTO configuracao = configuracaoService.getConfiguracoes();

        log.info("Configuracoes: {}", configuracao);
        log.info("TipoDespesaInvestimentoId: {}", configuracao.getTipoDespesaInvestimentoId());
        log.info("Despesa tipo: {}", despesa.getTipo());
        log.info("Despesa tipo id: {}", despesa.getTipo().getId());
        log.info("MetaEconomiaId: {}", metaEconomiaId);

        // Verificação detalhada com logs
        boolean condicao1 = configuracao != null;
        boolean condicao2 = configuracao.getTipoDespesaInvestimentoId() != null;
        boolean condicao3 = despesa.getTipo() != null;
        boolean condicao4 = false;
        if (condicao1 && condicao2 && condicao3) {
            condicao4 = despesa.getTipo().getId().equals(configuracao.getTipoDespesaInvestimentoId());
        }
        boolean condicao5 = metaEconomiaId != null;

        log.info("Condição 1 (configuracao != null): {}", condicao1);
        log.info("Condição 2 (tipoDespesaInvestimentoId != null): {}", condicao2);
        log.info("Condição 3 (despesa.getTipo() != null): {}", condicao3);
        log.info("Condição 4 (ids iguais): {}", condicao4);
        log.info("Condição 5 (metaEconomiaId != null): {}", condicao5);

        // Comparação direta dos IDs como Long
        if (condicao3 && condicao2) {
            log.info("Comparação direta: {} == {}: {}",
                    despesa.getTipo().getId(),
                    configuracao.getTipoDespesaInvestimentoId(),
                    despesa.getTipo().getId() == configuracao.getTipoDespesaInvestimentoId());
        }

        if (condicao1 && condicao2 && condicao3 && condicao4 && condicao5) {
            MetaEconomia meta = metaEconomiaService.buscarPorId(metaEconomiaId)
                    .orElseThrow(() -> new EntityNotFoundException("Meta não encontrada: " + metaEconomiaId));
            despesa.setMetaEconomia(meta);
            meta.setValorEconomizado(meta.getValorEconomizado() + valorPago.doubleValue());
            metaEconomiaService.salvarMeta(meta);

            log.info("Meta de economia atualizada: {}, novo valor economizado: {}",
                    meta.getNome(), meta.getValorEconomizado());
        } else {
            log.info("Não foi possível integrar com MetaEconomia - Condições não atendidas");
        }

        return despesaRepository.save(despesa);
    }

    @Transactional
    public Despesa atualizarDespesa(Long id, Despesa despesaAtualizada) {
        Despesa despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Despesa não encontrada"));

        log.info("Atualizando despesa: {} para {}", despesa, despesaAtualizada);

        // Preserva os dados de pagamento originais
        BigDecimal valorPagoOriginal = despesa.getValorPago();
        LocalDate dataPagamentoOriginal = despesa.getDataPagamento();
        ContaCorrente contaCorrenteOriginal = despesa.getContaCorrente();

        // Atualiza os campos da despesa (exceto pagamento)
        despesa.setNome(despesaAtualizada.getNome());
        despesa.setValorPrevisto(despesaAtualizada.getValorPrevisto());
        despesa.setDataVencimento(despesaAtualizada.getDataVencimento());
        despesa.setParcela(despesaAtualizada.getParcela());
        despesa.setDetalhes(despesaAtualizada.getDetalhes());
        despesa.setTipo(despesaAtualizada.getTipo());
        despesa.setClassificacao(despesaAtualizada.getClassificacao());
        despesa.setVariabilidade(despesaAtualizada.getVariabilidade());

        // Restaura os dados de pagamento originais
        despesa.setValorPago(valorPagoOriginal);
        despesa.setDataPagamento(dataPagamentoOriginal);
        despesa.setContaCorrente(contaCorrenteOriginal);

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


    public Map<Long, Double> calcularGastosPorCategoria(Integer ano, Integer mes) {
        log.info("Calculando gastos por categoria para ano={}, mes={}", ano, mes);
        // 1. Gastos das despesas comuns
        List<Despesa> despesas = despesaRepository.findByAnoAndMes(ano, mes);
        log.info("Despesas encontradas: {}", despesas.size());
        Map<Long, Double> gastosPorCategoria = new HashMap<>();

        for (Despesa despesa : despesas) {
            Long categoriaId = despesa.getTipo().getId();
            BigDecimal valor = despesa.getValorPago() != null ? despesa.getValorPago() : despesa.getValorPrevisto();
            double valorDouble = valor.doubleValue();
            gastosPorCategoria.put(categoriaId, gastosPorCategoria.getOrDefault(categoriaId, 0.0) + valorDouble);
        }

        // 2. Gastos das faturas de cartão (não lançadas como despesa)
        List<LancamentoCartao> lancamentos = lancamentoCartaoRepository.findByAnoAndMes(ano, mes);
        log.info("Lançamentos de cartão encontrados: {}", lancamentos.size());
        for (LancamentoCartao lancamento : lancamentos) {
            Long categoriaId = lancamento.getTipoDespesa().getId();
            double valor = lancamento.getValorTotal().doubleValue();
            gastosPorCategoria.put(categoriaId, gastosPorCategoria.getOrDefault(categoriaId, 0.0) + valor);
        }

        log.info("Mapa final de gastos por categoria (por id): {}", gastosPorCategoria);
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

    @Transactional
    public Despesa estornarPagamento(Long id) {
        Despesa despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Despesa não encontrada"));

        // Verifica se existe pagamento
        if (despesa.getValorPago() == null || despesa.getDataPagamento() == null || despesa.getContaCorrente() == null) {
            throw new IllegalStateException("Despesa não possui pagamento para estornar");
        }

        // Cria movimentação de estorno
        Movimentacao movimentacaoEntrada = Movimentacao.builder()
                .tipo(TipoMovimentacao.ENTRADA)
                .valor(despesa.getValorPago())
                .contaCorrente(despesa.getContaCorrente())
                .despesa(despesa)
                .descricao("Estorno de pagamento: " + despesa.getNome())
                .dataRecebimento(LocalDate.now())
                .dataCadastro(LocalDateTime.now())
                .build();

        movimentacaoService.registrarMovimentacao(movimentacaoEntrada);

        // Se for despesa de investimento, estorna também na meta de economia
        ConfiguracaoDTO configuracao = configuracaoService.getConfiguracoes();
        if (configuracao != null &&
                configuracao.getTipoDespesaInvestimentoId() != null &&
                despesa.getTipo() != null &&
                despesa.getTipo().getId().equals(configuracao.getTipoDespesaInvestimentoId()) &&
                despesa.getMetaEconomia() != null) {

            MetaEconomia meta = metaEconomiaService.buscarPorId(despesa.getMetaEconomia().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Meta não encontrada"));
            meta.setValorEconomizado(meta.getValorEconomizado() - despesa.getValorPago().doubleValue());
            metaEconomiaService.salvarMeta(meta);

            log.info("Estorno na meta de economia: {}, novo valor economizado: {}",
                    meta.getNome(), meta.getValorEconomizado());
        }

        // Limpa os dados de pagamento
        despesa.setValorPago(null);
        despesa.setDataPagamento(null);
        despesa.setContaCorrente(null);

        return despesaRepository.save(despesa);
    }


    // No MetaEconomiaService.java
    public List<Despesa> buscarDespesasRelacionadas(Long metaId) {
        // Verificar se a meta existe
        MetaEconomia meta = metaEconomiaService.buscarPorId(metaId)
                .orElseThrow(() -> new EntityNotFoundException("Meta não encontrada com ID: " + metaId));

        // Buscar despesas relacionadas
        return despesaRepository.findByMetaEconomiaId(metaId);
    }


//    // Novo método para filtro dinâmico
//    public List<Despesa> listarDespesasPorFiltrosDinamicos(Map<String, Object> filtros) {
//        log.info("Buscando despesas com filtros dinâmicos: {}", filtros);
//        return despesaRepository.findAll(DespesaSpecification.comFiltros(filtros));
//    }

    public List<Despesa> listarDespesasPorFiltrosDinamicos(Map<String, Object> filtros) {
        log.info("Buscando despesas com filtros dinâmicos: {}", filtros);
        Map<String, Object> filtrosMapeados = new HashMap<>();
        filtros.forEach((key, value) -> {
            switch (key) {
                case "descricao" -> filtrosMapeados.put("nome", value);
                case "valor" -> filtrosMapeados.put("valorPrevisto", value);
                case "tipoDespesaId" -> filtrosMapeados.put("tipoDespesaId", value);
                case "dataInicio" -> filtrosMapeados.put("dataVencimentoInicio", value);
                case "dataFim" -> filtrosMapeados.put("dataVencimentoFim", value);
                default -> filtrosMapeados.put(key, value); // id, detalhes, classificacao, variabilidade, parcela
            }
        });
        return despesaRepository.findAll(DespesaSpecification.comFiltros(filtrosMapeados));
    }


}