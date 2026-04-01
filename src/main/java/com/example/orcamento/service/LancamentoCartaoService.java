package com.example.orcamento.service;

import com.example.orcamento.dto.CategoriaDespesaDTO;
import com.example.orcamento.dto.LancamentoCartaoComCompraDTO;
import com.example.orcamento.dto.LancamentoCartaoDetalhadoDTO;
import com.example.orcamento.dto.SubcategoriaDespesaDTO;
import com.example.orcamento.dto.dashboard.FaturaCartaoAnualDTO;
import com.example.orcamento.dto.dashboard.FaturaMensalDTO;
import com.example.orcamento.mapper.CompraMapper;
import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.CategoriaDespesa;
import com.example.orcamento.model.GeracaoFaturaCartao;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.SubcategoriaDespesaRepository;
import com.example.orcamento.specification.LancamentoCartaoSpecification;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LancamentoCartaoService {
    private final LancamentoCartaoRepository lancamentoCartaoRepository;
    private final CartaoCreditoRepository cartaoCreditoRepository;
    private final SubcategoriaDespesaRepository subcategoriaDespesaRepository;
    private final DespesaService despesaService;
    private final CompraMapper compraMapper;
    private final GeracaoFaturaCartaoService geracaoFaturaCartaoService;

    public LancamentoCartao cadastrarLancamento(LancamentoCartao lancamento) {
        log.info("Lancamento a ser salvo: {}", lancamento);
        lancamento.setTenantId(com.example.orcamento.security.TenantContext.getTenantId());
        validarLancamento(lancamento);
        return lancamentoCartaoRepository.save(lancamento);
    }

    @Transactional
    public List<LancamentoCartao> cadastrarMultiplosLancamentos(List<LancamentoCartao> lancamentos) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        lancamentos.forEach(l -> {
            l.setTenantId(tenantId);
            validarLancamento(l);
        });
        return lancamentoCartaoRepository.saveAll(lancamentos);
    }

    public List<LancamentoCartao> listarLancamentos() {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return lancamentoCartaoRepository.findByTenantId(tenantId);
    }

    public List<LancamentoCartaoComCompraDTO> listarLancamentosComCompra() {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<LancamentoCartao> lancamentos = lancamentoCartaoRepository.findByTenantId(tenantId);
        return lancamentos.stream()
                .map(this::toLancamentoCartaoComCompraDTO)
                .collect(Collectors.toList());
    }

    private LancamentoCartaoComCompraDTO toLancamentoCartaoComCompraDTO(LancamentoCartao lancamento) {
        LancamentoCartaoComCompraDTO.LancamentoCartaoComCompraDTOBuilder builder = LancamentoCartaoComCompraDTO.builder()
                .id(lancamento.getId())
                .descricao(lancamento.getDescricao())
                .valorTotal(lancamento.getValorTotal())
                .parcelaAtual(lancamento.getParcelaAtual())
                .totalParcelas(lancamento.getTotalParcelas())
                .dataCompra(lancamento.getDataCompra())
                .detalhes(lancamento.getDetalhes())
                .mesAnoFatura(lancamento.getMesAnoFatura())
                .cartaoCreditoId(lancamento.getCartaoCredito() != null ? lancamento.getCartaoCredito().getId() : null)
                .proprietario(lancamento.getProprietario())
                .tenantId(lancamento.getTenantId())
                .dataRegistro(lancamento.getDataRegistro() != null ? lancamento.getDataRegistro().toLocalDate() : null)
                .pagoPorTerceiro(lancamento.getPagoPorTerceiro())
                .classificacao(lancamento.getClassificacao() != null ? lancamento.getClassificacao().name() : null)
                .variabilidade(lancamento.getVariabilidade() != null ? lancamento.getVariabilidade().name() : null);

        if (lancamento.getSubcategoria() != null) {
            SubcategoriaDespesa subcat = lancamento.getSubcategoria();
            if (subcat.getCategoria() != null) {
                CategoriaDespesa cat = subcat.getCategoria();
                builder.categoria(new CategoriaDespesaDTO(cat.getId(), cat.getNome(), new SubcategoriaDespesaDTO(subcat.getId(), subcat.getNome())));
            }
        }

        if (lancamento.getCompra() != null) {
            builder.compra(compraMapper.toDto(lancamento.getCompra()));
        }

        return builder.build();
    }

    public void excluirLancamento(Long id) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        buscarLancamentoPorId(id, tenantId);
        lancamentoCartaoRepository.deleteByIdAndTenantId(id, tenantId);
    }

    @Transactional
    public LancamentoCartao atualizarLancamento(Long id, LancamentoCartao lancamentoAtualizado) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        LancamentoCartao lancamentoExistente = buscarLancamentoPorId(id, tenantId);

        lancamentoExistente.setDescricao(lancamentoAtualizado.getDescricao());
        lancamentoExistente.setValorTotal(lancamentoAtualizado.getValorTotal());
        lancamentoExistente.setProprietario(lancamentoAtualizado.getProprietario());
        lancamentoExistente.setMesAnoFatura(lancamentoAtualizado.getMesAnoFatura());
        lancamentoExistente.setDataRegistro(lancamentoAtualizado.getDataRegistro());
        lancamentoExistente.setPagoPorTerceiro(lancamentoAtualizado.getPagoPorTerceiro());
        lancamentoExistente.setDetalhes(lancamentoAtualizado.getDetalhes());

        if (lancamentoAtualizado.getSubcategoria() != null && lancamentoAtualizado.getSubcategoria().getId() != null) {
            lancamentoExistente.setSubcategoria(buscarSubcategoriaPorId(lancamentoAtualizado.getSubcategoria().getId(), tenantId));
        }

        if (lancamentoAtualizado.getClassificacao() != null) {
            lancamentoExistente.setClassificacao(lancamentoAtualizado.getClassificacao());
        }

        if (lancamentoAtualizado.getVariabilidade() != null) {
            lancamentoExistente.setVariabilidade(lancamentoAtualizado.getVariabilidade());
        }

        if (lancamentoAtualizado.getCartaoCredito() != null && lancamentoAtualizado.getCartaoCredito().getId() != null) {
            lancamentoExistente.setCartaoCredito(buscarCartaoPorId(lancamentoAtualizado.getCartaoCredito().getId(), tenantId));
        }

        validarLancamento(lancamentoExistente);
        return lancamentoCartaoRepository.save(lancamentoExistente);
    }

    private void validarLancamento(LancamentoCartao lancamento) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        if (lancamento.getCartaoCredito() == null || lancamento.getCartaoCredito().getId() == null) {
            throw new IllegalArgumentException("Cartao de credito e obrigatorio.");
        }

        lancamento.setCartaoCredito(buscarCartaoPorId(lancamento.getCartaoCredito().getId(), tenantId));

        if (lancamento.getSubcategoria() != null && lancamento.getSubcategoria().getId() != null) {
            lancamento.setSubcategoria(buscarSubcategoriaPorId(lancamento.getSubcategoria().getId(), tenantId));
        }

        if (lancamento.getCartaoCredito().getDiaVencimento() == null) {
            throw new IllegalArgumentException("O dia de vencimento do cartao nao pode ser nulo.");
        }
        if (lancamento.getMesAnoFatura() == null) {
            throw new IllegalArgumentException("O mes/ano da fatura nao pode ser nulo.");
        }
        if (lancamento.getParcelaAtual() == null || lancamento.getTotalParcelas() == null) {
            throw new IllegalArgumentException("Parcela atual e total de parcelas nao podem ser nulos.");
        }
        if (lancamento.getParcelaAtual() > lancamento.getTotalParcelas()) {
            throw new IllegalArgumentException("A parcela atual nao pode ser maior que o total de parcelas.");
        }
        if (lancamento.getParcelaAtual() < 1 || lancamento.getTotalParcelas() < 1) {
            throw new IllegalArgumentException("Parcela atual e total de parcelas devem ser maiores que zero.");
        }
    }

    public List<FaturaCartaoAnualDTO> getFaturasAnuais(int ano) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        Map<String, GeracaoFaturaCartao> geracoesPorChave = geracaoFaturaCartaoService.listarGeracoesPorAnoMapeadas(ano);
        List<Long> cartoesIds = cartaoCreditoRepository.findByTenantId(tenantId).stream()
                .map(CartaoCredito::getId)
                .toList();

        return cartoesIds.stream()
                .map(cartaoId -> {
                    FaturaCartaoAnualDTO dto = new FaturaCartaoAnualDTO();
                    dto.setCartaoId(cartaoId);

                    CartaoCredito cartao = buscarCartaoPorId(cartaoId, tenantId);

                    Map<String, FaturaMensalDTO> faturasPorMes = new LinkedHashMap<>();
                    for (int mesNum = 1; mesNum <= 12; mesNum++) {
                        String mesNome = mesParaString(mesNum);
                        String mesAnoFatura = mesNome + "/" + ano;
                        GeracaoFaturaCartao geracao = geracoesPorChave.get(GeracaoFaturaCartaoService.chave(cartaoId, mesNum));

                        BigDecimal valorFatura = lancamentoCartaoRepository.getFaturaDoMes(cartaoId, mesAnoFatura, tenantId);
                        BigDecimal valorTerceiros = lancamentoCartaoRepository.getFaturaDoMesTerceiros(cartaoId, mesAnoFatura, tenantId);
                        boolean faturaLancada = despesaService.verificarFaturaLancada(cartao.getNome(), mesNum, ano);

                        FaturaMensalDTO faturaMensalDTO = new FaturaMensalDTO(
                                valorFatura != null ? valorFatura : BigDecimal.ZERO,
                                faturaLancada,
                                valorTerceiros != null ? valorTerceiros : BigDecimal.ZERO,
                                geracao != null ? geracao.getId() : null,
                                geracao != null ? geracao.getGeradoPor() : null,
                                geracao != null ? geracao.getGeradoEm() : null,
                                geracao != null ? geracao.getUltimoReprocessamentoPor() : null,
                                geracao != null ? geracao.getUltimoReprocessamentoEm() : null
                        );

                        faturasPorMes.put(mesNome, faturaMensalDTO);
                    }

                    dto.setFaturasPorMes(faturasPorMes);
                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String mesParaString(int mes) {
        return switch (mes) {
            case 1 -> "JANEIRO";
            case 2 -> "FEVEREIRO";
            case 3 -> "MARCO";
            case 4 -> "ABRIL";
            case 5 -> "MAIO";
            case 6 -> "JUNHO";
            case 7 -> "JULHO";
            case 8 -> "AGOSTO";
            case 9 -> "SETEMBRO";
            case 10 -> "OUTUBRO";
            case 11 -> "NOVEMBRO";
            case 12 -> "DEZEMBRO";
            default -> "DESCONHECIDO";
        };
    }

    public List<LancamentoCartao> listarLancamentosPorFiltros(Long cartaoId, String mesAnoFatura) {
        log.info("Buscando lancamentos com filtros - cartaoId: {}, mesAnoFatura: {}", cartaoId, mesAnoFatura);

        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return lancamentoCartaoRepository.findByCartaoAndMesAno(cartaoId, mesAnoFatura, tenantId);
    }

    public LancamentoCartao atualizarStatusPagamento(Long id, Boolean pagoPorTerceiro) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        LancamentoCartao lancamento = buscarLancamentoPorId(id, tenantId);

        if (lancamento.getProprietario() != null && lancamento.getProprietario().equals("Terceiros")) {
            lancamento.setPagoPorTerceiro(pagoPorTerceiro);
            return lancamentoCartaoRepository.save(lancamento);
        }
        throw new IllegalArgumentException("Apenas lancamentos de terceiros podem ter o status de pagamento alterado.");
    }

    public List<LancamentoCartao> listarLancamentosTerceiros(String mesAnoFatura) {
        log.info("Buscando lancamentos de terceiros com filtro mesAnoFatura: {}", mesAnoFatura);
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        if (mesAnoFatura != null && !mesAnoFatura.trim().isEmpty()) {
            return lancamentoCartaoRepository.findByProprietarioAndMesAnoFaturaAndTenantId("Terceiros", mesAnoFatura, tenantId);
        }
        return lancamentoCartaoRepository.findByProprietarioAndTenantId("Terceiros", tenantId);
    }

    public List<LancamentoCartao> listarLancamentosPorFiltrosDinamicos(Map<String, Object> filtros) {
        log.info("Buscando lancamentos com filtros dinamicos: {}", filtros);
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        filtros.put("tenantId", tenantId);

        return lancamentoCartaoRepository.findAll(LancamentoCartaoSpecification.comFiltros(filtros));
    }

    public LancamentoCartaoDetalhadoDTO buscarLancamentoComCompra(Long id) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        LancamentoCartao lancamento = buscarLancamentoPorId(id, tenantId);

        LancamentoCartaoDetalhadoDTO.LancamentoCartaoDetalhadoDTOBuilder builder = LancamentoCartaoDetalhadoDTO.builder()
                .id(lancamento.getId())
                .descricao(lancamento.getDescricao())
                .valorTotal(lancamento.getValorTotal())
                .parcelaAtual(lancamento.getParcelaAtual())
                .totalParcelas(lancamento.getTotalParcelas())
                .dataCompra(lancamento.getDataCompra())
                .detalhes(lancamento.getDetalhes())
                .mesAnoFatura(lancamento.getMesAnoFatura())
                .cartaoCreditoId(lancamento.getCartaoCredito() != null ? lancamento.getCartaoCredito().getId() : null)
                .proprietario(lancamento.getProprietario())
                .tenantId(lancamento.getTenantId())
                .dataRegistro(lancamento.getDataRegistro() != null ? lancamento.getDataRegistro().toLocalDate() : null)
                .pagoPorTerceiro(lancamento.getPagoPorTerceiro())
                .classificacao(lancamento.getClassificacao() != null ? lancamento.getClassificacao().name() : null)
                .variabilidade(lancamento.getVariabilidade() != null ? lancamento.getVariabilidade().name() : null);

        if (lancamento.getCompra() != null) {
            builder.compra(compraMapper.toDto(lancamento.getCompra()));
        }

        return builder.build();
    }

    private LancamentoCartao buscarLancamentoPorId(Long id, String tenantId) {
        return lancamentoCartaoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Lancamento com ID " + id + " nao encontrado para o tenant atual."));
    }

    private CartaoCredito buscarCartaoPorId(Long id, String tenantId) {
        return cartaoCreditoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Cartao de credito com ID " + id + " nao encontrado para o tenant atual."));
    }

    private SubcategoriaDespesa buscarSubcategoriaPorId(Long id, String tenantId) {
        return subcategoriaDespesaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Subcategoria nao encontrada para o tenant atual: " + id));
    }
}
