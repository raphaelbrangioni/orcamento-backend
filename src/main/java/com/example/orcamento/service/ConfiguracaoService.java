package com.example.orcamento.service;

import com.example.orcamento.dto.ConfiguracaoDTO;
import com.example.orcamento.model.Configuracao;
import com.example.orcamento.repository.ConfiguracaoRepository;
import com.example.orcamento.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracaoService {

    private final ConfiguracaoRepository configuracaoRepository;

    public ConfiguracaoDTO getConfiguracoes() {
        String tenantId = TenantContext.getTenantId();
        Configuracao configuracao = configuracaoRepository.findByTenantId(tenantId).orElse(null);

        if (configuracao == null) {
            return new ConfiguracaoDTO();
        }

        return mapToDTO(configuracao);
    }

    @Transactional
    public ConfiguracaoDTO salvarConfiguracoes(ConfiguracaoDTO configuracaoDTO) {
        String tenantId = TenantContext.getTenantId();
        Configuracao configuracao = configuracaoRepository.findByTenantId(tenantId).orElse(null);

        if (configuracao == null) {
            configuracao = new Configuracao();
            configuracao.setTenantId(tenantId);
        }

        configuracao.setTipoDespesaInvestimentoId(configuracaoDTO.getTipoDespesaInvestimentoId());

        Configuracao configuracaoSalva = configuracaoRepository.save(configuracao);
        log.info("Configuracoes salvas com sucesso: {}", configuracaoSalva);

        return mapToDTO(configuracaoSalva);
    }

    private ConfiguracaoDTO mapToDTO(Configuracao configuracao) {
        ConfiguracaoDTO dto = new ConfiguracaoDTO();
        dto.setId(configuracao.getId());
        dto.setTipoDespesaInvestimentoId(configuracao.getTipoDespesaInvestimentoId());
        return dto;
    }
}
