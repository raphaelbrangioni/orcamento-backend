package com.example.orcamento.service;

import com.example.orcamento.model.Movimentacao;
import com.example.orcamento.model.Receita;
import com.example.orcamento.model.TipoMovimentacao;
import com.example.orcamento.repository.MovimentacaoRepository;
import com.example.orcamento.repository.ReceitaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceitaService {

    private final ReceitaRepository receitaRepository;
    private final MovimentacaoService movimentacaoService;
    private final MovimentacaoRepository movimentacaoRepository;
    private final ContaCorrenteService contaCorrenteService;

    public List<Receita> listarReceitas() {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return receitaRepository.findByTenantId(tenantId);
    }

    @Transactional
    public Receita salvarReceita(Receita receita) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        receita.setTenantId(tenantId);
        if (receita.getContaCorrente() == null || receita.getContaCorrente().getId() == null) {
            throw new IllegalArgumentException("Conta corrente e obrigatoria para salvar a receita");
        }
        if (receita.getValor() == null || receita.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da receita deve ser maior que zero");
        }

        log.info("Receita recebida do frontend: {}", receita);
        log.info("Valor de isPrevista recebido: {}", receita.isPrevista());

        Receita receitaSalva = receitaRepository.save(receita);

        log.info("Receita salva no banco: {}", receitaSalva);
        log.info("Valor de isPrevista apos salvar: {}", receitaSalva.isPrevista());

        if (!receitaSalva.isPrevista()) {
            Movimentacao movimentacao = Movimentacao.builder()
                    .tipo(TipoMovimentacao.ENTRADA)
                    .valor(receita.getValor())
                    .contaCorrente(receita.getContaCorrente())
                    .receita(receitaSalva)
                    .descricao("Recebimento de " + receita.getDescricao())
                    .dataRecebimento(receita.getDataRecebimento())
                    .dataCadastro(LocalDateTime.now())
                    .tenantId(tenantId)
                    .build();
            movimentacaoService.registrarMovimentacao(movimentacao);
        }

        log.info(
                "receita.criada receitaId={} tenantId={} contaCorrenteId={} valor={} prevista={}",
                receitaSalva.getId(),
                tenantId,
                receitaSalva.getContaCorrente() != null ? receitaSalva.getContaCorrente().getId() : null,
                receitaSalva.getValor(),
                receitaSalva.isPrevista()
        );

        return receitaSalva;
    }

    @Transactional
    public void excluirReceita(Long id) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        Receita receita = buscarReceitaPorId(id, tenantId);

        log.info("Excluindo a receita: {}", receita);

        if (!receita.isPrevista()) {
            List<Movimentacao> movimentacoes = movimentacaoRepository.findByReceita(receita);
            if (!movimentacoes.isEmpty()) {
                movimentacaoRepository.deleteAll(movimentacoes);
            }

            Movimentacao movimentacao = Movimentacao.builder()
                    .tipo(TipoMovimentacao.SAIDA)
                    .valor(receita.getValor())
                    .contaCorrente(receita.getContaCorrente())
                    .descricao("Cancelamento de receita: " + receita.getDescricao())
                    .dataRecebimento(receita.getDataRecebimento())
                    .dataCadastro(LocalDateTime.now())
                    .tenantId(tenantId)
                    .build();
            movimentacaoService.registrarMovimentacao(movimentacao);
        }

        receitaRepository.deleteByIdAndTenantId(id, tenantId);
        log.info("receita.excluida receitaId={} tenantId={}", id, tenantId);
    }

    @Transactional
    public Receita atualizarReceita(Long id, Receita receitaAtualizada) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        Receita receita = buscarReceitaPorId(id, tenantId);

        boolean precisaRefazerMovimentacao = receita.isPrevista() != receitaAtualizada.isPrevista() ||
                (receita.getValor() != null && !receita.getValor().equals(receitaAtualizada.getValor())) ||
                (receita.getValor() == null && receitaAtualizada.getValor() != null) ||
                (receita.getContaCorrente() != null && receita.getContaCorrente().getId() != null
                        && receitaAtualizada.getContaCorrente() != null && receitaAtualizada.getContaCorrente().getId() != null
                        && !receita.getContaCorrente().getId().equals(receitaAtualizada.getContaCorrente().getId())) ||
                !Objects.equals(receita.getDataRecebimento(), receitaAtualizada.getDataRecebimento());

        receita.setDescricao(receitaAtualizada.getDescricao());
        receita.setValor(receitaAtualizada.getValor());
        receita.setDataRecebimento(receitaAtualizada.getDataRecebimento());
        receita.setTipo(receitaAtualizada.getTipo());
        receita.setContaCorrente(receitaAtualizada.getContaCorrente());
        receita.setPrevista(receitaAtualizada.isPrevista());

        Receita receitaSalva = receitaRepository.save(receita);

        if (precisaRefazerMovimentacao) {
            List<Movimentacao> movimentacoesExistentes = movimentacaoRepository.findByReceita(receitaSalva);

            if (movimentacoesExistentes != null && !movimentacoesExistentes.isEmpty()) {
                LocalDateTime agora = LocalDateTime.now();

                for (Movimentacao m : movimentacoesExistentes) {
                    if (m.getContaCorrente() == null || m.getContaCorrente().getId() == null || m.getValor() == null || m.getTipo() == null) {
                        continue;
                    }

                    boolean desfazerComoEntrada = m.getTipo() == TipoMovimentacao.SAIDA;
                    contaCorrenteService.atualizarSaldo(m.getContaCorrente().getId(), m.getValor(), desfazerComoEntrada, agora);
                }

                movimentacaoRepository.deleteAll(movimentacoesExistentes);
            }

            if (!receitaSalva.isPrevista()) {
                String tenantAtual = com.example.orcamento.security.TenantContext.getTenantId();
                Movimentacao movimentacaoEntrada = Movimentacao.builder()
                        .tipo(TipoMovimentacao.ENTRADA)
                        .valor(receitaSalva.getValor())
                        .contaCorrente(receitaSalva.getContaCorrente())
                        .receita(receitaSalva)
                        .descricao("Recebimento de " + receitaSalva.getDescricao())
                        .dataRecebimento(receitaSalva.getDataRecebimento())
                        .dataCadastro(LocalDateTime.now())
                        .tenantId(tenantAtual)
                        .build();
                movimentacaoService.registrarMovimentacao(movimentacaoEntrada);
            }
        }

        log.info(
                "receita.atualizada receitaId={} tenantId={} contaCorrenteId={} valor={} prevista={} refezMovimentacao={}",
                receitaSalva.getId(),
                tenantId,
                receitaSalva.getContaCorrente() != null ? receitaSalva.getContaCorrente().getId() : null,
                receitaSalva.getValor(),
                receitaSalva.isPrevista(),
                precisaRefazerMovimentacao
        );

        return receitaSalva;
    }

    public Map<String, Map<String, BigDecimal>> listarReceitasPorMes(int ano) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<Receita> receitas = receitaRepository.findByTenantId(tenantId).stream()
                .filter(r -> r.getDataRecebimento().getYear() == ano)
                .collect(Collectors.toList());

        return receitas.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getDataRecebimento().getMonth().toString(),
                        Collectors.teeing(
                                Collectors.mapping(
                                        r -> r.isPrevista() ? r.getValor() : BigDecimal.ZERO,
                                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                                ),
                                Collectors.mapping(
                                        r -> !r.isPrevista() ? r.getValor() : BigDecimal.ZERO,
                                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                                ),
                                (previstas, efetivadas) -> Map.of("previstas", previstas, "efetivadas", efetivadas)
                        )
                ));
    }

    public Map<String, Map<String, BigDecimal>> listarReceitasPorMesETipo(int ano) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<Receita> receitas = receitaRepository.findByTenantId(tenantId).stream()
                .filter(r -> r.getDataRecebimento().getYear() == ano)
                .collect(Collectors.toList());

        return receitas.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getDataRecebimento().getMonth().toString(),
                        Collectors.groupingBy(
                                r -> r.getTipo() != null ? r.getTipo().name() : "OUTROS",
                                Collectors.mapping(
                                        Receita::getValor,
                                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                                )
                        )
                ));
    }

    @Transactional
    public Receita efetivarReceita(Long id) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        Receita receita = buscarReceitaPorId(id, tenantId);

        if (!receita.isPrevista()) {
            throw new IllegalStateException("A receita ja esta efetivada");
        }

        receita.setPrevista(false);
        Receita receitaAtualizada = receitaRepository.save(receita);

        Movimentacao movimentacao = Movimentacao.builder()
                .tipo(TipoMovimentacao.ENTRADA)
                .valor(receita.getValor())
                .contaCorrente(receita.getContaCorrente())
                .receita(receitaAtualizada)
                .descricao("Efetivacao de receita: " + receita.getDescricao())
                .dataRecebimento(receita.getDataRecebimento())
                .dataCadastro(LocalDateTime.now())
                .tenantId(tenantId)
                .build();
        movimentacaoService.registrarMovimentacao(movimentacao);

        log.info(
                "receita.efetivada receitaId={} tenantId={} contaCorrenteId={} valor={}",
                receitaAtualizada.getId(),
                tenantId,
                receitaAtualizada.getContaCorrente() != null ? receitaAtualizada.getContaCorrente().getId() : null,
                receitaAtualizada.getValor()
        );

        return receitaAtualizada;
    }

    private Receita buscarReceitaPorId(Long id, String tenantId) {
        return receitaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Receita nao encontrada com ID: " + id));
    }
}
