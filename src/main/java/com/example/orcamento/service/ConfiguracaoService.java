package com.example.orcamento.service;

import com.example.orcamento.dto.ConfiguracaoDTO;
import com.example.orcamento.model.Configuracao;
import com.example.orcamento.repository.ConfiguracaoRepository;
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
        Configuracao configuracao = configuracaoRepository.findFirstByOrderByIdAsc();

        if (configuracao == null) {
            return new ConfiguracaoDTO();
        }

        return mapToDTO(configuracao);
    }

    @Transactional
    public ConfiguracaoDTO salvarConfiguracoes(ConfiguracaoDTO configuracaoDTO) {
        Configuracao configuracao = configuracaoRepository.findFirstByOrderByIdAsc();

        if (configuracao == null) {
            configuracao = new Configuracao();
        }

        configuracao.setTipoDespesaInvestimentoId(configuracaoDTO.getTipoDespesaInvestimentoId());

        Configuracao configuracaoSalva = configuracaoRepository.save(configuracao);
        log.info("Configurações salvas com sucesso: {}", configuracaoSalva);

        return mapToDTO(configuracaoSalva);
    }

    private ConfiguracaoDTO mapToDTO(Configuracao configuracao) {
        return ConfiguracaoDTO.builder()
                .id(configuracao.getId())
                .tipoDespesaInvestimentoId(configuracao.getTipoDespesaInvestimentoId())
                .build();
    }
}