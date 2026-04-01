CREATE TABLE geracao_fatura_cartao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    cartao_credito_id BIGINT NOT NULL,
    ano INT NOT NULL,
    mes INT NOT NULL,
    valor_fatura DECIMAL(19,2) NOT NULL,
    valor_terceiros DECIMAL(19,2) NOT NULL,
    valor_proprio DECIMAL(19,2) NOT NULL,
    despesa_id BIGINT NULL,
    status VARCHAR(50) NOT NULL,
    gerado_por VARCHAR(255) NOT NULL,
    gerado_em TIMESTAMP NOT NULL,
    ultimo_reprocessamento_por VARCHAR(255) NULL,
    ultimo_reprocessamento_em TIMESTAMP NULL,
    CONSTRAINT uk_geracao_fatura_cartao_tenant_cartao_ano_mes UNIQUE (tenant_id, cartao_credito_id, ano, mes)
);
