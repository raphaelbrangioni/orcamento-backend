package com.example.orcamento.dto.dashboard;

import com.example.orcamento.model.Despesa;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DespesasMensaisDTO {
    private Integer mes;
    private List<Despesa> despesas;
}
