package com.example.orcamento.service;

import com.example.orcamento.dto.GeracaoFaturaCartaoPatchRequestDTO;
import com.example.orcamento.dto.GeracaoFaturaCartaoResponseDTO;
import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.GeracaoFaturaCartao;
import com.example.orcamento.model.StatusGeracaoFaturaCartao;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.model.TipoClassificacaoDespesa;
import com.example.orcamento.model.TipoVariabilidadeDespesa;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.GeracaoFaturaCartaoRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.SubcategoriaDespesaRepository;
import com.example.orcamento.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeracaoFaturaCartaoService {

    private static final String CATEGORIA_PAGAMENTO_FATURA = "Pagamento de fatura";
    private static final String SUBCATEGORIA_PAGAMENTO_FATURA = "Pagamento de fatura";

    private final GeracaoFaturaCartaoRepository geracaoFaturaCartaoRepository;
    private final CartaoCreditoRepository cartaoCreditoRepository;
    private final LancamentoCartaoRepository lancamentoCartaoRepository;
    private final DespesaRepository despesaRepository;
    private final DespesaService despesaService;
    private final SubcategoriaDespesaRepository subcategoriaDespesaRepository;

    @Transactional
    public GeracaoFaturaCartaoResponseDTO gerarFatura(Long cartaoCreditoId, int ano, int mes) {
        validarAnoMes(ano, mes);

        String tenantId = TenantContext.getTenantId();
        String username = obterUsernameAutenticado();
        CartaoCredito cartao = buscarCartao(cartaoCreditoId, tenantId);
        YearMonth competencia = YearMonth.of(ano, mes);

        BigDecimal valorFatura = obterValorFatura(cartao.getId(), competencia, tenantId);
        BigDecimal valorTerceiros = obterValorTerceiros(cartao.getId(), competencia, tenantId);
        BigDecimal valorProprio = valorFatura.subtract(valorTerceiros).max(BigDecimal.ZERO);

        if (valorFatura.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Nao existe valor de fatura para o cartao na competencia informada");
        }

        LocalDateTime agora = LocalDateTime.now();
        GeracaoFaturaCartao geracao = geracaoFaturaCartaoRepository
                .findByTenantIdAndCartaoCreditoIdAndAnoAndMes(tenantId, cartaoCreditoId, ano, mes)
                .orElseGet(GeracaoFaturaCartao::new);

        boolean reprocessamento = geracao.getId() != null;
        Despesa despesa = reprocessamento
                ? atualizarDespesaExistente(geracao.getDespesaId(), tenantId, cartao, competencia, valorProprio)
                : criarDespesaFatura(tenantId, cartao, competencia, valorProprio);

        geracao.setTenantId(tenantId);
        geracao.setCartaoCredito(cartao);
        geracao.setAno(ano);
        geracao.setMes(mes);
        geracao.setValorFatura(valorFatura);
        geracao.setValorTerceiros(valorTerceiros);
        geracao.setValorProprio(valorProprio);
        geracao.setDespesaId(despesa.getId());
        geracao.setStatus(reprocessamento ? StatusGeracaoFaturaCartao.REPROCESSADA : StatusGeracaoFaturaCartao.GERADA);
        if (!reprocessamento) {
            geracao.setGeradoPor(username);
            geracao.setGeradoEm(agora);
            geracao.setUltimoReprocessamentoPor(null);
            geracao.setUltimoReprocessamentoEm(null);
        } else {
            geracao.setUltimoReprocessamentoPor(username);
            geracao.setUltimoReprocessamentoEm(agora);
        }

        GeracaoFaturaCartao geracaoSalva = geracaoFaturaCartaoRepository.save(geracao);
        log.info(
                "geracao_fatura_cartao.processada geracaoId={} tenantId={} cartaoId={} ano={} mes={} despesaId={} status={} username={} valorFatura={} valorTerceiros={} valorProprio={}",
                geracaoSalva.getId(),
                tenantId,
                cartaoCreditoId,
                ano,
                mes,
                despesa.getId(),
                geracaoSalva.getStatus(),
                username,
                valorFatura,
                valorTerceiros,
                valorProprio
        );
        return toResponseDto(geracaoSalva, despesa);
    }

    @Transactional(readOnly = true)
    public Optional<GeracaoFaturaCartaoResponseDTO> buscarGeracao(Long cartaoCreditoId, int ano, int mes) {
        validarAnoMes(ano, mes);
        String tenantId = TenantContext.getTenantId();
        return geracaoFaturaCartaoRepository.findByTenantIdAndCartaoCreditoIdAndAnoAndMes(tenantId, cartaoCreditoId, ano, mes)
                .map(geracao -> {
                    Despesa despesa = geracao.getDespesaId() != null
                            ? despesaRepository.findByIdAndTenantId(geracao.getDespesaId(), tenantId).orElse(null)
                            : null;
                    return toResponseDto(geracao, despesa);
                });
    }

    @Transactional
    public GeracaoFaturaCartaoResponseDTO ajustarGeracao(Long geracaoId, GeracaoFaturaCartaoPatchRequestDTO request) {
        String tenantId = TenantContext.getTenantId();
        String username = obterUsernameAutenticado();
        GeracaoFaturaCartao geracao = geracaoFaturaCartaoRepository.findByIdAndTenantId(geracaoId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Geracao de fatura nao encontrada"));

        BigDecimal valorTerceiros = request.getValorTerceiros() != null ? request.getValorTerceiros() : geracao.getValorTerceiros();
        validarAjusteValores(geracao.getValorFatura(), valorTerceiros);

        Despesa despesa = geracao.getDespesaId() != null
                ? despesaRepository.findByIdAndTenantId(geracao.getDespesaId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Despesa da fatura nao encontrada"))
                : null;

        if (despesa != null && (despesa.getValorPago() != null || despesa.getDataPagamento() != null)) {
            throw new IllegalStateException("Nao e permitido ajustar uma geracao cuja despesa vinculada ja foi paga");
        }

        BigDecimal valorProprio = geracao.getValorFatura().subtract(valorTerceiros).max(BigDecimal.ZERO);
        geracao.setValorTerceiros(valorTerceiros);
        geracao.setValorProprio(valorProprio);
        geracao.setObservacao(request.getObservacao());
        geracao.setAjustadoPor(username);
        geracao.setAjustadoEm(LocalDateTime.now());

        if (despesa != null) {
            despesa.setValorPrevisto(valorProprio);
            despesaRepository.save(despesa);
        }

        GeracaoFaturaCartao geracaoSalva = geracaoFaturaCartaoRepository.save(geracao);
        return toResponseDto(geracaoSalva, despesa);
    }

    @Transactional(readOnly = true)
    public List<GeracaoFaturaCartaoResponseDTO> listarGeracoes(int ano, int mes) {
        validarAnoMes(ano, mes);
        String tenantId = TenantContext.getTenantId();
        return geracaoFaturaCartaoRepository.findByTenantIdAndAnoAndMesOrderByGeradoEmDesc(tenantId, ano, mes)
                .stream()
                .map(geracao -> {
                    Despesa despesa = geracao.getDespesaId() != null
                            ? despesaRepository.findByIdAndTenantId(geracao.getDespesaId(), tenantId).orElse(null)
                            : null;
                    return toResponseDto(geracao, despesa);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, GeracaoFaturaCartao> listarGeracoesPorAnoMapeadas(int ano) {
        String tenantId = TenantContext.getTenantId();
        return geracaoFaturaCartaoRepository.findByTenantIdAndAno(tenantId, ano).stream()
                .collect(Collectors.toMap(
                        geracao -> chave(geracao.getCartaoCredito().getId(), geracao.getMes()),
                        Function.identity(),
                        (primeiro, segundo) -> segundo
                ));
    }

    @Transactional(readOnly = true)
    public boolean verificarFaturaLancada(Long cartaoCreditoId, int ano, int mes, String nomeCartao) {
        String tenantId = TenantContext.getTenantId();
        boolean geracaoExiste = geracaoFaturaCartaoRepository
                .findByTenantIdAndCartaoCreditoIdAndAnoAndMes(tenantId, cartaoCreditoId, ano, mes)
                .isPresent();
        if (geracaoExiste) {
            return true;
        }
        return despesaService.verificarFaturaLancadaLegacy(nomeCartao, mes, ano);
    }

    private Despesa criarDespesaFatura(String tenantId, CartaoCredito cartao, YearMonth competencia, BigDecimal valorProprio) {
        Despesa despesa = Despesa.builder()
                .nome("Fatura Cartao " + cartao.getNome())
                .tenantId(tenantId)
                .valorPrevisto(valorProprio)
                .dataVencimento(obterDataVencimento(cartao, competencia))
                .detalhes("Lancamento de fatura prevista referente a " + formatarMesAnoAbreviado(competencia) + ".")
                .subcategoria(buscarSubcategoriaPagamentoFatura(tenantId))
                .classificacao(TipoClassificacaoDespesa.NECESSARIO)
                .variabilidade(TipoVariabilidadeDespesa.VARIAVEL)
                .build();
        return despesaService.salvarDespesa(despesa);
    }

    private Despesa atualizarDespesaExistente(Long despesaId, String tenantId, CartaoCredito cartao, YearMonth competencia, BigDecimal valorProprio) {
        if (despesaId == null) {
            throw new IllegalStateException("Geracao de fatura existente sem despesa vinculada");
        }
        Despesa despesa = despesaRepository.findByIdAndTenantId(despesaId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Despesa da fatura nao encontrada"));
        if (despesa.getValorPago() != null || despesa.getDataPagamento() != null) {
            throw new IllegalStateException("Nao e permitido reprocessar uma fatura cuja despesa ja foi paga");
        }

        despesa.setNome("Fatura Cartao " + cartao.getNome());
        despesa.setValorPrevisto(valorProprio);
        despesa.setDataVencimento(obterDataVencimento(cartao, competencia));
        despesa.setDetalhes("Lancamento de fatura prevista referente a " + formatarMesAnoAbreviado(competencia) + ".");
        despesa.setSubcategoria(buscarSubcategoriaPagamentoFatura(tenantId));
        despesa.setClassificacao(TipoClassificacaoDespesa.NECESSARIO);
        despesa.setVariabilidade(TipoVariabilidadeDespesa.VARIAVEL);
        return despesaRepository.save(despesa);
    }

    private SubcategoriaDespesa buscarSubcategoriaPagamentoFatura(String tenantId) {
        return subcategoriaDespesaRepository
                .findByNomeAndCategoriaNomeAndTenantId(
                        SUBCATEGORIA_PAGAMENTO_FATURA,
                        CATEGORIA_PAGAMENTO_FATURA,
                        tenantId
                )
                .orElseThrow(() -> new IllegalStateException("Subcategoria 'Pagamento de fatura' nao encontrada"));
    }

    private CartaoCredito buscarCartao(Long cartaoCreditoId, String tenantId) {
        return cartaoCreditoRepository.findByIdAndTenantId(cartaoCreditoId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Cartao de credito nao encontrado"));
    }

    private BigDecimal obterValorFatura(Long cartaoId, YearMonth competencia, String tenantId) {
        BigDecimal valor = lancamentoCartaoRepository.getFaturaDoMes(cartaoId, formatarMesAnoFatura(competencia), tenantId);
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private BigDecimal obterValorTerceiros(Long cartaoId, YearMonth competencia, String tenantId) {
        BigDecimal valor = lancamentoCartaoRepository.getFaturaDoMesTerceiros(cartaoId, formatarMesAnoFatura(competencia), tenantId);
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private LocalDate obterDataVencimento(CartaoCredito cartao, YearMonth competencia) {
        int dia = Math.min(cartao.getDiaVencimento(), competencia.lengthOfMonth());
        return competencia.atDay(dia);
    }

    private GeracaoFaturaCartaoResponseDTO toResponseDto(GeracaoFaturaCartao geracao, Despesa despesa) {
        return GeracaoFaturaCartaoResponseDTO.builder()
                .id(geracao.getId())
                .cartaoCreditoId(geracao.getCartaoCredito().getId())
                .nomeCartao(geracao.getCartaoCredito().getNome())
                .ano(geracao.getAno())
                .mes(geracao.getMes())
                .valorFatura(geracao.getValorFatura())
                .valorTerceiros(geracao.getValorTerceiros())
                .valorProprio(geracao.getValorProprio())
                .despesaId(geracao.getDespesaId())
                .nomeDespesa(despesa != null ? despesa.getNome() : null)
                .dataVencimentoDespesa(despesa != null ? despesa.getDataVencimento() : null)
                .status(geracao.getStatus())
                .geradoPor(geracao.getGeradoPor())
                .geradoEm(geracao.getGeradoEm())
                .ultimoReprocessamentoPor(geracao.getUltimoReprocessamentoPor())
                .ultimoReprocessamentoEm(geracao.getUltimoReprocessamentoEm())
                .observacao(geracao.getObservacao())
                .ajustadoPor(geracao.getAjustadoPor())
                .ajustadoEm(geracao.getAjustadoEm())
                .build();
    }

    private String formatarMesAnoFatura(YearMonth competencia) {
        String mesNome = competencia.getMonth()
                .getDisplayName(TextStyle.FULL, new Locale("pt", "BR"))
                .toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replace('Ã', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U');
        return mesNome + "/" + competencia.getYear();
    }

    private String formatarMesAnoAbreviado(YearMonth competencia) {
        String mesNome = competencia.getMonth()
                .getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"))
                .toUpperCase(Locale.ROOT)
                .replace(".", "");
        return mesNome + "/" + competencia.getYear();
    }

    private String obterUsernameAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "desconhecido";
        }
        return authentication.getName();
    }

    private void validarAnoMes(int ano, int mes) {
        if (ano < 2000 || ano > 3000) {
            throw new IllegalArgumentException("ano invalido");
        }
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("mes invalido");
        }
    }

    private void validarAjusteValores(BigDecimal valorFatura, BigDecimal valorTerceiros) {
        if (valorTerceiros == null) {
            throw new IllegalArgumentException("valorTerceiros e obrigatorio");
        }
        if (valorTerceiros.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("valorTerceiros nao pode ser negativo");
        }
        if (valorTerceiros.compareTo(valorFatura) > 0) {
            throw new IllegalArgumentException("valorTerceiros nao pode ser maior que valorFatura");
        }
    }

    public static String chave(Long cartaoId, int mes) {
        return cartaoId + "-" + mes;
    }
}
