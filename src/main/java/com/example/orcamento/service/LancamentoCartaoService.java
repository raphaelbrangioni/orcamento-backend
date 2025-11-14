// src/main/java/com/example/orcamento/service/LancamentoCartaoService.java
package com.example.orcamento.service;

import com.example.orcamento.dto.dashboard.FaturaCartaoAnualDTO;
import com.example.orcamento.dto.dashboard.FaturaMensalDTO;
import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.CategoriaDespesa;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.specification.LancamentoCartaoSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import com.example.orcamento.dto.CompraDTO;
import com.example.orcamento.dto.LancamentoCartaoDetalhadoDTO;
import com.example.orcamento.dto.LancamentoCartaoComCompraDTO;
import com.example.orcamento.dto.CategoriaDespesaDTO;
import com.example.orcamento.dto.SubcategoriaDespesaDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class LancamentoCartaoService {
    private final LancamentoCartaoRepository lancamentoCartaoRepository;
    private final CartaoCreditoRepository cartaoCreditoRepository;
    private final DespesaService despesaService;

    public LancamentoCartao cadastrarLancamento(LancamentoCartao lancamento) {
        log.info("Lançamento a ser salvo: {}", lancamento);
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
            builder.compra(new CompraDTO(lancamento.getCompra()));
        }

        return builder.build();
    }

    public void excluirLancamento(Long id) {
        lancamentoCartaoRepository.deleteById(id);
    }

    @Transactional
    public LancamentoCartao atualizarLancamento(Long id, LancamentoCartao lancamentoAtualizado) {
        LancamentoCartao lancamentoExistente = lancamentoCartaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento com ID " + id + " não encontrado."));

        // Atualiza apenas os campos que podem ser modificados
        lancamentoExistente.setDescricao(lancamentoAtualizado.getDescricao());
        lancamentoExistente.setValorTotal(lancamentoAtualizado.getValorTotal());
        lancamentoExistente.setProprietario(lancamentoAtualizado.getProprietario());
        lancamentoExistente.setMesAnoFatura(lancamentoAtualizado.getMesAnoFatura());
        lancamentoExistente.setDataRegistro(lancamentoAtualizado.getDataRegistro());
        lancamentoExistente.setPagoPorTerceiro(lancamentoAtualizado.getPagoPorTerceiro());
        lancamentoExistente.setDetalhes(lancamentoAtualizado.getDetalhes());

        if (lancamentoAtualizado.getSubcategoria() != null) {
            lancamentoExistente.setSubcategoria(lancamentoAtualizado.getSubcategoria());
        }

        if (lancamentoAtualizado.getClassificacao() != null) {
            lancamentoExistente.setClassificacao(lancamentoAtualizado.getClassificacao());
        }

        // Se um novo cartão de crédito for fornecido, atualiza a associação
        if (lancamentoAtualizado.getCartaoCredito() != null && lancamentoAtualizado.getCartaoCredito().getId() != null) {
            CartaoCredito novoCartao = cartaoCreditoRepository.findById(lancamentoAtualizado.getCartaoCredito().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Cartão de crédito com ID " + lancamentoAtualizado.getCartaoCredito().getId() + " não encontrado."));
            lancamentoExistente.setCartaoCredito(novoCartao);
        }

        // As informações de parcela não devem ser alteradas em uma atualização simples.
        // Se for necessário, um método específico para isso deve ser criado.

        validarLancamento(lancamentoExistente);
        return lancamentoCartaoRepository.save(lancamentoExistente);
    }

    private void validarLancamento(LancamentoCartao lancamento) {
        lancamento.setCartaoCredito(cartaoCreditoRepository.findById(lancamento.getCartaoCredito().getId())
                .orElseThrow(() -> new IllegalArgumentException("Cartão de crédito com ID " + lancamento.getCartaoCredito().getId() + " não encontrado.")));

        if (lancamento.getCartaoCredito().getDiaVencimento() == null) {
            throw new IllegalArgumentException("O dia de vencimento do cartão não pode ser nulo.");
        }
        if (lancamento.getMesAnoFatura() == null) {
            throw new IllegalArgumentException("O mês/ano da fatura não pode ser nulo.");
        }
        if (lancamento.getParcelaAtual() == null || lancamento.getTotalParcelas() == null) {
            throw new IllegalArgumentException("Parcela atual e total de parcelas não podem ser nulos.");
        }
        if (lancamento.getParcelaAtual() > lancamento.getTotalParcelas()) {
            throw new IllegalArgumentException("A parcela atual não pode ser maior que o total de parcelas.");
        }
        if (lancamento.getParcelaAtual() < 1 || lancamento.getTotalParcelas() < 1) {
            throw new IllegalArgumentException("Parcela atual e total de parcelas devem ser maiores que zero.");
        }
    }

    public List<FaturaCartaoAnualDTO> getFaturasAnuais(int ano) {
        List<Long> cartoesIds = cartaoCreditoRepository.findAllCartaoIds();

        return cartoesIds.stream()
                .map(cartaoId -> {
                    FaturaCartaoAnualDTO dto = new FaturaCartaoAnualDTO();
                    dto.setCartaoId(cartaoId);

                    var cartao = cartaoCreditoRepository.findById(cartaoId).orElse(null);
                    if (cartao == null) return null; // Ou lança uma exceção

                    Map<String, FaturaMensalDTO> faturasPorMes = new LinkedHashMap<>();
                    for (int mesNum = 1; mesNum <= 12; mesNum++) {
                        String mesNome = mesParaString(mesNum);
                        String mesAnoFatura = mesNome + "/" + ano;
                        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();

                        BigDecimal valorFatura = lancamentoCartaoRepository
                                .getFaturaDoMes(cartaoId, mesAnoFatura, tenantId);

                        BigDecimal valorTerceiros = lancamentoCartaoRepository
                                .getFaturaDoMesTerceiros(cartaoId, mesAnoFatura, tenantId);

                        //log.info("Cartão: {}, Mes: {}, Fatura: {}", cartao.getNome(), mesNum, ano);

                        boolean faturaLancada = despesaService.verificarFaturaLancada(cartao.getNome(), mesNum, ano);

                        com.example.orcamento.dto.dashboard.FaturaMensalDTO faturaMensalDTO = new com.example.orcamento.dto.dashboard.FaturaMensalDTO(
                                valorFatura != null ? valorFatura : BigDecimal.ZERO,
                                faturaLancada,
                                valorTerceiros != null ? valorTerceiros : BigDecimal.ZERO
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
        log.info("Buscando lançamentos com filtros - cartaoId: {}, mesAnoFatura: {}", cartaoId, mesAnoFatura);

        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<LancamentoCartao> lancamentos = lancamentoCartaoRepository.findByCartaoAndMesAno(cartaoId, mesAnoFatura, tenantId);
        return lancamentos;
    }

    public LancamentoCartao atualizarStatusPagamento(Long id, Boolean pagoPorTerceiro) {
        LancamentoCartao lancamento = lancamentoCartaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento com ID " + id + " não encontrado."));

        // Só atualiza o status se o lançamento for de terceiros
        if (lancamento.getProprietario() != null && lancamento.getProprietario().equals("Terceiros")) {
            lancamento.setPagoPorTerceiro(pagoPorTerceiro);
            return lancamentoCartaoRepository.save(lancamento);
        } else {
            throw new IllegalArgumentException("Apenas lançamentos de terceiros podem ter o status de pagamento alterado.");
        }
    }

    public List<LancamentoCartao> listarLancamentosTerceiros(String mesAnoFatura) {
        log.info("Buscando lançamentos de terceiros com filtro mesAnoFatura: {}", mesAnoFatura);
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        if (mesAnoFatura != null && !mesAnoFatura.trim().isEmpty()) {
            return lancamentoCartaoRepository.findByProprietarioAndMesAnoFaturaAndTenantId("Terceiros", mesAnoFatura, tenantId);
        } else {
            return lancamentoCartaoRepository.findByProprietarioAndTenantId("Terceiros", tenantId);
        }
    }

    public List<LancamentoCartao> listarLancamentosPorFiltrosDinamicos(Map<String, Object> filtros) {
        log.info("Buscando lançamentos com filtros dinâmicos: {}", filtros);
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        filtros.put("tenantId", tenantId);

        return lancamentoCartaoRepository.findAll(LancamentoCartaoSpecification.comFiltros(filtros));
    }

    public LancamentoCartaoDetalhadoDTO buscarLancamentoComCompra(Long id) {
        LancamentoCartao lancamento = lancamentoCartaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento com ID " + id + " não encontrado."));

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
            builder.compra(new CompraDTO(lancamento.getCompra()));
        }

        return builder.build();
    }
}