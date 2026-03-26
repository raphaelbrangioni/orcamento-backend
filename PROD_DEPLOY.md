# Deploy Backend PROD

Este documento descreve o fluxo de deploy do backend no ambiente de producao (`prod`).

## Contexto atual

O ambiente `prod` roda no cluster Kubernetes local e usa:

- namespace: `orcamento`
- release Helm: `orcamento-backend`
- deployment: `orcamento-backend`
- container: `orcamento-backend`
- registry de imagem: `harbor.orcamento.local`
- projeto no Harbor: `orcamento`
- imagem: `harbor.orcamento.local/orcamento/orcamento-backend`
- chart OCI: `oci://harbor.orcamento.local/orcamento/charts/orcamento-backend`

## Arquivos usados no deploy

Script:

- [`deploy-backend-prod.ps1`](C:/Projetos/build_deploy/deploy-backend-prod.ps1)

Arquivos de configuracao:

- [`deploy/backend/prod/values.yaml`](C:/Projetos/orcamento/deploy/backend/prod/values.yaml)
- [`deploy/backend/prod/application.yaml`](C:/Projetos/orcamento/deploy/backend/prod/application.yaml)

## Valores padrao do PROD

- namespace: `orcamento`
- release: `orcamento-backend`
- deployment: `orcamento-backend`
- banco: `orcamento`
- host MySQL: `mysql-external.orcamento.svc.cluster.local`
- secret de backend: `orcamento-backend-secrets`
- chart version padrao: `1.1.10`

Origins previstos em CORS:

- `http://orcamento.local`
- `http://api.orcamento.local`
- `http://localhost:*`

## Script de deploy

Comportamento do script [`deploy-backend-prod.ps1`](C:/Projetos/build_deploy/deploy-backend-prod.ps1):

1. Faz build da imagem Docker do backend.
2. Faz push da imagem para o Harbor.
3. Executa `helm upgrade --install`.
4. Injeta o `application.yaml` via `--set-file`.
5. Aguarda o rollout do deployment.
6. Lista os pods no namespace `orcamento`.

## Protecao de producao

O script exige protecao explicita para aplicar no namespace `orcamento`.

Para deploy real, e obrigatorio usar:

- `-AllowProd`

E confirmar manualmente digitando:

- `PROD`

Essa protecao nao e exigida para `-DryRun`.

## Como executar

### Dry run

Para validar sem aplicar:

```powershell
powershell -ExecutionPolicy Bypass -File C:\Projetos\build_deploy\deploy-backend-prod.ps1 -DryRun
```

### Deploy real

```powershell
powershell -ExecutionPolicy Bypass -File C:\Projetos\build_deploy\deploy-backend-prod.ps1 -AllowProd
```

### Deploy com versao explicita

```powershell
powershell -ExecutionPolicy Bypass -File C:\Projetos\build_deploy\deploy-backend-prod.ps1 -AllowProd -Version 1.1.12
```

### Deploy sem rebuild da imagem

Use quando a imagem ja tiver sido publicada no Harbor:

```powershell
powershell -ExecutionPolicy Bypass -File C:\Projetos\build_deploy\deploy-backend-prod.ps1 -AllowProd -Version 1.1.12 -SkipBuildPush
```

## Parametros importantes

- `-Version`
  - tag da imagem
- `-RegistryHost`
  - por padrao `harbor.orcamento.local`
- `-RegistryProject`
  - por padrao `orcamento`
- `-RegistryUser`
  - usuario do Harbor
- `-RegistryPassword`
  - senha do Harbor
- `-ChartPath`
  - por padrao `oci://harbor.orcamento.local/orcamento/charts/orcamento-backend`
- `-ChartVersion`
  - por padrao `1.1.10`
- `-ValuesFile`
  - por padrao `C:\Projetos\orcamento\deploy\backend\prod\values.yaml`
- `-ApplicationYamlFile`
  - por padrao `C:\Projetos\orcamento\deploy\backend\prod\application.yaml`
- `-Namespace`
  - por padrao `orcamento`
- `-ReleaseName`
  - por padrao `orcamento-backend`
- `-AllowProd`
  - habilita aplicacao real em producao
- `-SkipBuildPush`
  - pula build e push da imagem
- `-DryRun`
  - renderiza e valida sem aplicar

## Configuracao aplicada em PROD

O `application.yaml` de producao:

- fixa `spring.profiles.active: prod`
- usa `ddl-auto: validate`
- habilita Flyway
- define `app.cors` explicitamente
- e entregue ao pod via `ConfigMap`

O pod le a configuracao a partir de:

- `SPRING_CONFIG_ADDITIONAL_LOCATION=/config/`

com mount do `ConfigMap`:

- `orcamento-backend-config`

## Validacoes uteis

Ver releases:

```powershell
helm list -A
```

Ver deployment:

```powershell
kubectl get deploy -n orcamento
```

Ver pods:

```powershell
kubectl get pods -n orcamento -o wide
```

Ver logs:

```powershell
kubectl logs -n orcamento deployment/orcamento-backend -f
```

Ver valores aplicados na release:

```powershell
helm get values orcamento-backend -n orcamento -a
```

Ver manifest gerado:

```powershell
helm get manifest orcamento-backend -n orcamento
```

Ver ConfigMap em uso:

```powershell
kubectl get configmap orcamento-backend-config -n orcamento -o yaml
```

## Dependencias operacionais

Para o deploy funcionar corretamente, o ambiente precisa ter:

- acesso ao cluster Kubernetes
- acesso ao Harbor
- chart OCI publicado no Harbor
- secret `orcamento-backend-secrets` criado no namespace `orcamento`

Esse secret deve fornecer ao menos:

- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `SPRING_MAIL_PASSWORD`

## Observacoes

- O script de `prod` usa Harbor por padrao, nao `docker.io`.
- O chart backend e consumido via OCI no Harbor.
- O `DryRun` de `prod` foi validado sem aplicar nada no namespace `orcamento`.
