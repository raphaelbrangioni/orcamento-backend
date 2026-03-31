package com.example.orcamento.service;

import com.example.orcamento.dto.conciliacao.ConciliacaoOfxProcessamentoDTO;
import com.example.orcamento.model.ConciliacaoOfxProcessamento;
import com.example.orcamento.repository.ConciliacaoOfxProcessamentoRepository;
import com.example.orcamento.security.TenantContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConciliacaoOfxProcessamentoService {

    private final ConciliacaoOfxProcessamentoRepository conciliacaoOfxProcessamentoRepository;

    @Transactional(readOnly = true)
    public List<ConciliacaoOfxProcessamentoDTO> listarProcessamentos(
            Long contaCorrenteId,
            String status,
            LocalDateTime processadoDe,
            LocalDateTime processadoAte
    ) {
        String tenantId = TenantContext.getTenantId();

        Specification<ConciliacaoOfxProcessamento> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (contaCorrenteId != null) {
                predicates.add(cb.equal(root.get("contaCorrenteId"), contaCorrenteId));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status.trim().toUpperCase()));
            }
            if (processadoDe != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("processadoEm"), processadoDe));
            }
            if (processadoAte != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("processadoEm"), processadoAte));
            }

            query.orderBy(cb.desc(root.get("processadoEm")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return conciliacaoOfxProcessamentoRepository.findAll(specification).stream()
                .map(this::toDto)
                .toList();
    }

    public LocalDateTime inicioDoDia(java.time.LocalDate data) {
        return data != null ? data.atStartOfDay() : null;
    }

    public LocalDateTime fimDoDia(java.time.LocalDate data) {
        return data != null ? data.atTime(LocalTime.MAX) : null;
    }

    private ConciliacaoOfxProcessamentoDTO toDto(ConciliacaoOfxProcessamento processamento) {
        return ConciliacaoOfxProcessamentoDTO.builder()
                .id(processamento.getId())
                .username(processamento.getUsername())
                .contaCorrenteId(processamento.getContaCorrenteId())
                .nomeArquivo(processamento.getNomeArquivo())
                .tamanhoArquivo(processamento.getTamanhoArquivo())
                .bancoIdOfx(processamento.getBancoIdOfx())
                .contaIdOfx(processamento.getContaIdOfx())
                .periodoInicio(processamento.getPeriodoInicio())
                .periodoFim(processamento.getPeriodoFim())
                .toleranciaDias(processamento.getToleranciaDias())
                .toleranciaValor(processamento.getToleranciaValor())
                .toleranciaValorMinimo(processamento.getToleranciaValorMinimo())
                .conciliadosQuantidade(processamento.getConciliadosQuantidade())
                .bancoSemPagamentoQuantidade(processamento.getBancoSemPagamentoQuantidade())
                .pagamentoSemBancoQuantidade(processamento.getPagamentoSemBancoQuantidade())
                .ambiguosQuantidade(processamento.getAmbiguosQuantidade())
                .receitasConciliadasQuantidade(processamento.getReceitasConciliadasQuantidade())
                .transferenciasConciliadasQuantidade(processamento.getTransferenciasConciliadasQuantidade())
                .creditosOrigemCartaoQuantidade(processamento.getCreditosOrigemCartaoQuantidade())
                .bancoSemReceitaQuantidade(processamento.getBancoSemReceitaQuantidade())
                .receitaSemBancoQuantidade(processamento.getReceitaSemBancoQuantidade())
                .receitasAmbiguasQuantidade(processamento.getReceitasAmbiguasQuantidade())
                .status(processamento.getStatus())
                .mensagemErro(processamento.getMensagemErro())
                .processadoEm(processamento.getProcessadoEm())
                .build();
    }
}
