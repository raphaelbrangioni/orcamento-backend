package com.example.orcamento.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Entity
@Data
@ToString
public class CategoriaDespesa {
    @Id
    private Long id;

    private String nome;

    private String tenantId;

    @ToString.Exclude
    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL)
    // @JsonIgnore removido para permitir fetch e serialização
    private List<SubcategoriaDespesa> subcategorias;

}
