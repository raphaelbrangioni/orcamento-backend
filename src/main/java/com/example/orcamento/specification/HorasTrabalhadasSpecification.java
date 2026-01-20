package com.example.orcamento.specification;

import com.example.orcamento.model.HorasTrabalhadas;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HorasTrabalhadasSpecification {

    public static Specification<HorasTrabalhadas> comFiltros(Map<String, Object> filtros) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            filtros.forEach((chave, valor) -> {
                if (valor == null) return;

                switch (chave) {
                    case "inicio":
                        predicates.add(builder.greaterThanOrEqualTo(root.get("inicio"), (LocalDateTime) valor));
                        break;
                    case "fim":
                        predicates.add(builder.lessThanOrEqualTo(root.get("fim"), (LocalDateTime) valor));
                        break;
                    case "clienteId":
                        predicates.add(builder.equal(root.get("clienteId"), valor));
                        break;
                    case "projetoId":
                        predicates.add(builder.equal(root.get("projetoId"), valor));
                        break;
                    case "tenantId":
                        predicates.add(builder.equal(root.get("tenantId"), valor));
                        break;
                }
            });

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
