package com.example.orcamento.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

@Entity
@Data
@ToString
public class SubcategoriaDespesa {
    @Id
    // Removido @GeneratedValue para controle manual do id
    private Long id;

    private String nome;

    private String tenantId;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "categoria_id")
    @JsonIgnore // Evita recursividade infinita na serialização
    private CategoriaDespesa categoria;

    @OneToMany(mappedBy = "subcategoria")
    @JsonIgnore
    private List<TipoDespesa> tiposDespesa;
}
