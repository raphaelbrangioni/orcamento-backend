ALTER TABLE geracao_fatura_cartao
    ADD COLUMN observacao VARCHAR(1000) NULL;

ALTER TABLE geracao_fatura_cartao
    ADD COLUMN ajustado_por VARCHAR(255) NULL;

ALTER TABLE geracao_fatura_cartao
    ADD COLUMN ajustado_em TIMESTAMP NULL;
