ALTER TABLE fechamento_mensal
    ADD COLUMN fechado_por VARCHAR(255) NOT NULL DEFAULT 'desconhecido';

ALTER TABLE fechamento_mensal
    ADD COLUMN fechado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE fechamento_mensal
    ADD COLUMN ultimo_reprocessamento_por VARCHAR(255) NULL;

ALTER TABLE fechamento_mensal
    ADD COLUMN ultimo_reprocessamento_em TIMESTAMP NULL;

CREATE TABLE fechamento_mensal_historico (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    fechamento_mensal_id BIGINT NULL,
    ano INT NOT NULL,
    mes INT NOT NULL,
    evento VARCHAR(50) NOT NULL,
    username VARCHAR(255) NOT NULL,
    ocorrido_em TIMESTAMP NOT NULL
);
