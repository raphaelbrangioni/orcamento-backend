// src/main/java/com/example/orcamento/service/LancamentoCartaoService.java
package com.example.orcamento.service;

import com.example.orcamento.dto.dashboard.FaturaCartaoAnualDTO;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.specification.LancamentoCartaoSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        lancamento.setClassificacao(lancamentoAtualizado.getClassificacao());
        lancamento.setVariabilidade(lancamentoAtualizado.getVariabilidade());
        lancamento.setProprietario(lancamentoAtualizado.getProprietario());
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
                        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
                        BigDecimal valorFatura = lancamentoCartaoRepository
                                .getFaturaDoMes(cartaoId, mesAnoFatura, tenantId);

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

//    // Novo método para listar lançamentos com proprietario = "Terceiros"
//    public List<LancamentoCartao> listarLancamentosTerceiros() {
//        log.info("Buscando lançamentos com proprietario = Terceiros");
//        return lancamentoCartaoRepository.findByProprietario("Terceiros");
//    }

    // Método atualizado para incluir filtro opcional mesAnoFatura
    public List<LancamentoCartao> listarLancamentosTerceiros(String mesAnoFatura) {
        log.info("Buscando lançamentos com proprietario = Terceiros e mesAnoFatura = {}", mesAnoFatura);
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return lancamentoCartaoRepository.findByProprietarioAndMesAnoFatura("Terceiros", mesAnoFatura, tenantId);
    }

//    // Novo método para filtro dinâmico
//    public List<LancamentoCartao> listarLancamentosPorFiltrosDinamicos(Map<String, Object> filtros) {
//        log.info("Buscando lançamentos com filtros dinâmicos: {}", filtros);
//        return lancamentoCartaoRepository.findAll(LancamentoCartaoSpecification.comFiltros(filtros));
//    }

//    public List<LancamentoCartao> listarLancamentosPorFiltrosDinamicos(Map<String, Object> filtros) {
//        log.info("Buscando lançamentos com filtros dinâmicos: {}", filtros);
//        // Mapeia nomes de campos do DTO para os da entidade
//        Map<String, Object> filtrosMapeados = new HashMap<>();
//        filtros.forEach((key, value) -> {
//            switch (key) {
//                case "descricao" -> filtrosMapeados.put("descricao", value);
//                case "valor" -> filtrosMapeados.put("valorTotal", value);
//                case "dataReferencia" -> filtrosMapeados.put("dataCompra", value);
//                case "parcela" -> filtrosMapeados.put("parcelaAtual", value);
//                case "tipoDespesaId" -> filtrosMapeados.put("tipoDespesaId", value);
//                default -> filtrosMapeados.put(key, value); // Campos como id, detalhes, classificacao, variabilidade, totalParcelas são iguais
//            }
//        });
//        return lancamentoCartaoRepository.findAll(LancamentoCartaoSpecification.comFiltros(filtrosMapeados));
//    }

//    public List<LancamentoCartao> listarLancamentosPorFiltrosDinamicos(Map<String, Object> filtros) {
//        log.info("Buscando lançamentos com filtros dinâmicos: {}", filtros);
//        // Mapeia nomes de campos do DTO para os da entidade
//        Map<String, Object> filtrosMapeados = new HashMap<>();
//        filtros.forEach((key, value) -> {
//            switch (key) {
//                case "descricao" -> filtrosMapeados.put("descricao", value);
//                case "valor" -> filtrosMapeados.put("valorTotal", value);
//                case "dataReferencia" -> filtrosMapeados.put("mesAnoFatura", value); // Mapeia para mesAnoFatura
//                case "parcela" -> filtrosMapeados.put("parcelaAtual", value);
//                case "tipoDespesaId" -> filtrosMapeados.put("tipoDespesaId", value);
//                default -> filtrosMapeados.put(key, value); // Campos como id, detalhes, classificacao, variabilidade, totalParcelas
//            }
//        });
//        return lancamentoCartaoRepository.findAll(LancamentoCartaoSpecification.comFiltros(filtrosMapeados));
//    }

    public List<LancamentoCartao> listarLancamentosPorFiltrosDinamicos(Map<String, Object> filtros) {
        log.info("Buscando lançamentos com filtros dinâmicos: {}", filtros);
        Map<String, Object> filtrosMapeados = new HashMap<>();
        filtros.forEach((key, value) -> {
            switch (key) {
                case "descricao" -> filtrosMapeados.put("descricao", value);
                case "valor" -> filtrosMapeados.put("valorTotal", value);
                case "parcela" -> filtrosMapeados.put("parcelaAtual", value);
                case "tipoDespesaId" -> filtrosMapeados.put("tipoDespesaId", value);
                case "dataInicio" -> {
                    if (value != null && filtros.containsKey("dataFim")) {
                        List<String> mesesAnos = generateMesAnoList(
                                value.toString(),
                                filtros.get("dataFim").toString()
                        );
                        filtrosMapeados.put("mesAnoFaturaList", mesesAnos);
                    }
                }
                case "dataFim" -> {} // Ignorado, tratado junto com dataInicio
                default -> filtrosMapeados.put(key, value); // id, detalhes, classificacao, variabilidade, totalParcelas
            }
        });

        // Garante que mesAnoFatura simples também vira lista para o filtro funcionar
        if (filtros.containsKey("mesAnoFatura") && filtros.get("mesAnoFatura") != null && !filtrosMapeados.containsKey("mesAnoFaturaList")) {
            List<String> lista = new ArrayList<>();
            lista.add(filtros.get("mesAnoFatura").toString());
            filtrosMapeados.put("mesAnoFaturaList", lista);
        }
        return lancamentoCartaoRepository.findAll(LancamentoCartaoSpecification.comFiltros(filtrosMapeados));
    }

    // Método auxiliar para gerar lista de MES/ANO no intervalo
    private List<String> generateMesAnoList(String dataInicio, String dataFim) {
        try {
            LocalDate inicio = LocalDate.parse(dataInicio, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate fim = LocalDate.parse(dataFim, DateTimeFormatter.ISO_LOCAL_DATE);
            List<String> mesesAnos = new ArrayList<>();
            String[] meses = {
                    "JANEIRO", "FEVEREIRO", "MARCO", "ABRIL", "MAIO", "JUNHO",
                    "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"
            };

            while (!inicio.isAfter(fim)) {
                String mesAno = meses[inicio.getMonthValue() - 1] + "/" + inicio.getYear();
                mesesAnos.add(mesAno);
                inicio = inicio.plusMonths(1);
            }
            return mesesAnos;
        } catch (Exception e) {
            log.warn("Formato inválido para dataInicio: {} ou dataFim: {}. Retornando lista vazia.", dataInicio, dataFim);
            return List.of();
        }
    }

}