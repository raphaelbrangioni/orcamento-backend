package com.example.orcamento.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;

@Entity
@Table(name = "conta_corrente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContaCorrente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String agencia;

    @Column(name = "numero_conta", nullable = false)
    private String numeroConta;

    @Column(nullable = false)
    private String banco;

    @Column(name = "nome_banco", nullable = false)
    private String nomeBanco;

    @Column(nullable = false)
    private BigDecimal saldo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    // Métodos para atualizar saldo (mantidos explicitamente)
    public void adicionarValor(BigDecimal valor) {
        this.saldo = this.saldo.add(valor);
    }

    public void subtrairValor(BigDecimal valor) {
        this.saldo = this.saldo.subtract(valor);
    }
}