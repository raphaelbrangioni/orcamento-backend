// src/main/java/com/example/orcamento/service/LancamentoCartaoService.java
package com.example.orcamento.service;

import com.example.orcamento.dto.dashboard.FaturaCartaoAnualDTO;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.CartaoCreditoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LancamentoCartaoService {
    private final LancamentoCartaoRepository lancamentoCartaoRepository;
    private final CartaoCreditoRepository cartaoCreditoRepository;

    public LancamentoCartao cadastrarLancamento(LancamentoCartao lancamento) {
        log.info("Lançamento a ser salvo: {}", lancamento);
        validarLancamento(lancamento);
        return lancamentoCartaoRepository.save(lancamento);
    }

    @Transactional
    public List<LancamentoCartao> cadastrarMultiplosLancamentos(List<LancamentoCartao> lancamentos) {
        lancamentos.forEach(this::validarLancamento);
        return lancamentoCartaoRepository.saveAll(lancamentos);
    }

    public List<LancamentoCartao> listarLancamentos() {
        return lancamentoCartaoRepository.findAll();
    }

    public void excluirLancamento(Long id) {
        lancamentoCartaoRepository.deleteById(id);
    }

    public LancamentoCartao atualizarLancamento(Long id, LancamentoCartao lancamentoAtualizado) {
        LancamentoCartao lancamento = lancamentoCartaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento com ID " + id + " não encontrado."));
        lancamento.setDescricao(lancamentoAtualizado.getDescricao());
        lancamento.setValorTotal(lancamentoAtualizado.getValorTotal());
        lancamento.setParcelaAtual(lancamentoAtualizado.getParcelaAtual());
        lancamento.setTotalParcelas(lancamentoAtualizado.getTotalParcelas());
        lancamento.setDataCompra(lancamentoAtualizado.getDataCompra());
        lancamento.setDetalhes(lancamentoAtualizado.getDetalhes());
        lancamento.setMesAnoFatura(lancamentoAtualizado.getMesAnoFatura());
        lancamento.setTipoDespesa(lancamentoAtualizado.getTipoDespesa());
        lancamento.setPagoPorTerceiro(lancamentoAtualizado.getPagoPorTerceiro());
        lancamento.setCartaoCredito(cartaoCreditoRepository.findById(lancamentoAtualizado.getCartaoCredito().getId())
                .orElseThrow(() -> new IllegalArgumentException("Cartão de crédito com ID " + lancamentoAtualizado.getCartaoCredito().getId() + " não encontrado.")));
        validarLancamento(lancamento);
        return lancamentoCartaoRepository.save(lancamento);
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

                    // Usar LinkedHashMap para garantir a ordem dos meses
                    Map<String, BigDecimal> faturasPorMes = new LinkedHashMap<>();
                    List<String> mesesOrdenados = List.of(
                            "JANEIRO", "FEVEREIRO", "MARCO", "ABRIL", "MAIO",
                            "JUNHO", "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO",
                            "NOVEMBRO", "DEZEMBRO"
                    );

                    // Preenchendo o mapa com os meses na ordem fixa
                    for (String mes : mesesOrdenados) {
                        String mesAnoFatura = mes + "/" + ano;
                        BigDecimal valorFatura = lancamentoCartaoRepository
                                .getFaturaDoMes(cartaoId, mesAnoFatura);

                        faturasPorMes.put(mes, valorFatura != null ? valorFatura : BigDecimal.ZERO);
                    }

                    dto.setFaturasPorMes(faturasPorMes);
                    return dto;
                })
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

        List<LancamentoCartao> lancamentos = lancamentoCartaoRepository.findByCartaoAndMesAno(cartaoId, mesAnoFatura);
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
}