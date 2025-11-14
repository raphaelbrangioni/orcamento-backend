package com.example.orcamento.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipos_despesa")
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoDespesa {

    @Id
    // Removido @GeneratedValue para permitir atribuição manual do id
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false)
    private String tenantId;

    // Novo campo para compatibilidade retroativa com subcategoria
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subcategoria_id")
    private SubcategoriaDespesa subcategoria;


    // Métodos de conveniência para acessar a categoria e ícone
    public CategoriaDespesa getCategoria() {
        return subcategoria != null ? subcategoria.getCategoria() : null;
    }

}
