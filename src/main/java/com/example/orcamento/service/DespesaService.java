package com.example.orcamento.service;

import com.example.orcamento.dto.ConfiguracaoDTO;
import com.example.orcamento.dto.dashboard.DespesasMensaisDTO;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.model.MetaEconomia;
import com.example.orcamento.model.Movimentacao;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.model.TipoMovimentacao;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            throw new IllegalArgumentException("Conta corrente invalida para salvar a despesa");
        }
        if (despesa.getValorPrevisto() == null || despesa.getValorPrevisto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor previsto da despesa deve ser maior que zero");
        }

        if (despesa.getSubcategoria() != null && despesa.getSubcategoria().getId() != null) {
            SubcategoriaDespesa subcategoria = buscarSubcategoriaPorId(despesa.getSubcategoria().getId());
            despesa.setSubcategoria(subcategoria);
        } else {
            throw new IllegalArgumentException("Subcategoria obrigatoria para despesa");
        }

        log.info("Salvando uma despesa: {}", despesa);
        despesa.setTenantId(com.example.orcamento.security.TenantContext.getTenantId());
        Despesa despesaSalva = despesaRepository.save(despesa);

        ConfiguracaoDTO configuracao = configuracaoService.getConfiguracoes();
        if (configuracao != null
                && configuracao.getTipoDespesaInvestimentoId() != null
                && despesaSalva.getMetaEconomia() != null
                && despesaSalva.getValorPago() != null
                && despesaSalva.getDataPagamento() != null) {

            log.info("Integracao com MetaEconomia. Atualizando: {}", despesa.getMetaEconomia());

            MetaEconomia meta = metaEconomiaService.buscarPorId(despesaSalva.getMetaEconomia().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Meta nao encontrada"));
            meta.setValorEconomizado(meta.getValorEconomizado() + despesaSalva.getValorPago().doubleValue());
            metaEconomiaService.salvarMeta(meta);
        }

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
    public Despesa criarDespesaComPagamento(Despesa despesa, BigDecimal valorPago, LocalDate dataPagamento, Long contaCorrenteId, Long metaEconomiaId, FormaDePagamento formaPagamento) {
        if (despesa == null) {
            throw new IllegalArgumentException("Despesa e obrigatoria");
        }
        if (valorPago == null || valorPago.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("valorPago e obrigatorio e deve ser maior que zero");
        }
        if (dataPagamento == null) {
            throw new IllegalArgumentException("dataPagamento e obrigatoria");
        }
        if (contaCorrenteId == null) {
            throw new IllegalArgumentException("contaCorrenteId e obrigatorio");
        }

        despesa.setValorPago(valorPago);
        despesa.setDataPagamento(dataPagamento);
        despesa.setFormaDePagamento(formaPagamento);

        ContaCorrente conta = contaCorrenteService.buscarPorId(contaCorrenteId)
                .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada: " + contaCorrenteId));
        despesa.setContaCorrente(conta);

        if (metaEconomiaId != null) {
            MetaEconomia meta = metaEconomiaService.buscarPorId(metaEconomiaId)
                    .orElseThrow(() -> new IllegalArgumentException("Meta nao encontrada: " + metaEconomiaId));
            despesa.setMetaEconomia(meta);
        }

        return salvarDespesa(despesa);
    }

    @Transactional
    public void excluirDespesa(Long id) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        Despesa despesa = buscarDespesaPorId(id);

        log.info("Excluindo a despesa: {}", despesa);

        if (despesa.getAnexo() != null && !despesa.getAnexo().isEmpty()) {
            fileStorageService.deleteFile(despesa.getAnexo());
        }

        List<Movimentacao> movimentacoes = movimentacaoRepository.findByDespesa(despesa);
        if (!movimentacoes.isEmpty()) {
            movimentacaoRepository.deleteAll(movimentacoes);
        }

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

        despesaRepository.deleteByIdAndTenantId(id, tenantId);
    }

    public List<Despesa> listarPorSubcategoria(Long subcategoriaId) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return despesaRepository.findBySubcategoriaId(tenantId, subcategoriaId);
    }

    @Transactional
    public Despesa atualizarPagamento(Long id, BigDecimal valorPago, LocalDate dataPagamento, Long contaCorrenteId, Long metaEconomiaId, FormaDePagamento formaPagamento) {
        Despesa despesa = buscarDespesaPorId(id);

        if (despesa.getDataPagamento() != null) {
            throw new IllegalStateException("Despesa ja foi paga");
        }

        despesa.setValorPago(valorPago);
        despesa.setDataPagamento(dataPagamento);
        despesa.setFormaDePagamento(formaPagamento);

        if (contaCorrenteId != null) {
            ContaCorrente conta = contaCorrenteService.buscarPorId(contaCorrenteId)
                    .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada: " + contaCorrenteId));
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

        if (despesa.getMetaEconomia() != null && despesa.getSubcategoria() != null) {
            SubcategoriaDespesa subcategoria = despesa.getSubcategoria();
            if (subcategoria.getCategoria() != null && "Investimentos".equalsIgnoreCase(subcategoria.getCategoria().getNome())) {
                log.info("Despesa de investimento identificada. Atualizando a meta de economia...");
                MetaEconomia meta = despesa.getMetaEconomia();
                meta.setValorEconomizado(meta.getValorEconomizado() + valorPago.doubleValue());
                metaEconomiaService.salvarMeta(meta);
                log.info("Meta de economia atualizada com sucesso.");
            } else {
                log.info("Despesa nao e da categoria 'Investimentos'. Nenhuma meta foi atualizada.");
            }
        } else {
            log.info("Despesa nao esta associada a uma meta ou subcategoria. Nenhuma meta foi atualizada.");
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
        Despesa despesa = buscarDespesaPorId(id);

        log.info("Atualizando despesa: {} para {}", despesa, despesaAtualizada);

        if (anexo != null && !anexo.isEmpty()) {
            if (despesa.getAnexo() != null && !despesa.getAnexo().isEmpty()) {
                fileStorageService.deleteFile(despesa.getAnexo());
            }
            String fileName = fileStorageService.storeFile(anexo);
            despesa.setAnexo(fileName);
        }

        BigDecimal valorPagoOriginal = despesa.getValorPago();
        LocalDate dataPagamentoOriginal = despesa.getDataPagamento();
        ContaCorrente contaCorrenteOriginal = despesa.getContaCorrente();

        despesa.setNome(despesaAtualizada.getNome());
        despesa.setValorPrevisto(despesaAtualizada.getValorPrevisto());
        despesa.setDataVencimento(despesaAtualizada.getDataVencimento());
        despesa.setParcela(despesaAtualizada.getParcela());
        despesa.setDetalhes(despesaAtualizada.getDetalhes());
        despesa.setSubcategoria(despesaAtualizada.getSubcategoria());

        if (despesaAtualizada.getSubcategoria() != null && despesaAtualizada.getSubcategoria().getId() != null) {
            SubcategoriaDespesa subcategoria = buscarSubcategoriaPorId(despesaAtualizada.getSubcategoria().getId());
            despesa.setSubcategoria(subcategoria);
        } else {
            throw new IllegalArgumentException("Subcategoria obrigatoria para despesa");
        }

        despesa.setClassificacao(despesaAtualizada.getClassificacao());
        despesa.setVariabilidade(despesaAtualizada.getVariabilidade());
        despesa.setFormaDePagamento(despesaAtualizada.getFormaDePagamento());

        despesa.setValorPago(valorPagoOriginal);
        despesa.setDataPagamento(dataPagamentoOriginal);
        despesa.setContaCorrente(contaCorrenteOriginal);
        despesa.setTenantId(tenantId);

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
        List<Despesa> despesas = despesaRepository.findByAnoAndMes(tenantId, ano, mes);
        log.info("Despesas encontradas: {}", despesas.size());
        Map<Long, Double> gastosPorCategoria = new HashMap<>();

        for (Despesa despesa : despesas) {
            Long categoriaId = despesa.getSubcategoria().getId();
            BigDecimal valor = despesa.getValorPago() != null ? despesa.getValorPago() : despesa.getValorPrevisto();
            double valorDouble = valor.doubleValue();
            gastosPorCategoria.put(categoriaId, gastosPorCategoria.getOrDefault(categoriaId, 0.0) + valorDouble);
        }

        String tenantIdCartao = com.example.orcamento.security.TenantContext.getTenantId();
        List<LancamentoCartao> lancamentos = lancamentoCartaoRepository.findByAnoAndMesAndTenantId(ano, mes, tenantIdCartao);
        log.info("Lancamentos de cartao encontrados: {}", lancamentos.size());
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
        LocalDate dataFinalEfetiva = (dataFim != null) ? dataFim : dataReferencia;
        return despesaRepository.findVencidasEProximas(tenantId, dataReferencia, dataFinalEfetiva);
    }

    public List<Despesa> listarDespesasPorPeriodo(LocalDate inicio, LocalDate fim) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return despesaRepository.findByTenantIdAndDataVencimentoBetween(tenantId, inicio, fim);
    }

    public List<DespesasMensaisDTO> buscarDespesasPorAno(int ano) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<Despesa> despesas = despesaRepository.findDespesasByAno(tenantId, ano);

        Map<Integer, List<Despesa>> despesasPorMes = despesas.stream()
                .collect(Collectors.groupingBy(d -> d.getDataVencimento().getMonthValue()));

        return despesasPorMes.entrySet().stream()
                .map(entry -> new DespesasMensaisDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DespesasMensaisDTO::getMes))
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
        List<Despesa> parcelas = despesaRepository.findByDespesaParceladaId(tenantId, despesaParceladaId);

        for (Despesa parcela : parcelas) {
            excluirDespesa(parcela.getId());
        }

        log.info("Excluidas {} parcelas associadas a despesa parcelada ID {}", parcelas.size(), despesaParceladaId);
    }

    @Transactional
    public Despesa estornarPagamento(Long id) {
        Despesa despesa = buscarDespesaPorId(id);

        if (despesa.getValorPago() == null || despesa.getDataPagamento() == null || despesa.getContaCorrente() == null) {
            throw new IllegalStateException("Despesa nao possui pagamento para estornar");
        }

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

        ConfiguracaoDTO configuracao = configuracaoService.getConfiguracoes();
        if (configuracao != null
                && configuracao.getTipoDespesaInvestimentoId() != null
                && despesa.getSubcategoria() != null
                && despesa.getSubcategoria().getId().equals(configuracao.getTipoDespesaInvestimentoId())
                && despesa.getMetaEconomia() != null) {

            MetaEconomia meta = metaEconomiaService.buscarPorId(despesa.getMetaEconomia().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Meta nao encontrada"));
            meta.setValorEconomizado(meta.getValorEconomizado() - despesa.getValorPago().doubleValue());
            metaEconomiaService.salvarMeta(meta);

            log.info("Estorno na meta de economia: {}, novo valor economizado: {}",
                    meta.getNome(), meta.getValorEconomizado());
        }

        despesa.setValorPago(null);
        despesa.setDataPagamento(null);
        despesa.setContaCorrente(null);

        return despesaRepository.save(despesa);
    }

    public List<Despesa> buscarDespesasRelacionadas(Long metaId) {
        metaEconomiaService.buscarPorId(metaId)
                .orElseThrow(() -> new EntityNotFoundException("Meta nao encontrada com ID: " + metaId));
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return despesaRepository.findByMetaEconomiaId(tenantId, metaId);
    }

    public List<Despesa> getDespesasByMetaEconomia(Long metaEconomiaId) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return despesaRepository.findByMetaEconomiaId(tenantId, metaEconomiaId);
    }

    public boolean verificarFaturaLancada(String nomeCartao, int mes, int ano) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        String nomeBusca = "Fatura Cartao " + nomeCartao;
        return despesaRepository.existsByNomeLikeAndMesAndAno(tenantId, nomeBusca, mes, ano);
    }

    public List<Despesa> listarDespesasPorFiltrosDinamicos(Map<String, Object> filtros) {
        log.info("Buscando despesas com filtros dinamicos: {}", filtros);
        Map<String, Object> filtrosMapeados = new HashMap<>();
        filtros.forEach((key, value) -> {
            switch (key) {
                case "descricao" -> filtrosMapeados.put("nome", value);
                case "valor" -> filtrosMapeados.put("valorPrevisto", value);
                case "tipoDespesaId" -> filtrosMapeados.put("subcategoriaId", value);
                case "dataInicio" -> filtrosMapeados.put("dataVencimentoInicio", value);
                case "dataFim" -> filtrosMapeados.put("dataVencimentoFim", value);
                default -> filtrosMapeados.put(key, value);
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
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return subcategoriaDespesaRepository.findByIdAndTenantId(subcategoriaId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Subcategoria nao encontrada: " + subcategoriaId));
    }

    private Despesa buscarDespesaPorId(Long id) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return despesaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Despesa nao encontrada ou nao pertence ao tenant atual"));
    }
}
