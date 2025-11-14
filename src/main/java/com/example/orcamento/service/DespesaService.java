// src/main/java/com/example/orcamento/service/DespesaService.java
package com.example.orcamento.service;

import com.example.orcamento.dto.ConfiguracaoDTO;
import com.example.orcamento.dto.dashboard.DespesasMensaisDTO;
import com.example.orcamento.model.*;
import com.example.orcamento.model.enums.FormaDePagamento;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.MovimentacaoRepository;
import com.example.orcamento.repository.SubcategoriaDespesaRepository;
import com.example.orcamento.specification.DespesaSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    @Autowired
    private SubcategoriaDespesaRepository subcategoriaDespesaRepository;
    @Autowired
    private FileStorageService fileStorageService;

    public List<Despesa> listarDespesas() {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return despesaRepository.findByTenantId(tenantId);
    }

    @Transactional
    public Despesa salvarDespesa(Despesa despesa) {
        if (despesa.getContaCorrente() != null && despesa.getContaCorrente().getId() == null) {
            throw new IllegalArgumentException("Conta corrente inválida para salvar a despesa");
        }
        if (despesa.getValorPrevisto() == null || despesa.getValorPrevisto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor previsto da despesa deve ser maior que zero");
        }

        // Associar subcategoria diretamente (novo fluxo simplificado)
        if (despesa.getSubcategoria() != null && despesa.getSubcategoria().getId() != null) {
            SubcategoriaDespesa subcategoria = subcategoriaDespesaRepository.findById(despesa.getSubcategoria().getId())
                .orElseThrow(() -> new EntityNotFoundException("Subcategoria não encontrada"));
            despesa.setSubcategoria(subcategoria);
        } else {
            throw new IllegalArgumentException("Subcategoria obrigatória para despesa");
        }

        log.info("Salvando uma despesa: {}", despesa);
        // Garante que a despesa salva pertence ao tenant logado
        despesa.setTenantId(com.example.orcamento.security.TenantContext.getTenantId());
        Despesa despesaSalva = despesaRepository.save(despesa);

        // Integração com MetaEconomia
        ConfiguracaoDTO configuracao = configuracaoService.getConfiguracoes();
        if (configuracao != null &&
                configuracao.getTipoDespesaInvestimentoId() != null &&
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
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        Despesa despesa = despesaRepository.findById(id)
                .filter(d -> tenantId.equals(d.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Despesa não encontrada ou não pertence ao tenant atual"));

        log.info("Excluindo a despesa: {}", despesa);

        // Exclui o anexo, se existir
        if (despesa.getAnexo() != null && !despesa.getAnexo().isEmpty()) {
            fileStorageService.deleteFile(despesa.getAnexo());
        }

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

    public List<Despesa> listarPorSubcategoria(Long subcategoriaId) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return despesaRepository.findBySubcategoriaId(tenantId, subcategoriaId);
    }

    @Transactional
    public Despesa atualizarPagamento(Long id, BigDecimal valorPago, LocalDate dataPagamento, Long contaCorrenteId, Long metaEconomiaId, FormaDePagamento formaPagamento) {
        Despesa despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Despesa não encontrada"));

        if (despesa.getDataPagamento() != null) {
            throw new IllegalStateException("Despesa já foi paga");
        }

        despesa.setValorPago(valorPago);
        despesa.setDataPagamento(dataPagamento);
        despesa.setFormaDePagamento(formaPagamento);

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

        // Lógica de integração com MetaEconomia (Refatorada)
        if (despesa.getMetaEconomia() != null && despesa.getSubcategoria() != null) {
            SubcategoriaDespesa subcategoria = despesa.getSubcategoria();
            // Assumindo que SubcategoriaDespesa tem uma referência para CategoriaDespesa
            // e que o nome da categoria de investimentos é "Investimentos"
            if (subcategoria.getCategoria() != null && "Investimentos".equalsIgnoreCase(subcategoria.getCategoria().getNome())) {
                log.info("Despesa de investimento identificada. Atualizando a meta de economia...");
                MetaEconomia meta = despesa.getMetaEconomia();
                meta.setValorEconomizado(meta.getValorEconomizado() + valorPago.doubleValue());
                metaEconomiaService.salvarMeta(meta);
                log.info("Meta de economia atualizada com sucesso.");
            } else {
                log.info("Despesa não é da categoria 'Investimentos'. Nenhuma meta foi atualizada.");
            }
        } else {
            log.info("Despesa não está associada a uma meta ou subcategoria. Nenhuma meta foi atualizada.");
        }

        return despesaRepository.save(despesa);
    }

    @Transactional
    public Despesa atualizarDespesa(Long id, Despesa despesaAtualizada) {
        return atualizarDespesa(id, despesaAtualizada, null);
    }

    @Transactional
    public Despesa atualizarDespesa(Long id, Despesa despesaAtualizada, MultipartFile anexo) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        Despesa despesa = despesaRepository.findById(id)
                .filter(d -> tenantId.equals(d.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Despesa não encontrada ou não pertence ao tenant atual"));

        log.info("Atualizando despesa: {} para {}", despesa, despesaAtualizada);

        // Lógica de anexo
        if (anexo != null && !anexo.isEmpty()) {
            // Se já existe um anexo, exclui o antigo
            if (despesa.getAnexo() != null && !despesa.getAnexo().isEmpty()) {
                fileStorageService.deleteFile(despesa.getAnexo());
            }
            String fileName = fileStorageService.storeFile(anexo);
            despesa.setAnexo(fileName);
        }

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
        despesa.setSubcategoria(despesaAtualizada.getSubcategoria());

        // Associar subcategoria diretamente (novo fluxo simplificado)
        if (despesaAtualizada.getSubcategoria() != null && despesaAtualizada.getSubcategoria().getId() != null) {
            SubcategoriaDespesa subcategoria = subcategoriaDespesaRepository.findById(despesaAtualizada.getSubcategoria().getId())
                .orElseThrow(() -> new EntityNotFoundException("Subcategoria não encontrada"));
            despesa.setSubcategoria(subcategoria);
        } else {
            throw new IllegalArgumentException("Subcategoria obrigatória para despesa");
        }

        despesa.setClassificacao(despesaAtualizada.getClassificacao());
        despesa.setVariabilidade(despesaAtualizada.getVariabilidade());
        despesa.setFormaDePagamento(despesaAtualizada.getFormaDePagamento());

        // Restaura os dados de pagamento originais
        despesa.setValorPago(valorPagoOriginal);
        despesa.setDataPagamento(dataPagamentoOriginal);
        despesa.setContaCorrente(contaCorrenteOriginal);

        log.info("Despesa atualizada: {}", despesa);

        return despesaRepository.save(despesa);
    }

    public Map<String, Map<String, BigDecimal>> listarPorMes(int ano) {
        return listarPorMes(ano, null);
    }

    public Map<String, Map<String, BigDecimal>> listarPorMes(int ano, Long subcategoriaId) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<Despesa> despesas = subcategoriaId != null
                ? despesaRepository.findByAnoAndSubcategoriaId(tenantId, ano, subcategoriaId)
                : despesaRepository.findDespesasByAno(tenantId, ano);

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
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        // 1. Gastos das despesas comuns
        List<Despesa> despesas = despesaRepository.findByAnoAndMes(tenantId, ano, mes);
        log.info("Despesas encontradas: {}", despesas.size());
        Map<Long, Double> gastosPorCategoria = new HashMap<>();

        for (Despesa despesa : despesas) {
            Long categoriaId = despesa.getSubcategoria().getId();
            BigDecimal valor = despesa.getValorPago() != null ? despesa.getValorPago() : despesa.getValorPrevisto();
            double valorDouble = valor.doubleValue();
            gastosPorCategoria.put(categoriaId, gastosPorCategoria.getOrDefault(categoriaId, 0.0) + valorDouble);
        }

        // 2. Gastos das faturas de cartão (não lançadas como despesa)
        String tenantIdCartao = com.example.orcamento.security.TenantContext.getTenantId();
        List<LancamentoCartao> lancamentos = lancamentoCartaoRepository.findByAnoAndMesAndTenantId(ano, mes, tenantIdCartao);
        log.info("Lançamentos de cartão encontrados: {}", lancamentos.size());
        for (LancamentoCartao lancamento : lancamentos) {
            Long subcategoriaId = lancamento.getSubcategoria() != null ? lancamento.getSubcategoria().getId() : null;
            double valor = lancamento.getValorTotal().doubleValue();
            if (subcategoriaId != null) {
                gastosPorCategoria.put(subcategoriaId, gastosPorCategoria.getOrDefault(subcategoriaId, 0.0) + valor);
            }
        }

        log.info("Mapa final de gastos por categoria (por id): {}", gastosPorCategoria);
        return gastosPorCategoria;
    }

    public List<Despesa> listarProximasEVencidas(LocalDate dataReferencia, LocalDate dataFim) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return despesaRepository.findVencidasEProximas(tenantId, dataReferencia, dataFim);
    }

    public List<Despesa> listarDespesasPorPeriodo(LocalDate inicio, LocalDate fim) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return despesaRepository.findByTenantIdAndDataVencimentoBetween(tenantId, inicio, fim);
    }

    public List<DespesasMensaisDTO> buscarDespesasPorAno(int ano) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        // Busca todas as despesas do ano informado
        List<Despesa> despesas = despesaRepository.findDespesasByAno(tenantId, ano);

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
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        // Buscar todas as despesas que são parcelas desta despesa parcelada
        List<Despesa> parcelas = despesaRepository.findByDespesaParceladaId(tenantId, despesaParceladaId);

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
                despesa.getSubcategoria() != null &&
                despesa.getSubcategoria().getId().equals(configuracao.getTipoDespesaInvestimentoId()) &&
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

    public List<Despesa> buscarDespesasRelacionadas(Long metaId) {
        // Verificar se a meta existe
        MetaEconomia meta = metaEconomiaService.buscarPorId(metaId)
                .orElseThrow(() -> new EntityNotFoundException("Meta não encontrada com ID: " + metaId));
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        // Buscar despesas relacionadas
        return despesaRepository.findByMetaEconomiaId(tenantId, metaId);
    }

    public List<Despesa> getDespesasByMetaEconomia(Long metaEconomiaId) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return despesaRepository.findByMetaEconomiaId(tenantId, metaEconomiaId);
    }

    public boolean verificarFaturaLancada(String nomeCartao, int mes, int ano) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        String nomeBusca = "Fatura Cartão " + nomeCartao;
        return despesaRepository.existsByNomeLikeAndMesAndAno(tenantId, nomeBusca, mes, ano);
    }

    public List<Despesa> listarDespesasPorFiltrosDinamicos(Map<String, Object> filtros) {
        log.info("Buscando despesas com filtros dinâmicos: {}", filtros);
        Map<String, Object> filtrosMapeados = new HashMap<>();
        filtros.forEach((key, value) -> {
            switch (key) {
                case "descricao" -> filtrosMapeados.put("nome", value);
                case "valor" -> filtrosMapeados.put("valorPrevisto", value);
                case "tipoDespesaId" -> filtrosMapeados.put("subcategoriaId", value);
                case "dataInicio" -> filtrosMapeados.put("dataVencimentoInicio", value);
                case "dataFim" -> filtrosMapeados.put("dataVencimentoFim", value);
                default -> filtrosMapeados.put(key, value); // id, detalhes, classificacao, variabilidade, parcela
            }
        });
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return despesaRepository.findAll(DespesaSpecification.comFiltros(tenantId, filtrosMapeados));
    }

    public List<Despesa> filtrarDinamico(Map<String, String> params) {
        Map<String, Object> filtros = new HashMap<>();
        filtros.putAll(params);
        return listarDespesasPorFiltrosDinamicos(filtros);
    }

    public SubcategoriaDespesa buscarSubcategoriaPorId(Long subcategoriaId) {
        return subcategoriaDespesaRepository.findById(subcategoriaId)
                .orElseThrow(() -> new EntityNotFoundException("Subcategoria não encontrada: " + subcategoriaId));
    }
}