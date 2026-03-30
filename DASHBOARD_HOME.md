# Dashboard Home

## Objetivo

Fornecer os dados da tela inicial do frontend em uma unica requisicao.

O endpoint consolida:

- competencia (`ano` e `mes`)
- fechamento mensal do periodo
- resumo das contas correntes
- resumo das faturas por cartao

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

## Observacao para frontend

Fluxo recomendado para a home:

1. chamar `GET /api/v1/dashboard/home` sem parametros para abrir a tela
2. se houver seletor de competencia, recarregar com `ano` e `mes`
3. usar `fechado = false` para identificar que os dados ainda sao uma previa
4. usar `despesasPagasNoCaixa` para cards relacionados a saida real de caixa
5. usar `faturaLancada` e `diaVencimento` para destacar a situacao operacional dos cartoes
