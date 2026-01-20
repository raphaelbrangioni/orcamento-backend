package com.example.orcamento.mapper;

import com.example.orcamento.dto.CompraDTO;
import com.example.orcamento.dto.CompraTerceiroDTO;
import com.example.orcamento.model.Compra;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CompraMapper {

    public CompraDTO toDto(Compra compra) {
        if (compra == null) {
            return null;
        }

        CompraDTO dto = new CompraDTO();
        dto.setId(compra.getId());
        dto.setDescricao(compra.getDescricao());
        dto.setValorTotal(compra.getValorTotal());
        dto.setNumeroParcelas(compra.getNumeroParcelas());
        dto.setDataCompra(compra.getDataCompra());
        dto.setCartaoCreditoId(compra.getCartaoCredito() != null ? compra.getCartaoCredito().getId() : null);
        dto.setSubcategoriaId(compra.getSubcategoria() != null ? compra.getSubcategoria().getId() : null);
        dto.setProprietario(compra.getProprietario());
        dto.setDetalhes(compra.getDetalhes());
        dto.setClassificacao(compra.getClassificacao() != null ? compra.getClassificacao().name() : null);
        dto.setVariabilidade(compra.getVariabilidade() != null ? compra.getVariabilidade().name() : null);

        if (compra.getTerceiros() != null) {
            dto.setTerceiros(compra.getTerceiros().stream().map(terceiro -> {
                CompraTerceiroDTO terceiroDTO = new CompraTerceiroDTO();
                terceiroDTO.setPessoaId(terceiro.getPessoa().getId());
                terceiroDTO.setPercentual(terceiro.getPercentual());
                return terceiroDTO;
            }).collect(Collectors.toList()));
        }

        return dto;
    }
}
