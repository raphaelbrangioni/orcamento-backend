package com.example.orcamento.service;

import com.example.orcamento.dto.conciliacao.ConciliacaoOfxRelatorioDTO;
import com.example.orcamento.dto.conciliacao.CreditoOrigemCartaoDTO;
import com.example.orcamento.dto.conciliacao.DespesaAmbiguaDTO;
import com.example.orcamento.dto.conciliacao.DespesaConciliacaoDTO;
import com.example.orcamento.dto.conciliacao.DespesaConciliadaDTO;
import com.example.orcamento.dto.conciliacao.MovimentoOfxDTO;
import com.example.orcamento.dto.conciliacao.ReceitaAmbiguaDTO;
import com.example.orcamento.dto.conciliacao.ReceitaConciliacaoDTO;
import com.example.orcamento.dto.conciliacao.ReceitaConciliadaDTO;
import com.example.orcamento.dto.conciliacao.TransferenciaConciliadaDTO;
import com.example.orcamento.model.ConciliacaoOfxProcessamento;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.Movimentacao;
import com.example.orcamento.model.Receita;
import com.example.orcamento.model.TipoMovimentacao;
import com.example.orcamento.repository.ConciliacaoOfxProcessamentoRepository;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.MovimentacaoRepository;
import com.example.orcamento.repository.ReceitaRepository;
import com.example.orcamento.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConciliacaoOfxService {

    private final ContaCorrenteService contaCorrenteService;
    private final ConciliacaoOfxProcessamentoRepository conciliacaoOfxProcessamentoRepository;
    private final DespesaRepository despesaRepository;
    private final ReceitaRepository receitaRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    public ConciliacaoOfxRelatorioDTO conciliar(
            Long contaCorrenteId,
            Integer toleranciaDias,
            BigDecimal toleranciaValor,
            BigDecimal toleranciaValorMinimo,
            MultipartFile file
    ) {
        String tenantId = TenantContext.getTenantId();
        String username = obterUsernameAutenticado();
        if (file == null || file.isEmpty()) {
            IllegalArgumentException exception = new IllegalArgumentException("Arquivo OFX e obrigatorio");
            salvarProcessamentoErro(tenantId, username, contaCorrenteId, toleranciaDias, toleranciaValor, toleranciaValorMinimo, file, exception);
            throw exception;
        }

        try {
            String hashArquivo = gerarHashArquivo(file);
            Optional<ConciliacaoOfxProcessamento> ultimoProcessamento = buscarUltimoProcessamentoMesmoArquivo(tenantId, contaCorrenteId, hashArquivo);
            ContaCorrente contaCorrente = contaCorrenteService.buscarPorId(contaCorrenteId)
                    .orElseThrow(() -> new EntityNotFoundException("Conta corrente nao encontrada para o tenant atual: " + contaCorrenteId));

            OfxExtrato extrato = parseOfx(file);
            Integer dias = toleranciaDias != null ? toleranciaDias : 2;
            BigDecimal toleranciaEmValor = toleranciaValor != null ? toleranciaValor : BigDecimal.ZERO;
            BigDecimal toleranciaMinima = toleranciaValorMinimo != null ? toleranciaValorMinimo : new BigDecimal("1000.00");

            List<Despesa> despesasPagas = despesaRepository
                    .findByTenantIdAndDataVencimentoBetween(contaCorrente.getTenantId(), extrato.periodoInicio(), extrato.periodoFim()).stream()
                    .filter(d -> d.getContaCorrente() != null && contaCorrenteId.equals(d.getContaCorrente().getId()))
                    .filter(d -> d.getValorPago() != null)
                    .toList();

            List<Receita> receitas = receitaRepository
                    .findByContaCorrenteIdAndTenantIdAndDataRecebimentoBetween(contaCorrenteId, contaCorrente.getTenantId(), extrato.periodoInicio(), extrato.periodoFim());

            List<Movimentacao> movimentacoes = movimentacaoRepository
                    .findByContaCorrenteIdAndTenantIdAndDataRecebimentoBetween(contaCorrenteId, contaCorrente.getTenantId(), extrato.periodoInicio(), extrato.periodoFim());

            Set<String> ofxDebitosConsumidos = new HashSet<>();
            Set<String> ofxCreditosConsumidos = new HashSet<>();
            Set<Long> despesasConsumidas = new HashSet<>();
            Set<Long> receitasConsumidas = new HashSet<>();

            List<CreditoOrigemCartaoDTO> creditosOrigemCartao = conciliarCreditosOrigemCartao(extrato.movimentos(), ofxDebitosConsumidos, ofxCreditosConsumidos);
            Set<Long> movimentacoesTransferenciaConsumidas = new HashSet<>();
            List<TransferenciaConciliadaDTO> transferenciasConciliadas = conciliarTransferencias(
                    extrato.movimentos(),
                    movimentacoes,
                    dias,
                    toleranciaEmValor,
                    toleranciaMinima,
                    ofxCreditosConsumidos,
                    ofxDebitosConsumidos,
                    movimentacoesTransferenciaConsumidas
            );

            ResultadoDespesas resultadoDespesas = conciliarDespesas(extrato.movimentos(), despesasPagas, dias, toleranciaEmValor, toleranciaMinima, ofxDebitosConsumidos, despesasConsumidas);
            ResultadoReceitas resultadoReceitas = conciliarReceitas(extrato.movimentos(), receitas, dias, toleranciaEmValor, toleranciaMinima, ofxCreditosConsumidos, receitasConsumidas);

            List<DespesaConciliacaoDTO> pagamentoSemBanco = despesasPagas.stream()
                    .filter(d -> !despesasConsumidas.contains(d.getId()))
                    .sorted(Comparator.comparing(Despesa::getDataPagamento, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(this::toDespesaDto)
                    .toList();

            List<ReceitaConciliacaoDTO> receitaSemBanco = receitas.stream()
                    .filter(r -> !receitasConsumidas.contains(r.getId()))
                    .sorted(Comparator.comparing(Receita::getDataRecebimento, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(this::toReceitaDto)
                    .toList();

            ConciliacaoOfxRelatorioDTO relatorio = ConciliacaoOfxRelatorioDTO.builder()
                    .contaCorrenteId(contaCorrenteId)
                    .periodoInicio(extrato.periodoInicio())
                    .periodoFim(extrato.periodoFim())
                    .toleranciaDias(dias)
                    .toleranciaValor(toleranciaEmValor)
                    .toleranciaValorMinimo(toleranciaMinima)
                    .bancoIdOfx(extrato.bankId())
                    .contaIdOfx(extrato.accountId())
                    .conciliados(resultadoDespesas.conciliados())
                    .bancoSemPagamento(resultadoDespesas.bancoSemPagamento())
                    .pagamentoSemBanco(pagamentoSemBanco)
                    .ambiguos(resultadoDespesas.ambiguos())
                    .receitasConciliadas(resultadoReceitas.conciliadas())
                    .transferenciasConciliadas(transferenciasConciliadas)
                    .creditosOrigemCartao(creditosOrigemCartao)
                    .bancoSemReceita(resultadoReceitas.bancoSemReceita())
                    .receitaSemBanco(receitaSemBanco)
                    .receitasAmbiguas(resultadoReceitas.ambiguas())
                    .arquivoJaProcessado(ultimoProcessamento.isPresent())
                    .ultimoProcessamentoEm(ultimoProcessamento.map(ConciliacaoOfxProcessamento::getProcessadoEm).orElse(null))
                    .build();

            salvarProcessamentoSucesso(tenantId, username, file, relatorio);
            return relatorio;
        } catch (RuntimeException exception) {
            salvarProcessamentoErro(tenantId, username, contaCorrenteId, toleranciaDias, toleranciaValor, toleranciaValorMinimo, file, exception);
            throw exception;
        }
    }

    private void salvarProcessamentoSucesso(
            String tenantId,
            String username,
            MultipartFile file,
            ConciliacaoOfxRelatorioDTO relatorio
    ) {
        conciliacaoOfxProcessamentoRepository.save(ConciliacaoOfxProcessamento.builder()
                .tenantId(tenantId)
                .username(username)
                .contaCorrenteId(relatorio.getContaCorrenteId())
                .nomeArquivo(obterNomeArquivo(file))
                .contentType(file != null ? file.getContentType() : null)
                .tamanhoArquivo(file != null ? file.getSize() : 0L)
                .hashArquivo(gerarHashArquivo(file))
                .bancoIdOfx(relatorio.getBancoIdOfx())
                .contaIdOfx(relatorio.getContaIdOfx())
                .periodoInicio(relatorio.getPeriodoInicio())
                .periodoFim(relatorio.getPeriodoFim())
                .toleranciaDias(relatorio.getToleranciaDias())
                .toleranciaValor(relatorio.getToleranciaValor() != null ? relatorio.getToleranciaValor() : BigDecimal.ZERO)
                .toleranciaValorMinimo(relatorio.getToleranciaValorMinimo() != null ? relatorio.getToleranciaValorMinimo() : BigDecimal.ZERO)
                .conciliadosQuantidade(size(relatorio.getConciliados()))
                .bancoSemPagamentoQuantidade(size(relatorio.getBancoSemPagamento()))
                .pagamentoSemBancoQuantidade(size(relatorio.getPagamentoSemBanco()))
                .ambiguosQuantidade(size(relatorio.getAmbiguos()))
                .receitasConciliadasQuantidade(size(relatorio.getReceitasConciliadas()))
                .transferenciasConciliadasQuantidade(size(relatorio.getTransferenciasConciliadas()))
                .creditosOrigemCartaoQuantidade(size(relatorio.getCreditosOrigemCartao()))
                .bancoSemReceitaQuantidade(size(relatorio.getBancoSemReceita()))
                .receitaSemBancoQuantidade(size(relatorio.getReceitaSemBanco()))
                .receitasAmbiguasQuantidade(size(relatorio.getReceitasAmbiguas()))
                .status("PROCESSADO")
                .mensagemErro(null)
                .processadoEm(LocalDateTime.now())
                .build());
    }

    private void salvarProcessamentoErro(
            String tenantId,
            String username,
            Long contaCorrenteId,
            Integer toleranciaDias,
            BigDecimal toleranciaValor,
            BigDecimal toleranciaValorMinimo,
            MultipartFile file,
            RuntimeException exception
    ) {
        conciliacaoOfxProcessamentoRepository.save(ConciliacaoOfxProcessamento.builder()
                .tenantId(tenantId != null ? tenantId : "desconhecido")
                .username(username)
                .contaCorrenteId(contaCorrenteId != null ? contaCorrenteId : 0L)
                .nomeArquivo(obterNomeArquivo(file))
                .contentType(file != null ? file.getContentType() : null)
                .tamanhoArquivo(file != null ? file.getSize() : 0L)
                .hashArquivo(gerarHashArquivo(file))
                .bancoIdOfx(null)
                .contaIdOfx(null)
                .periodoInicio(null)
                .periodoFim(null)
                .toleranciaDias(toleranciaDias != null ? toleranciaDias : 2)
                .toleranciaValor(toleranciaValor != null ? toleranciaValor : BigDecimal.ZERO)
                .toleranciaValorMinimo(toleranciaValorMinimo != null ? toleranciaValorMinimo : new BigDecimal("1000.00"))
                .conciliadosQuantidade(0)
                .bancoSemPagamentoQuantidade(0)
                .pagamentoSemBancoQuantidade(0)
                .ambiguosQuantidade(0)
                .receitasConciliadasQuantidade(0)
                .transferenciasConciliadasQuantidade(0)
                .creditosOrigemCartaoQuantidade(0)
                .bancoSemReceitaQuantidade(0)
                .receitaSemBancoQuantidade(0)
                .receitasAmbiguasQuantidade(0)
                .status("ERRO")
                .mensagemErro(truncarMensagem(exception.getMessage()))
                .processadoEm(LocalDateTime.now())
                .build());
    }

    private String obterUsernameAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "desconhecido";
        }
        return authentication.getName();
    }

    private String obterNomeArquivo(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            return "sem-nome";
        }
        return file.getOriginalFilename();
    }

    private String gerarHashArquivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            log.warn("conciliacao_ofx.hash_arquivo_indisponivel motivo={}", e.getMessage());
            return null;
        }
    }

    private Optional<ConciliacaoOfxProcessamento> buscarUltimoProcessamentoMesmoArquivo(String tenantId, Long contaCorrenteId, String hashArquivo) {
        if (tenantId == null || contaCorrenteId == null || hashArquivo == null || hashArquivo.isBlank()) {
            return Optional.empty();
        }
        return conciliacaoOfxProcessamentoRepository
                .findTopByTenantIdAndContaCorrenteIdAndHashArquivoOrderByProcessadoEmDesc(tenantId, contaCorrenteId, hashArquivo);
    }

    private int size(List<?> list) {
        return list != null ? list.size() : 0;
    }

    private String truncarMensagem(String mensagem) {
        if (mensagem == null || mensagem.isBlank()) {
            return "Erro desconhecido ao processar OFX";
        }
        return mensagem.length() > 1000 ? mensagem.substring(0, 1000) : mensagem;
    }

    private ResultadoDespesas conciliarDespesas(
            List<MovimentoOfxDTO> movimentos,
            List<Despesa> despesasPagas,
            int toleranciaDias,
            BigDecimal toleranciaValor,
            BigDecimal toleranciaValorMinimo,
            Set<String> ofxDebitosConsumidos,
            Set<Long> despesasConsumidas
    ) {
        List<DespesaConciliadaDTO> conciliados = new ArrayList<>();
        List<MovimentoOfxDTO> bancoSemPagamento = new ArrayList<>();
        List<DespesaAmbiguaDTO> ambiguos = new ArrayList<>();

        for (MovimentoOfxDTO movimento : movimentos) {
            if (movimento.getValor() == null || movimento.getValor().compareTo(BigDecimal.ZERO) >= 0) {
                continue;
            }
            if (ofxDebitosConsumidos.contains(movimento.getFitId())) {
                continue;
            }

            BigDecimal valorAbsoluto = movimento.getValor().abs();
            List<Despesa> candidatas = despesasPagas.stream()
                    .filter(d -> d.getId() != null && !despesasConsumidas.contains(d.getId()))
                    .filter(d -> d.getDataPagamento() != null)
                    .filter(d -> estaDentroDaToleranciaDias(movimento.getData(), d.getDataPagamento(), toleranciaDias))
                    .filter(d -> estaDentroDaToleranciaValor(valorAbsoluto, d.getValorPago(), toleranciaValor, toleranciaValorMinimo))
                    .toList();

            if (candidatas.size() == 1) {
                Despesa despesa = candidatas.get(0);
                despesasConsumidas.add(despesa.getId());
                ofxDebitosConsumidos.add(movimento.getFitId());
                conciliados.add(DespesaConciliadaDTO.builder()
                        .movimento(movimento)
                        .despesa(toDespesaDto(despesa))
                        .diferencaDias((int) Math.abs(ChronoUnit.DAYS.between(movimento.getData(), despesa.getDataPagamento())))
                        .build());
            } else if (candidatas.size() > 1) {
                ambiguos.add(DespesaAmbiguaDTO.builder()
                        .movimento(movimento)
                        .despesas(candidatas.stream().map(this::toDespesaDto).toList())
                        .build());
            } else {
                bancoSemPagamento.add(movimento);
            }
        }

        return new ResultadoDespesas(conciliados, bancoSemPagamento, ambiguos);
    }

    private ResultadoReceitas conciliarReceitas(
            List<MovimentoOfxDTO> movimentos,
            List<Receita> receitas,
            int toleranciaDias,
            BigDecimal toleranciaValor,
            BigDecimal toleranciaValorMinimo,
            Set<String> ofxCreditosConsumidos,
            Set<Long> receitasConsumidas
    ) {
        List<ReceitaConciliadaDTO> conciliadas = new ArrayList<>();
        List<MovimentoOfxDTO> bancoSemReceita = new ArrayList<>();
        List<ReceitaAmbiguaDTO> ambiguas = new ArrayList<>();

        for (MovimentoOfxDTO movimento : movimentos) {
            if (movimento.getValor() == null || movimento.getValor().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (ofxCreditosConsumidos.contains(movimento.getFitId())) {
                continue;
            }

            List<Receita> candidatas = receitas.stream()
                    .filter(r -> r.getId() != null && !receitasConsumidas.contains(r.getId()))
                    .filter(r -> r.getDataRecebimento() != null)
                    .filter(r -> estaDentroDaToleranciaDias(movimento.getData(), r.getDataRecebimento(), toleranciaDias))
                    .filter(r -> estaDentroDaToleranciaValor(movimento.getValor(), r.getValor(), toleranciaValor, toleranciaValorMinimo))
                    .toList();

            if (candidatas.size() == 1) {
                Receita receita = candidatas.get(0);
                receitasConsumidas.add(receita.getId());
                ofxCreditosConsumidos.add(movimento.getFitId());
                conciliadas.add(ReceitaConciliadaDTO.builder()
                        .movimento(movimento)
                        .receita(toReceitaDto(receita))
                        .diferencaDias((int) Math.abs(ChronoUnit.DAYS.between(movimento.getData(), receita.getDataRecebimento())))
                        .build());
            } else if (candidatas.size() > 1) {
                ambiguas.add(ReceitaAmbiguaDTO.builder()
                        .movimento(movimento)
                        .receitas(candidatas.stream().map(this::toReceitaDto).toList())
                        .build());
            } else {
                bancoSemReceita.add(movimento);
            }
        }

        return new ResultadoReceitas(conciliadas, bancoSemReceita, ambiguas);
    }

    private List<TransferenciaConciliadaDTO> conciliarTransferencias(
            List<MovimentoOfxDTO> movimentos,
            List<Movimentacao> movimentacoes,
            int toleranciaDias,
            BigDecimal toleranciaValor,
            BigDecimal toleranciaValorMinimo,
            Set<String> ofxCreditosConsumidos,
            Set<String> ofxDebitosConsumidos,
            Set<Long> movimentacoesTransferenciaConsumidas
    ) {
        List<Movimentacao> candidatasTransferencia = movimentacoes.stream()
                .filter(movimentacao -> movimentacao.getTransferenciaId() != null)
                .toList();

        List<TransferenciaConciliadaDTO> conciliadas = new ArrayList<>();
        for (MovimentoOfxDTO movimento : movimentos) {
            if (movimento.getValor() == null || movimento.getValor().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if (movimento.getValor().compareTo(BigDecimal.ZERO) > 0 && ofxCreditosConsumidos.contains(movimento.getFitId())) {
                continue;
            }
            if (movimento.getValor().compareTo(BigDecimal.ZERO) < 0 && ofxDebitosConsumidos.contains(movimento.getFitId())) {
                continue;
            }

            Movimentacao match = candidatasTransferencia.stream()
                    .filter(m -> m.getId() != null)
                    .filter(m -> !movimentacoesTransferenciaConsumidas.contains(m.getId()))
                    .filter(m -> m.getDataRecebimento() != null)
                    .filter(m -> mesmoSentidoTransferencia(movimento.getValor(), m.getTipo()))
                    .filter(m -> estaDentroDaToleranciaDias(movimento.getData(), m.getDataRecebimento(), toleranciaDias))
                    .filter(m -> estaDentroDaToleranciaValor(movimento.getValor(), m.getValor(), toleranciaValor, toleranciaValorMinimo))
                    .sorted(Comparator.comparing(m -> distanciaDias(movimento.getData(), m.getDataRecebimento())))
                    .findFirst()
                    .orElse(null);

            if (match != null) {
                movimentacoesTransferenciaConsumidas.add(match.getId());
                consumirMovimentoOfx(movimento, ofxCreditosConsumidos, ofxDebitosConsumidos);
                conciliadas.add(TransferenciaConciliadaDTO.builder()
                        .movimentoBanco(movimento)
                        .movimentacaoId(match.getId())
                        .transferenciaId(match.getTransferenciaId())
                        .build());
            }
        }
        return conciliadas;
    }

    private List<CreditoOrigemCartaoDTO> conciliarCreditosOrigemCartao(
            List<MovimentoOfxDTO> movimentos,
            Set<String> ofxDebitosConsumidos,
            Set<String> ofxCreditosConsumidos
    ) {
        List<CreditoOrigemCartaoDTO> resultado = new ArrayList<>();
        List<MovimentoOfxDTO> creditos = movimentos.stream()
                .filter(m -> m.getValor() != null && m.getValor().compareTo(BigDecimal.ZERO) > 0)
                .filter(m -> memoOrigemCartao(m.getMemo()))
                .toList();

        for (MovimentoOfxDTO credito : creditos) {
            MovimentoOfxDTO debitoRelacionado = movimentos.stream()
                    .filter(m -> m.getValor() != null && m.getValor().compareTo(BigDecimal.ZERO) < 0)
                    .filter(m -> !ofxDebitosConsumidos.contains(m.getFitId()))
                    .filter(m -> memoPixCartao(m.getMemo()))
                    .filter(m -> credito.getData() != null && credito.getData().equals(m.getData()))
                    .filter(m -> credito.getValor().abs().compareTo(m.getValor().abs()) == 0)
                    .findFirst()
                    .orElse(null);

            ofxCreditosConsumidos.add(credito.getFitId());
            if (debitoRelacionado != null) {
                ofxDebitosConsumidos.add(debitoRelacionado.getFitId());
            }

            resultado.add(CreditoOrigemCartaoDTO.builder()
                    .creditoOrigemCartao(credito)
                    .debitoPixRelacionado(debitoRelacionado)
                    .build());
        }

        return resultado;
    }

    private boolean estaDentroDaToleranciaDias(LocalDate data1, LocalDate data2, int toleranciaDias) {
        if (data1 == null || data2 == null) {
            return false;
        }
        return Math.abs(ChronoUnit.DAYS.between(data1, data2)) <= toleranciaDias;
    }

    private boolean estaDentroDaToleranciaValor(
            BigDecimal valor1,
            BigDecimal valor2,
            BigDecimal toleranciaValor,
            BigDecimal toleranciaValorMinimo
    ) {
        if (valor1 == null || valor2 == null) {
            return false;
        }
        BigDecimal v1 = valor1.abs().setScale(2, RoundingMode.HALF_UP);
        BigDecimal v2 = valor2.abs().setScale(2, RoundingMode.HALF_UP);
        BigDecimal diferenca = v1.subtract(v2).abs();
        BigDecimal base = v1.max(v2);
        if (base.compareTo(toleranciaValorMinimo) >= 0) {
            return diferenca.compareTo(toleranciaValor) <= 0;
        }
        return diferenca.compareTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)) == 0;
    }

    private boolean memoOrigemCartao(String memo) {
        String texto = normalize(memo);
        return texto.contains("ORIGEM CARTAO");
    }

    private boolean memoPixCartao(String memo) {
        String texto = normalize(memo);
        return texto.contains("PIX CARTAO") || texto.contains("CARTAO");
    }

    private boolean memoPareceTransferencia(String memo) {
        String texto = normalize(memo);
        return texto.contains("PIX TRANSF")
                || texto.contains(" TED ")
                || texto.startsWith("TED ")
                || texto.contains("TRANSFER")
                || texto.startsWith("PIX ENVIADO")
                || texto.startsWith("PIX RECEBIDO");
    }

    private boolean mesmoSentidoTransferencia(BigDecimal valorMovimentoBanco, TipoMovimentacao tipoMovimentacao) {
        if (valorMovimentoBanco == null || tipoMovimentacao == null) {
            return false;
        }
        return (valorMovimentoBanco.compareTo(BigDecimal.ZERO) > 0 && tipoMovimentacao == TipoMovimentacao.ENTRADA)
                || (valorMovimentoBanco.compareTo(BigDecimal.ZERO) < 0 && tipoMovimentacao == TipoMovimentacao.SAIDA);
    }

    private int distanciaDias(LocalDate data1, LocalDate data2) {
        if (data1 == null || data2 == null) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.abs(ChronoUnit.DAYS.between(data1, data2));
    }

    private void consumirMovimentoOfx(MovimentoOfxDTO movimento, Set<String> ofxCreditosConsumidos, Set<String> ofxDebitosConsumidos) {
        if (movimento.getValor() == null) {
            return;
        }
        if (movimento.getValor().compareTo(BigDecimal.ZERO) > 0) {
            ofxCreditosConsumidos.add(movimento.getFitId());
        } else if (movimento.getValor().compareTo(BigDecimal.ZERO) < 0) {
            ofxDebitosConsumidos.add(movimento.getFitId());
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private DespesaConciliacaoDTO toDespesaDto(Despesa despesa) {
        return DespesaConciliacaoDTO.builder()
                .id(despesa.getId())
                .nome(despesa.getNome())
                .dataPagamento(despesa.getDataPagamento())
                .dataVencimento(despesa.getDataVencimento())
                .valorPago(despesa.getValorPago())
                .valorPrevisto(despesa.getValorPrevisto())
                .build();
    }

    private ReceitaConciliacaoDTO toReceitaDto(Receita receita) {
        return ReceitaConciliacaoDTO.builder()
                .id(receita.getId())
                .descricao(receita.getDescricao())
                .dataRecebimento(receita.getDataRecebimento())
                .valor(receita.getValor())
                .build();
    }

    private OfxExtrato parseOfx(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), Charset.forName("windows-1252")))) {
            String bankId = null;
            String accountId = null;
            LocalDate periodoInicio = null;
            LocalDate periodoFim = null;
            List<MovimentoOfxDTO> movimentos = new ArrayList<>();

            MovimentoBuilder current = null;
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if ("<STMTTRN>".equalsIgnoreCase(trimmed)) {
                    current = new MovimentoBuilder();
                    continue;
                }
                if ("</STMTTRN>".equalsIgnoreCase(trimmed)) {
                    if (current != null) {
                        movimentos.add(current.build());
                    }
                    current = null;
                    continue;
                }

                if (current != null) {
                    current.accept(trimmed);
                    continue;
                }

                if (trimmed.startsWith("<BANKID>")) {
                    bankId = extractValue(trimmed);
                } else if (trimmed.startsWith("<ACCTID>")) {
                    accountId = extractValue(trimmed);
                } else if (trimmed.startsWith("<DTSTART>")) {
                    periodoInicio = parseOfxDate(extractValue(trimmed));
                } else if (trimmed.startsWith("<DTEND>")) {
                    periodoFim = parseOfxDate(extractValue(trimmed));
                }
            }

            if (periodoInicio == null || periodoFim == null) {
                throw new IllegalArgumentException("Nao foi possivel identificar o periodo do OFX");
            }

            return new OfxExtrato(bankId, accountId, periodoInicio, periodoFim, movimentos);
        } catch (IOException e) {
            throw new IllegalArgumentException("Erro ao ler arquivo OFX", e);
        }
    }

    private String extractValue(String line) {
        int idx = line.indexOf('>');
        if (idx < 0) {
            return "";
        }
        String value = line.substring(idx + 1).trim();
        int closeIdx = value.indexOf("</");
        if (closeIdx >= 0) {
            value = value.substring(0, closeIdx).trim();
        }
        return value;
    }

    private LocalDate parseOfxDate(String raw) {
        if (raw == null || raw.length() < 8) {
            return null;
        }
        String base = raw.substring(0, 8);
        return LocalDate.of(
                Integer.parseInt(base.substring(0, 4)),
                Integer.parseInt(base.substring(4, 6)),
                Integer.parseInt(base.substring(6, 8))
        );
    }

    private record OfxExtrato(
            String bankId,
            String accountId,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            List<MovimentoOfxDTO> movimentos
    ) {
    }

    private record ResultadoDespesas(
            List<DespesaConciliadaDTO> conciliados,
            List<MovimentoOfxDTO> bancoSemPagamento,
            List<DespesaAmbiguaDTO> ambiguos
    ) {
    }

    private record ResultadoReceitas(
            List<ReceitaConciliadaDTO> conciliadas,
            List<MovimentoOfxDTO> bancoSemReceita,
            List<ReceitaAmbiguaDTO> ambiguas
    ) {
    }

    private static class MovimentoBuilder {
        private LocalDate data;
        private BigDecimal valor;
        private String memo;
        private String fitId;

        void accept(String line) {
            if (line.startsWith("<DTPOSTED>")) {
                this.data = parseStaticDate(extractTaggedValue(line, "DTPOSTED"));
            } else if (line.startsWith("<TRNAMT>")) {
                this.valor = new BigDecimal(extractTaggedValue(line, "TRNAMT"));
            } else if (line.startsWith("<FITID>")) {
                this.fitId = extractTaggedValue(line, "FITID");
            } else if (line.startsWith("<MEMO>")) {
                this.memo = extractTaggedValue(line, "MEMO");
            }
        }

        MovimentoOfxDTO build() {
            return MovimentoOfxDTO.builder()
                    .data(data)
                    .valor(valor)
                    .memo(memo)
                    .fitId(fitId)
                    .build();
        }

        private static LocalDate parseStaticDate(String raw) {
            if (raw == null || raw.length() < 8) {
                return null;
            }
            String base = raw.substring(0, 8);
            return LocalDate.of(
                    Integer.parseInt(base.substring(0, 4)),
                    Integer.parseInt(base.substring(4, 6)),
                    Integer.parseInt(base.substring(6, 8))
            );
        }

        private static String extractTaggedValue(String line, String tagName) {
            String openTag = "<" + tagName + ">";
            if (!line.startsWith(openTag)) {
                return "";
            }
            String value = line.substring(openTag.length()).trim();
            int closeIdx = value.indexOf("</");
            if (closeIdx >= 0) {
                value = value.substring(0, closeIdx).trim();
            }
            return value;
        }
    }
}
