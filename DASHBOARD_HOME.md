# Dashboard Home

## Objetivo

Fornecer os dados da tela inicial do frontend em uma unica requisicao.

O endpoint consolida:

- competencia (`ano` e `mes`)
- fechamento mensal do periodo
- resumo das contas correntes
- resumo das faturas por cartao
- alertas operacionais da home

## Endpoint

`GET /api/v1/dashboard/home`

## Parametros opcionais

- `ano`
- `mes`

Se `ano` e `mes` nao forem enviados, a API usa a competencia atual.

Exemplo:

```http
GET /api/v1/dashboard/home?ano=2026&mes=2
Authorization: Bearer <token>
x-tenant-id: 06660607625
```

## Resposta

```json
{
  "ano": 2026,
  "mes": 2,
  "fechamentoMensal": {
    "id": 5,
    "ano": 2026,
    "mes": 2,
    "fechado": true,
    "saldoInicial": 1824.08,
    "receitasRealizadas": 23923.96,
    "despesasDoMes": 30412.08,
    "despesasPagas": 29208.37,
    "despesasPagasNoCaixa": 28130.68,
    "despesasPagasCartao": 1077.69,
    "totalFaturas": 22155.99,
    "totalTerceirosFaturas": 8359.40,
    "totalFaturasProprias": 13796.59,
    "totalFaturasLancadasComoDespesa": 12000.00,
    "totalFaturasNaoLancadas": 1796.59,
    "saldoFinal": 26669.35,
    "calculadoEm": "2026-03-26T18:00:00.123"
  },
  "contas": {
    "quantidadeContasAtivas": 2,
    "saldoTotal": 13882.36
  },
  "totalFaturasCartoes": 22155.99,
  "cartoes": [
    {
      "cartaoId": 1,
      "nome": "Nubank",
      "diaVencimento": 10,
      "valorFatura": 15000.00,
      "valorTerceiros": 5000.00,
      "faturaLancada": true
    },
    {
      "cartaoId": 2,
      "nome": "Itau",
      "diaVencimento": 15,
      "valorFatura": 7155.99,
      "valorTerceiros": 3359.40,
      "faturaLancada": false
    }
  ],
  "alertas": [
    {
      "tipo": "FATURAS_NAO_LANCADAS",
      "nivel": "warning",
      "titulo": "Existem faturas proprias nao lancadas",
      "mensagem": "Parte das faturas do mes ainda nao foi lancada como despesa.",
      "quantidade": 2,
      "valor": 1796.59
    },
    {
      "tipo": "FATURAS_A_VENCER",
      "nivel": "warning",
      "titulo": "Existem faturas a vencer",
      "mensagem": "Ha faturas proprias com vencimento futuro na competencia selecionada.",
      "quantidade": 2,
      "valor": 9500.00
    }
  ]
}
```

## Campos

### Raiz

- `ano`
- `mes`
- `fechamentoMensal`
- `contas`
- `totalFaturasCartoes`
- `cartoes`
- `alertas`

### `fechamentoMensal`

Nao vem mais `null`.

Agora esse bloco sempre e retornado no mesmo formato:

- se o mes estiver efetivamente fechado, vem com `fechado = true`
- se o mes estiver em aberto ou for futuro, vem como previa com `fechado = false`

Campos:

- `id`
- `ano`
- `mes`
- `fechado`
- `saldoInicial`
- `receitasRealizadas`
- `despesasDoMes`
- `despesasPagas`
- `despesasPagasNoCaixa`
- `despesasPagasCartao`
- `totalFaturas`
- `totalTerceirosFaturas`
- `totalFaturasProprias`
- `totalFaturasLancadasComoDespesa`
- `totalFaturasNaoLancadas`
- `saldoFinal`
- `calculadoEm`

### `contas`

- `quantidadeContasAtivas`
- `saldoTotal`

### `cartoes[]`

- `cartaoId`
- `nome`
- `diaVencimento`
- `valorFatura`
- `valorTerceiros`
- `faturaLancada`

### `alertas[]`

- `tipo`
- `nivel`
- `titulo`
- `mensagem`
- `quantidade`
- `valor`

## Regras importantes

- A API e multi-tenant.
- O bloco `fechamentoMensal` sempre e retornado.
- Quando o mes ainda nao estiver fechado, os valores representam uma previa calculada.
- O bloco `cartoes` sempre pode ser retornado, mesmo sem fechamento mensal.
- `totalFaturasCartoes` e a soma de `valorFatura` de todos os cartoes retornados.
- `despesasDoMes`, `despesasPagas`, `despesasPagasNoCaixa` e `despesasPagasCartao` usam a mesma competencia por `dataVencimento`.
- `despesasPagas = despesasPagasNoCaixa + despesasPagasCartao`.
- `despesasPagasNoCaixa` representa despesas do mes pagas fora do cartao de credito.
- `despesasPagasCartao` representa despesas do mes pagas com `formaPagamento = CREDITO`.
- `saldoFinal` usa a formula `saldoInicial + receitasRealizadas - despesasPagasNoCaixa`.
- `faturaLancada` indica se a despesa da fatura daquele cartao/mes ja foi criada no sistema.
- `totalFaturasProprias = totalFaturas - totalTerceirosFaturas`.
- `totalFaturasLancadasComoDespesa` representa a parte das faturas que ja virou despesa no sistema.
- `totalFaturasNaoLancadas` representa a parte das faturas proprias que ainda nao virou despesa.
- `alertas` pode vir vazio.
- Tipos atuais de alerta:
  - `FATURAS_NAO_LANCADAS`
  - `FATURAS_A_VENCER`
  - `DESPESAS_VENCIDAS_NAO_PAGAS`
  - `CONTAS_NEGATIVAS`
- `FATURAS_A_VENCER`:
  - no mes atual considera faturas proprias com vencimento a partir do dia atual
  - em mes futuro considera as faturas proprias do mes
  - em mes passado nao e retornado

## Observacao para frontend

Fluxo recomendado para a home:

1. chamar `GET /api/v1/dashboard/home` sem parametros para abrir a tela
2. se houver seletor de competencia, recarregar com `ano` e `mes`
3. usar `fechado = false` para identificar que os dados ainda sao uma previa
4. usar `despesasPagasNoCaixa` para cards relacionados a saida real de caixa
5. usar `faturaLancada` e `diaVencimento` para destacar a situacao operacional dos cartoes
6. usar `alertas` para destacar acoes pendentes e riscos operacionais na home
