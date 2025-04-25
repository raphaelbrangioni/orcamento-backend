package com.example.orcamento.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "despesas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private BigDecimal valorPrevisto; // 🔹 Valor estimado da despesa

    private BigDecimal valorPago; // 🔹 Valor real pago (pode ser null até o pagamento)

    @Column(nullable = false)
    private LocalDate dataVencimento; // 🔹 Data prevista para pagamento

    private LocalDate dataPagamento; // 🔹 Data em que a despesa foi paga (pode ser null)

    private Integer parcela; // 🔹 Número da parcela (se aplicável)

    private String detalhes; // 🔹 Descrição ou observação sobre a despesa

    @ManyToOne
    @JoinColumn(name = "tipo_despesa_id", nullable = false)
    private TipoDespesa tipo;

    // Novo campo
    @ManyToOne
    @JoinColumn(name = "conta_corrente_id")
    private ContaCorrente contaCorrente;

    @ManyToOne
    @JoinColumn(name = "meta_economia_id")
    private MetaEconomia metaEconomia;

    // Adicione este campo ao seu modelo Despesa.java existente
    private Long despesaParceladaId;
}
