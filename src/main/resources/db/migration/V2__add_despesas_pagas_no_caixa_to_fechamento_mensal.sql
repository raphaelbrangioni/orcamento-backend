ALTER TABLE fechamento_mensal
ADD COLUMN despesas_pagas_no_caixa DECIMAL(19,2) NOT NULL DEFAULT 0.00
AFTER despesas_pagas;

UPDATE fechamento_mensal
SET despesas_pagas_no_caixa = despesas_pagas - despesas_pagas_cartao
WHERE despesas_pagas_no_caixa = 0.00;
