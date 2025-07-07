package com.example.orcamento.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AcessoUsuarioDTO {
    private Long id;
    private LocalDateTime dataHoraLogin;
    private LocalDateTime dataHoraLogout;
    private String ipOrigem;
    private String tenantId;
}
