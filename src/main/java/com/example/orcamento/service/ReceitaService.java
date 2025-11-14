// src/main/java/com/example/orcamento/service/ReceitaService.java
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceitaService {

    private final ReceitaRepository receitaRepository;
    private final MovimentacaoService movimentacaoService;
    private final MovimentacaoRepository movimentacaoRepository;

    public List<Receita> listarReceitas() {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        return receitaRepository.findByTenantId(tenantId);
    }

    @Transactional
    public Receita salvarReceita(Receita receita) {
        receita.setTenantId(com.example.orcamento.security.TenantContext.getTenantId());
        if (receita.getContaCorrente() == null || receita.getContaCorrente().getId() == null) {
            throw new IllegalArgumentException("Conta corrente é obrigatória para salvar a receita");
        }
        if (receita.getValor() == null || receita.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da receita deve ser maior que zero");
        }

        log.info("Receita recebida do frontend: {}", receita);
        log.info("Valor de isPrevista recebido: {}", receita.isPrevista());

        // Salva a receita primeiro para obter o ID
        Receita receitaSalva = receitaRepository.save(receita);

        log.info("Receita salva no banco: {}", receitaSalva);
        log.info("Valor de isPrevista após salvar: {}", receitaSalva.isPrevista());

        // Registra a movimentação apenas se não for prevista
        if (!receitaSalva.isPrevista()) {
            String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
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

        return receitaSalva;
    }

    @Transactional
    public void excluirReceita(Long id) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        Receita receita = receitaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Receita não encontrada com ID: " + id));
        if (!tenantId.equals(receita.getTenantId())) {
            throw new SecurityException("Acesso negado à receita de outro tenant.");
        }

        log.info("Excluindo a receita: {}", receita);

        // Remove ou desassocia as movimentações existentes apenas se não for prevista
        if (!receita.isPrevista()) {
            List<Movimentacao> movimentacoes = movimentacaoRepository.findByReceita(receita);
            if (!movimentacoes.isEmpty()) {
                movimentacaoRepository.deleteAll(movimentacoes); // Remove as movimentações existentes
            }

            // Registra a movimentação de saída (correção)
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

        // Exclui a receita
        receitaRepository.deleteById(id);
    }

    @Transactional
    public Receita atualizarReceita(Long id, Receita receitaAtualizada) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        Receita receita = receitaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Receita não encontrada com ID: " + id));
        if (!tenantId.equals(receita.getTenantId())) {
            throw new SecurityException("Acesso negado à receita de outro tenant.");
        }

        // Se o status "isPrevista" mudou ou valores afetam o saldo
        if (receita.isPrevista() != receitaAtualizada.isPrevista() ||
                !receita.getValor().equals(receitaAtualizada.getValor()) ||
                !receita.getContaCorrente().getId().equals(receitaAtualizada.getContaCorrente().getId())) {

            if (!receita.isPrevista()) {
                // Remove o valor antigo se era efetivada
                Movimentacao movimentacaoSaida = Movimentacao.builder()
                        .tipo(TipoMovimentacao.SAIDA)
                        .valor(receita.getValor())
                        .contaCorrente(receita.getContaCorrente())
                        .receita(receita)
                        .descricao("Correção de receita (saída): " + receita.getDescricao())
                        .dataRecebimento(receita.getDataRecebimento())
                        .dataCadastro(LocalDateTime.now())
                        .tenantId(tenantId)
                        .build();
                movimentacaoService.registrarMovimentacao(movimentacaoSaida);
            }

            if (!receitaAtualizada.isPrevista()) {
                // Adiciona o novo valor se agora é efetivada
                Movimentacao movimentacaoEntrada = Movimentacao.builder()
                        .tipo(TipoMovimentacao.ENTRADA)
                        .valor(receitaAtualizada.getValor())
                        .contaCorrente(receitaAtualizada.getContaCorrente())
                        .receita(receita)
                        .descricao("Correção de receita (entrada): " + receitaAtualizada.getDescricao())
                        .dataRecebimento(receitaAtualizada.getDataRecebimento())
                        .dataCadastro(LocalDateTime.now())
                        .tenantId(tenantId)
                        .build();
                movimentacaoService.registrarMovimentacao(movimentacaoEntrada);
            }
        }

        receita.setDescricao(receitaAtualizada.getDescricao());
        receita.setValor(receitaAtualizada.getValor());
        receita.setDataRecebimento(receitaAtualizada.getDataRecebimento());
        receita.setTipo(receitaAtualizada.getTipo());
        receita.setContaCorrente(receitaAtualizada.getContaCorrente());
        receita.setPrevista(receitaAtualizada.isPrevista());
        return receitaRepository.save(receita);
    }

    // src/main/java/com/example/orcamento/service/ReceitaService.java
    public Map<String, Map<String, BigDecimal>> listarReceitasPorMes(int ano) {
        List<Receita> receitas = receitaRepository.findAll().stream()
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
        List<Receita> receitas = receitaRepository.findAll().stream()
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
        Receita receita = receitaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Receita não encontrada com ID: " + id));

        if (!receita.isPrevista()) {
            throw new IllegalStateException("A receita já está efetivada");
        }

        receita.setPrevista(false);
        Receita receitaAtualizada = receitaRepository.save(receita);

        // Registra a movimentação ao efetivar
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        Movimentacao movimentacao = Movimentacao.builder()
                .tipo(TipoMovimentacao.ENTRADA)
                .valor(receita.getValor())
                .contaCorrente(receita.getContaCorrente())
                .receita(receitaAtualizada)
                .descricao("Efetivação de receita: " + receita.getDescricao())
                .dataRecebimento(receita.getDataRecebimento())
                .dataCadastro(LocalDateTime.now())
                .tenantId(tenantId)
                .build();
        movimentacaoService.registrarMovimentacao(movimentacao);

        return receitaAtualizada;
    }
}