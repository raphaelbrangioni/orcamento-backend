package com.example.orcamento.service;

import com.example.orcamento.dto.TransacaoFinanceiraDTO;
import com.example.orcamento.service.TransacaoFinanceiraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertaService {

    private static final Logger log = LoggerFactory.getLogger(AlertaService.class);

    @Autowired
    private DespesaService despesaService;

    @Autowired
    private TransacaoFinanceiraService transacaoFinanceiraService;

    public List<Map<String, Object>> verificarLimites() {
        List<Map<String, Object>> alertasDetalhados = new ArrayList<>();
        log.info("Funcionalidade de limites foi removida do sistema");
        return alertasDetalhados;
    }
}