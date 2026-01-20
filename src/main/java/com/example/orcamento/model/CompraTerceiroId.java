package com.example.orcamento.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompraTerceiroId implements Serializable {

    @Column(name = "compra_id")
    private Long compraId;

    @Column(name = "pessoa_id")
    private Long pessoaId;
}
