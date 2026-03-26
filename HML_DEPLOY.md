# Deploy Backend HML

Este documento descreve o fluxo de deploy do backend no ambiente de homologacao (`hml`).

## Contexto atual

O ambiente `hml` roda no cluster Kubernetes local e usa:

- namespace: `orcamento-hml`
- release Helm: `orcamento-backend-hml`
- deployment: `orcamento-backend-hml`
- container: `orcamento-backend`
- registry de imagem: `harbor.orcamento.local`
- projeto no Harbor: `orcamento`
- imagem: `harbor.orcamento.local/orcamento/orcamento-backend`

O cluster esta acessivel via `kubectl`, e a release atual pode ser vista com:

```powershell
helm list -A
```

## Arquivos usados no deploy

Script:

- [`deploy-backend-hml.ps1`](C:/Projetos/build_deploy/deploy-backend-hml.ps1)

Arquivos de configuracao:

- [`deploy/backend/hml/values.yaml`](C:/Projetos/orcamento/deploy/backend/hml/values.yaml)
- [`deploy/backend/hml/application.yaml`](C:/Projetos/orcamento/deploy/backend/hml/application.yaml)

Chart Helm local:

- [`Chart.yaml`](C:/Projetos/helm/orcamento-backend/Chart.yaml)
- [`values.yaml`](C:/Projetos/helm/orcamento-backend/values.yaml)

## Valores padrao do HML

- namespace: `orcamento-hml`
- release: `orcamento-backend-hml`
- banco: `orcamento-hml`
- host MySQL: `mysql-external.orcamento.svc.cluster.local`
- secret de backend: `orcamento-backend-hml-secrets`
- CORS:
  - `http://hml.orcamento.local`
  - `http://api.hml.orcamento.local`
  - `http://localhost:8080`

## Script de deploy

Comportamento do script [`deploy-backend-hml.ps1`](C:/Projetos/build_deploy/deploy-backend-hml.ps1):

1. Faz build da imagem Docker do backend.
2. Faz push da imagem para o Harbor.
3. Executa `helm upgrade --install`.
4. Injeta o `application.yaml` via `--set-file`.
5. Aguarda o rollout do deployment.
6. Lista os pods no namespace `orcamento-hml`.

## Como executar

### Dry run

Para validar sem aplicar:

```powershell
powershell -ExecutionPolicy Bypass -File C:\Projetos\build_deploy\deploy-backend-hml.ps1 -DryRun
```

### Deploy normal

```powershell
powershell -ExecutionPolicy Bypass -File C:\Projetos\build_deploy\deploy-backend-hml.ps1
```

O script gera por padrao uma tag no formato:

```text
hml-YYYYMMDD-HHmm
```

Exemplo:

```text
hml-20260325-1237
```

### Deploy com versao explicita

```powershell
powershell -ExecutionPolicy Bypass -File C:\Projetos\build_deploy\deploy-backend-hml.ps1 -Version hml-20260325-1237
```

### Deploy sem rebuild da imagem

Use quando a imagem ja tiver sido publicada no Harbor:

```powershell
powershell -ExecutionPolicy Bypass -File C:\Projetos\build_deploy\deploy-backend-hml.ps1 -Version hml-20260325-1237 -SkipBuildPush
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
  - por padrao `C:\Projetos\helm\orcamento-backend`
- `-ValuesFile`
  - por padrao `C:\Projetos\orcamento\deploy\backend\hml\values.yaml`
- `-ApplicationYamlFile`
  - por padrao `C:\Projetos\orcamento\deploy\backend\hml\application.yaml`
- `-Namespace`
  - por padrao `orcamento-hml`
- `-ReleaseName`
  - por padrao `orcamento-backend-hml`
- `-SkipBuildPush`
  - pula build e push da imagem
- `-DryRun`
  - renderiza e valida sem aplicar

## Validacoes uteis

Ver releases:

```powershell
helm list -A
```

Ver deployment de homologacao:

```powershell
kubectl get deploy -n orcamento-hml
```

Ver pods:

```powershell
kubectl get pods -n orcamento-hml -o wide
```

Ver logs:

```powershell
kubectl logs -n orcamento-hml deployment/orcamento-backend-hml -f
```

Ver valores aplicados na release:

```powershell
helm get values orcamento-backend-hml -n orcamento-hml -a
```

## Dependencias operacionais

Para o deploy funcionar corretamente, o ambiente precisa ter:

- acesso ao cluster Kubernetes
- acesso ao Harbor
- chart Helm do backend
- secret `orcamento-backend-hml-secrets` criado no namespace `orcamento-hml`

Esse secret deve fornecer ao menos:

- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `SPRING_MAIL_PASSWORD`

## Observacoes

- O script de `hml` usa Harbor por padrao, nao `docker.io`.
- O chart atual usado no `dry-run` foi o chart local em `C:\Projetos\helm\orcamento-backend`.
- O `application.yaml` de `hml` fica versionado no repositorio e e injetado no ConfigMap do release.
