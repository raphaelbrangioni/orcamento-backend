package com.example.orcamento.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CompraDTO {
    private Long id;
    private String descricao;
    private BigDecimal valorTotal;
    private Integer numeroParcelas;
    private LocalDate dataCompra;
    private Long cartaoCreditoId;
    private Long subcategoriaId;
    private String proprietario;
    private String detalhes;
    private String classificacao;
    private String variabilidade;
    private List<CompraTerceiroDTO> terceiros;
    
    // Objetos aninhados para compatibilidade com o frontend
    @JsonIgnoreProperties(ignoreUnknown = true)
    private CartaoCreditoDTO cartaoCredito;
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    private SubcategoriaDTO subcategoria;
    
    // Métodos auxiliares para extrair IDs dos objetos aninhados
    public Long getCartaoCreditoId() {
        if (cartaoCreditoId != null) {
            return cartaoCreditoId;
        }
        if (cartaoCredito != null) {
            return cartaoCredito.getId();
        }
        return null;
    }
    
    public Long getSubcategoriaId() {
        if (subcategoriaId != null) {
            return subcategoriaId;
        }
        if (subcategoria != null) {
            return subcategoria.getId();
        }
        return null;
    }
    
    // DTOs aninhados simples
    @Data
    public static class CartaoCreditoDTO {
        private Long id;
    }
    
    @Data
    public static class SubcategoriaDTO {
        private Long id;
    }
}

