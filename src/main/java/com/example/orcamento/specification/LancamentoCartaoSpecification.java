// src/main/java/com/example/orcamento/specification/LancamentoCartaoSpecification.java
package com.example.orcamento.specification;

import com.example.orcamento.model.LancamentoCartao;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LancamentoCartaoSpecification {

    public static Specification<LancamentoCartao> comFiltros(Map<String, Object> filtros) {
        return (Root<LancamentoCartao> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Sempre filtrar pelo tenantId
            String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
            predicates.add(builder.equal(root.get("tenantId"), tenantId));

            if (filtros.containsKey("id") && filtros.get("id") != null) {
                predicates.add(builder.equal(root.get("id"), filtros.get("id")));
            }
            if (filtros.containsKey("descricao") && filtros.get("descricao") != null) {
                predicates.add(builder.like(
                        builder.lower(root.get("descricao")),
                        "%" + filtros.get("descricao").toString().toLowerCase() + "%"
                ));
            }
            if (filtros.containsKey("valorTotal") && filtros.get("valorTotal") != null) {
                predicates.add(builder.equal(root.get("valorTotal"), new BigDecimal(filtros.get("valorTotal").toString())));
            }
            if (filtros.containsKey("detalhes") && filtros.get("detalhes") != null) {
                predicates.add(builder.like(
                        builder.lower(root.get("detalhes")),
                        "%" + filtros.get("detalhes").toString().toLowerCase() + "%"
                ));
            }
            if (filtros.containsKey("tipoDespesaId") && filtros.get("tipoDespesaId") != null) {
                predicates.add(builder.equal(root.get("tipoDespesa").get("id"), filtros.get("tipoDespesaId")));
            }
            if (filtros.containsKey("classificacao") && filtros.get("classificacao") != null) {
                predicates.add(builder.equal(root.get("classificacao"), filtros.get("classificacao").toString()));
            }
            if (filtros.containsKey("variabilidade") && filtros.get("variabilidade") != null) {
                predicates.add(builder.equal(root.get("variabilidade"), filtros.get("variabilidade").toString()));
            }
            if (filtros.containsKey("parcelaAtual") && filtros.get("parcelaAtual") != null) {
                predicates.add(builder.equal(root.get("parcelaAtual"), filtros.get("parcelaAtual")));
            }
            if (filtros.containsKey("totalParcelas") && filtros.get("totalParcelas") != null) {
                predicates.add(builder.equal(root.get("totalParcelas"), filtros.get("totalParcelas")));
            }
            if (filtros.containsKey("mesAnoFaturaList") && filtros.get("mesAnoFaturaList") != null) {
                List<String> mesAnoList = (List<String>) filtros.get("mesAnoFaturaList");
                if (!mesAnoList.isEmpty()) {
                    predicates.add(root.get("mesAnoFatura").in(mesAnoList));
                }
            }
            if (filtros.containsKey("cartaoCreditoId") && filtros.get("cartaoCreditoId") != null) {
                predicates.add(builder.equal(root.get("cartaoCredito").get("id"), filtros.get("cartaoCreditoId")));
            }
            if (filtros.containsKey("proprietario") && filtros.get("proprietario") != null) {
                predicates.add(builder.equal(root.get("proprietario"), filtros.get("proprietario")));
            }
            if (filtros.containsKey("dataCompra") && filtros.get("dataCompra") != null) {
                predicates.add(builder.equal(root.get("dataCompra"), filtros.get("dataCompra")));
            }
            if (filtros.containsKey("dataRegistro") && filtros.get("dataRegistro") != null) {
                predicates.add(builder.equal(root.get("dataRegistro"), filtros.get("dataRegistro")));
            }
            if (filtros.containsKey("compraId") && filtros.get("compraId") != null) {
                predicates.add(builder.equal(root.get("compra").get("id"), filtros.get("compraId")));
            }
            if (filtros.containsKey("pagoPorTerceiro") && filtros.get("pagoPorTerceiro") != null) {
                predicates.add(builder.equal(root.get("pagoPorTerceiro"), filtros.get("pagoPorTerceiro")));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}