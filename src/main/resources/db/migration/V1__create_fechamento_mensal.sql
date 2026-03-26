CREATE TABLE IF NOT EXISTS fechamento_mensal (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id VARCHAR(255) NOT NULL,
    ano INT NOT NULL,
    mes INT NOT NULL,
    saldo_inicial DECIMAL(19,2) NOT NULL,
    receitas_realizadas DECIMAL(19,2) NOT NULL,
    despesas_do_mes DECIMAL(19,2) NOT NULL,
    despesas_pagas DECIMAL(19,2) NOT NULL,
    despesas_pagas_cartao DECIMAL(19,2) NOT NULL,
    total_faturas DECIMAL(19,2) NOT NULL,
    total_terceiros_faturas DECIMAL(19,2) NOT NULL,
    saldo_final DECIMAL(19,2) NOT NULL,
    calculado_em DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_fechamento_mensal_tenant_ano_mes UNIQUE (tenant_id, ano, mes)
);
