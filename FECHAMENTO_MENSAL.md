# Fechamento Mensal

## Objetivo

O fechamento mensal consolida os principais valores financeiros de um mes ja encerrado para o tenant atual.

Essa funcionalidade gera um snapshot com:

- `ano`
- `mes`
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

## Regras de negocio

- O fechamento e multi-tenant.
- O mes nao pode ser futuro.
- O fechamento mensal depende do fechamento diario.
- Para fechar um mes, o ultimo dia util do mes deve estar fechado em todas as contas correntes ativas.
- Sabados, domingos e feriados nacionais sao ignorados ao determinar o ultimo dia util.
- O fechamento e `upsert`: se o mes ja existir, ele e recalculado e atualizado.

## Origem dos valores

- `saldoInicial`
  - se existir fechamento mensal do mes anterior, o sistema usa o `saldoFinal` desse fechamento anterior
  - se nao existir fechamento mensal anterior, o sistema usa `0`
- `receitasRealizadas`
  - soma das receitas nao previstas com `dataRecebimento` no mes
- `despesasDoMes`
  - soma de `valorPrevisto` das despesas com `dataVencimento` no mes
- `despesasPagas`
  - soma de `valorPago` das despesas do mes com `dataVencimento` no mes
- `despesasPagasNoCaixa`
  - soma de `valorPago` das despesas do mes com `dataVencimento` no mes e `formaPagamento != CREDITO`
- `despesasPagasCartao`
  - soma de `valorPago` das despesas do mes com `dataVencimento` no mes e `formaPagamento = CREDITO`
- `totalFaturas`
  - soma dos lancamentos de cartao da `mesAnoFatura` correspondente ao mes
- `totalTerceirosFaturas`
  - subtotal de lancamentos de cartao com `proprietario = "Terceiros"`
- `saldoFinal`
  - `saldoInicial + receitasRealizadas - despesasPagasNoCaixa`

## APIs

### Fechar mes

`POST /api/v1/fechamentos-mensais/{ano}/{mes}/fechar`

Exemplo:

```http
POST /api/v1/fechamentos-mensais/2026/3/fechar
Authorization: Bearer <token>
x-tenant-id: 06660607625
```

Nao ha body.

Resposta `200 OK`:

```json
{
  "id": 1,
  "ano": 2026,
  "mes": 3,
  "saldoInicial": 9396.36,
  "receitasRealizadas": 1500.00,
  "despesasDoMes": 2300.00,
  "despesasPagas": 2100.00,
  "despesasPagasNoCaixa": 1119.50,
  "despesasPagasCartao": 980.50,
  "totalFaturas": 1200.50,
  "totalTerceirosFaturas": 220.00,
  "saldoFinal": 8512.40,
  "calculadoEm": "2026-03-26T15:20:00.123"
}
```

### Consultar fechamento do mes

`GET /api/v1/fechamentos-mensais/{ano}/{mes}`

Exemplo:

```http
GET /api/v1/fechamentos-mensais/2026/3
Authorization: Bearer <token>
x-tenant-id: 06660607625
```

Resposta `200 OK`:

```json
{
  "id": 1,
  "ano": 2026,
  "mes": 3,
  "saldoInicial": 9396.36,
  "receitasRealizadas": 1500.00,
  "despesasDoMes": 2300.00,
  "despesasPagas": 2100.00,
  "despesasPagasNoCaixa": 1119.50,
  "despesasPagasCartao": 980.50,
  "totalFaturas": 1200.50,
  "totalTerceirosFaturas": 220.00,
  "saldoFinal": 8512.40,
  "calculadoEm": "2026-03-26T15:20:00.123"
}
```

### Reabrir mes

`DELETE /api/v1/fechamentos-mensais/{ano}/{mes}`

Exemplo:

```http
DELETE /api/v1/fechamentos-mensais/2026/3
Authorization: Bearer <token>
x-tenant-id: 06660607625
```

Resposta `204 No Content`

## Erros esperados

### Mes futuro

`400 BAD_REQUEST`

```json
{
  "timestamp": "2026-03-26T15:20:00-03:00",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Nao e permitido fechar um mes futuro",
  "path": "/api/v1/fechamentos-mensais/2027/1/fechar",
  "traceId": "..."
}
```

### Ultimo dia util nao fechado

`400 BAD_REQUEST`

```json
{
  "timestamp": "2026-03-26T15:20:00-03:00",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Nao e permitido fechar o mes sem fechar o ultimo dia util da conta Banco X em 2026-03-31",
  "path": "/api/v1/fechamentos-mensais/2026/3/fechar",
  "traceId": "..."
}
```

### Fechamento inexistente

`400 BAD_REQUEST`

```json
{
  "timestamp": "2026-03-26T15:20:00-03:00",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Fechamento mensal nao encontrado para 2026/3",
  "path": "/api/v1/fechamentos-mensais/2026/3",
  "traceId": "..."
}
```

## Observacao para frontend

Para a primeira tela, o frontend pode operar de forma simples:

- selecionar `ano`
- selecionar `mes`
- acionar `POST /fechamentos-mensais/{ano}/{mes}/fechar`
- exibir o retorno consolidado
- usar `GET /fechamentos-mensais/{ano}/{mes}` para reconsulta
- usar `DELETE /fechamentos-mensais/{ano}/{mes}` quando precisar reabrir e recalcular
- considerar `despesasPagasNoCaixa` como saida real de caixa do mes
- considerar `despesasPagasCartao` como despesas do mes pagas via cartao, sem impacto direto no saldo final

Nao existe body no `POST`.
