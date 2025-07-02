// src/main/java/com/example/orcamento/specification/DespesaSpecification.java
package com.example.orcamento.specification;

import com.example.orcamento.model.Despesa;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DespesaSpecification {

    public static Specification<Despesa> comFiltros(String tenantId, Map<String, Object> filtros) {
        return (Root<Despesa> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Sempre filtrar pelo tenantId
            predicates.add(builder.equal(root.get("tenantId"), tenantId));

            if (filtros.containsKey("id") && filtros.get("id") != null) {
                predicates.add(builder.equal(root.get("id"), filtros.get("id")));
            }
            if (filtros.containsKey("nome") && filtros.get("nome") != null) {
                predicates.add(builder.like(
                        builder.lower(root.get("nome")),
                        "%" + filtros.get("nome").toString().toLowerCase() + "%"
                ));
            }
            if (filtros.containsKey("valorPrevisto") && filtros.get("valorPrevisto") != null) {
                predicates.add(builder.equal(root.get("valorPrevisto"), new BigDecimal(filtros.get("valorPrevisto").toString())));
            }
            if (filtros.containsKey("detalhes") && filtros.get("detalhes") != null) {
                predicates.add(builder.like(
                        builder.lower(root.get("detalhes")),
                        "%" + filtros.get("detalhes").toString().toLowerCase() + "%"
                ));
            }
            if (filtros.containsKey("tipoDespesaId") && filtros.get("tipoDespesaId") != null) {
                predicates.add(builder.equal(root.get("tipo").get("id"), filtros.get("tipoDespesaId")));
            }
            if (filtros.containsKey("classificacao") && filtros.get("classificacao") != null) {
                predicates.add(builder.equal(root.get("classificacao"), filtros.get("classificacao").toString()));
            }
            if (filtros.containsKey("variabilidade") && filtros.get("variabilidade") != null) {
                predicates.add(builder.equal(root.get("variabilidade"), filtros.get("variabilidade").toString()));
            }
            if (filtros.containsKey("parcela") && filtros.get("parcela") != null) {
                predicates.add(builder.equal(root.get("parcela"), filtros.get("parcela")));
            }
            if (filtros.containsKey("dataVencimentoInicio") && filtros.get("dataVencimentoInicio") != null) {
                predicates.add(builder.greaterThanOrEqualTo(
                        root.get("dataVencimento"),
                        LocalDate.parse(filtros.get("dataVencimentoInicio").toString())
                ));
            }
            if (filtros.containsKey("dataVencimentoFim") && filtros.get("dataVencimentoFim") != null) {
                predicates.add(builder.lessThanOrEqualTo(
                        root.get("dataVencimento"),
                        LocalDate.parse(filtros.get("dataVencimentoFim").toString())
                ));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}