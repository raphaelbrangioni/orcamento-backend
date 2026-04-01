# Geracao de Fatura de Cartao

## Objetivo

Registrar explicitamente a geracao da fatura de cartao como um processo proprio.

Esse fluxo:

- calcula os valores da fatura do cartao no mes
- separa a parte de terceiros da parte propria
- gera ou atualiza a despesa vinculada
- registra auditoria de quem gerou, reprocessou ou ajustou

Com isso, a despesa deixa de ser a fonte da verdade do processo.

## Conceitos

- `valorFatura`
  - valor bruto da fatura no mes
- `valorTerceiros`
  - parte da fatura atribuida a terceiros
- `valorProprio`
  - `valorFatura - valorTerceiros`
  - e o valor usado na despesa gerada

## Endpoints

### Gerar ou reprocessar fatura

`POST /api/v1/geracoes-fatura/{cartaoCreditoId}/{ano}/{mes}`

Exemplo:

```http
POST /api/v1/geracoes-fatura/11/2026/4
Authorization: Bearer <token>
x-tenant-id: 06660607625
```

Nao ha body.

Resposta `200 OK`:

```json
{
  "id": 2,
  "cartaoCreditoId": 11,
  "nomeCartao": "Latam Pass Itau Black - 7785",
  "ano": 2026,
  "mes": 4,
  "valorFatura": 5242.93,
  "valorTerceiros": 5071.36,
  "valorProprio": 171.57,
  "despesaId": 543,
  "nomeDespesa": "Fatura Cartao Latam Pass Itau Black - 7785",
  "dataVencimentoDespesa": "2026-04-02",
  "status": "GERADA",
  "geradoPor": "raphael",
  "geradoEm": "2026-03-31T21:15:21.8372701",
  "ultimoReprocessamentoPor": null,
  "ultimoReprocessamentoEm": null,
  "observacao": null,
  "ajustadoPor": null,
  "ajustadoEm": null
}
```

Regras:

- se ainda nao existir geracao para `cartao + ano + mes`, cria com `status = GERADA`
- se ja existir, reprocessa com `status = REPROCESSADA`
- o backend recalcula os valores a partir dos lancamentos atuais do cartao
- se a despesa vinculada ja estiver paga, o reprocessamento e bloqueado

### Consultar geracao especifica

`GET /api/v1/geracoes-fatura/{cartaoCreditoId}/{ano}/{mes}`

Exemplo:

```http
GET /api/v1/geracoes-fatura/11/2026/4
Authorization: Bearer <token>
x-tenant-id: 06660607625
```

Resposta `200 OK`:

```json
{
  "id": 2,
  "cartaoCreditoId": 11,
  "nomeCartao": "Latam Pass Itau Black - 7785",
  "ano": 2026,
  "mes": 4,
  "valorFatura": 5242.93,
  "valorTerceiros": 5071.36,
  "valorProprio": 171.57,
  "despesaId": 543,
  "nomeDespesa": "Fatura Cartao Latam Pass Itau Black - 7785",
  "dataVencimentoDespesa": "2026-04-02",
  "status": "GERADA",
  "geradoPor": "raphael",
  "geradoEm": "2026-03-31T21:15:21.8372701",
  "ultimoReprocessamentoPor": null,
  "ultimoReprocessamentoEm": null,
  "observacao": null,
  "ajustadoPor": null,
  "ajustadoEm": null
}
```

Se nao existir geracao, retorna `404 Not Found`.

### Listar geracoes do mes

`GET /api/v1/geracoes-fatura?ano={ano}&mes={mes}`

Exemplo:

```http
GET /api/v1/geracoes-fatura?ano=2026&mes=4
Authorization: Bearer <token>
x-tenant-id: 06660607625
```

Resposta `200 OK`:

```json
[
  {
    "id": 2,
    "cartaoCreditoId": 11,
    "nomeCartao": "Latam Pass Itau Black - 7785",
    "ano": 2026,
    "mes": 4,
    "valorFatura": 5242.93,
    "valorTerceiros": 5071.36,
    "valorProprio": 171.57,
    "despesaId": 543,
    "nomeDespesa": "Fatura Cartao Latam Pass Itau Black - 7785",
    "dataVencimentoDespesa": "2026-04-02",
    "status": "GERADA",
    "geradoPor": "raphael",
    "geradoEm": "2026-03-31T21:15:21.8372701",
    "ultimoReprocessamentoPor": null,
    "ultimoReprocessamentoEm": null,
    "observacao": null,
    "ajustadoPor": null,
    "ajustadoEm": null
  }
]
```

Uso recomendado:

- tela por competencia
- listar todas as faturas geradas de todos os cartoes do tenant

### Ajustar manualmente a geracao

`PATCH /api/v1/geracoes-fatura/{id}`

Exemplo:

```http
PATCH /api/v1/geracoes-fatura/2
Authorization: Bearer <token>
x-tenant-id: 06660607625
Content-Type: application/json
```

Body:

```json
{
  "valorTerceiros": 5000.00,
  "observacao": "Ajuste manual apos conferencia"
}
```

Resposta `200 OK`:

```json
{
  "id": 2,
  "cartaoCreditoId": 11,
  "nomeCartao": "Latam Pass Itau Black - 7785",
  "ano": 2026,
  "mes": 4,
  "valorFatura": 5242.93,
  "valorTerceiros": 5000.00,
  "valorProprio": 242.93,
  "despesaId": 543,
  "nomeDespesa": "Fatura Cartao Latam Pass Itau Black - 7785",
  "dataVencimentoDespesa": "2026-04-02",
  "status": "GERADA",
  "geradoPor": "raphael",
  "geradoEm": "2026-03-31T21:15:21.8372701",
  "ultimoReprocessamentoPor": null,
  "ultimoReprocessamentoEm": null,
  "observacao": "Ajuste manual apos conferencia",
  "ajustadoPor": "raphael",
  "ajustadoEm": "2026-03-31T22:10:00"
}
```

Regras:

- nesta V1, so e permitido ajustar:
  - `valorTerceiros`
  - `observacao`
- o backend recalcula:
  - `valorProprio = valorFatura - valorTerceiros`
- a despesa vinculada e atualizada com o novo `valorProprio`
- se a despesa vinculada ja estiver paga, o ajuste e bloqueado

## Campos da resposta

- `id`
- `cartaoCreditoId`
- `nomeCartao`
- `ano`
- `mes`
- `valorFatura`
- `valorTerceiros`
- `valorProprio`
- `despesaId`
- `nomeDespesa`
- `dataVencimentoDespesa`
- `status`
- `geradoPor`
- `geradoEm`
- `ultimoReprocessamentoPor`
- `ultimoReprocessamentoEm`
- `observacao`
- `ajustadoPor`
- `ajustadoEm`

## Regras de negocio

- O fluxo e multi-tenant.
- A geracao da fatura tem unicidade por:
  - `tenant`
  - `cartao`
  - `ano`
  - `mes`
- `valorProprio = valorFatura - valorTerceiros`.
- A despesa gerada usa sempre o `valorProprio`.
- O nome da despesa segue o padrao:
  - `Fatura Cartao {nome do cartao}`
- O vencimento da despesa usa o `diaVencimento` do cartao na competencia informada.
- `POST` nao e uma edicao manual:
  - ele recalcula a fatura com base nos dados atuais do sistema
- `PATCH` e um ajuste manual controlado
- Depois que a despesa da fatura estiver paga:
  - nao pode reprocessar
  - nao pode ajustar manualmente

## Erros esperados

### Fatura sem valor no mes

`400 BAD_REQUEST`

```json
{
  "message": "Nao existe valor de fatura para o cartao na competencia informada"
}
```

### Cartao inexistente

`400 BAD_REQUEST`

```json
{
  "message": "Cartao de credito nao encontrado"
}
```

### Reprocessamento bloqueado por despesa paga

`400 BAD_REQUEST`

```json
{
  "message": "Nao e permitido reprocessar uma fatura cuja despesa ja foi paga"
}
```

### Ajuste manual invalido

`400 BAD_REQUEST`

```json
{
  "message": "valorTerceiros nao pode ser maior que valorFatura"
}
```

### Ajuste bloqueado por despesa paga

`400 BAD_REQUEST`

```json
{
  "message": "Nao e permitido ajustar uma geracao cuja despesa vinculada ja foi paga"
}
```

## Observacao para frontend

Fluxo recomendado:

1. criar tela por competencia usando `GET /api/v1/geracoes-fatura?ano&mes`
2. listar por linha:
   - cartao
   - valor fatura
   - valor terceiros
   - valor proprio
   - despesa vinculada
   - status
   - gerado por/em
   - ajustado por/em
3. oferecer as acoes:
   - `Detalhes`
   - `Abrir despesa`
   - `Atualizar fatura`
   - `Editar ajuste`
4. usar `POST` quando a intencao for recalcular a fatura
5. usar `PATCH` quando a intencao for ajustar manualmente `valorTerceiros` e `observacao`

Validacoes sugeridas no frontend para o `PATCH`:

- `valorTerceiros >= 0`
- `valorTerceiros <= valorFatura`

Mesmo com validacao local, o frontend deve tratar `400` retornado pelo backend.
