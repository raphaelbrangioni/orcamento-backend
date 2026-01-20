package com.example.orcamento.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "compras_terceiros")
@Getter
@Setter
@ToString(exclude = "compra") // Exclui para evitar referência circular
@NoArgsConstructor
@AllArgsConstructor
public class CompraTerceiro {

    @EmbeddedId
    private CompraTerceiroId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("compraId")
    @JoinColumn(name = "compra_id")
    @JsonIgnore
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("pessoaId")
    @JoinColumn(name = "pessoa_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Pessoa pessoa;

    @Column(nullable = false)
    private BigDecimal percentual;
    
    // Implementação manual de equals e hashCode para evitar referência circular
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompraTerceiro that = (CompraTerceiro) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
