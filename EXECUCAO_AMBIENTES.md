# Execucao dos ambientes

Este documento define como executar o backend `orcamento` nos ambientes `dev`, `hml` e `prod`.

## Resumo

- `dev`: execucao local no IntelliJ ou via terminal com `mvnw.cmd`
- `hml`: execucao no Kubernetes local
- `prod`: execucao no Kubernetes local

O perfil padrao da aplicacao e `dev`, conforme [`src/main/resources/application.yaml`](C:/Projetos/orcamento/src/main/resources/application.yaml).

## Pre-requisitos

- Java 17
- MySQL acessivel
- Maven Wrapper do projeto
- Variaveis de ambiente configuradas para o perfil usado

## Dev no terminal

Com PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DEV_DB_USERNAME="root"
$env:DEV_DB_PASSWORD="admin.2011"
.\mvnw.cmd -DskipTests spring-boot:run
```

Se quiser rebuild completo antes de subir:

```powershell
.\mvnw.cmd clean compile
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DEV_DB_USERNAME="root"
$env:DEV_DB_PASSWORD="admin.2011"
.\mvnw.cmd -DskipTests spring-boot:run
```

## Dev no IntelliJ

Criar uma `Run Configuration` do tipo `Spring Boot` para a classe principal:

- [`src/main/java/com/example/orcamento/OrcamentoApplication.java`](C:/Projetos/orcamento/src/main/java/com/example/orcamento/OrcamentoApplication.java)

Configurar:

- `JDK`: 17
- `Active profiles`: `dev`
- `Environment variables`: `DEV_DB_USERNAME=root;DEV_DB_PASSWORD=admin.2011`

Observacao:

- Se a aplicacao iniciar com `using password: NO`, a execucao nao esta usando essa configuracao do IntelliJ ou as variaveis nao chegaram ao processo.

## Perfil dev

Arquivo:

- [`src/main/resources/application-dev.yaml`](C:/Projetos/orcamento/src/main/resources/application-dev.yaml)

Defaults relevantes:

- banco: `jdbc:mysql://localhost:3306/orcamento-dev`
- usuario: `root`
- senha: vazia por padrao
- porta da aplicacao: `8045`
- upload local: `./uploads/dev`

## Perfil hml

Arquivo:

- [`src/main/resources/application-hml.yaml`](C:/Projetos/orcamento/src/main/resources/application-hml.yaml)

Variaveis esperadas:

- `HML_DB_URL`
- `HML_DB_USERNAME`
- `HML_DB_PASSWORD`
- `HML_MAIL_HOST`
- `HML_MAIL_PORT`
- `HML_MAIL_USERNAME`
- `HML_MAIL_PASSWORD`
- `HML_CORS_ALLOWED_ORIGIN`

Execucao esperada:

- deploy no Kubernetes local
- profile ativo: `hml`

## Perfil prod

Arquivo:

- [`src/main/resources/application-prod.yaml`](C:/Projetos/orcamento/src/main/resources/application-prod.yaml)

Variaveis obrigatorias:

- `PROD_DB_URL`
- `PROD_DB_USERNAME`
- `PROD_DB_PASSWORD`
- `PROD_MAIL_HOST`
- `PROD_MAIL_USERNAME`
- `PROD_MAIL_PASSWORD`
- `PROD_MAIL_FROM`
- `PROD_CORS_ALLOWED_ORIGIN`

Execucao esperada:

- deploy no Kubernetes local
- profile ativo: `prod`
- `ddl-auto=validate`
- Swagger desabilitado

## Docker e imagem

Arquivos relacionados:

- [`Dockerfile`](C:/Projetos/orcamento/Dockerfile)
- [`build_backend.sh`](C:/Projetos/orcamento/build_backend.sh)

Build e push da imagem:

```bash
DOCKER_USER=<seu_usuario> ./build_backend.sh <versao>
```

## Kubernetes local

Como voce descreveu o fluxo atual:

- o cluster Kubernetes roda localmente na sua maquina
- a infraestrutura foi criada sobre Hyper-V
- `hml` e `prod` sao executados nesse cluster

Padrao recomendado de configuracao por ambiente:

- `SPRING_PROFILES_ACTIVE=hml` no deployment de homologacao
- `SPRING_PROFILES_ACTIVE=prod` no deployment de producao
- credenciais e URLs sensiveis via `Secret`
- configuracoes nao sensiveis via `ConfigMap`

## Problemas ja validados

### Falha de login no MySQL ao subir localmente

Erro:

- `Access denied for user 'root'@'localhost' (using password: NO)`

Causa:

- a aplicacao subiu sem `DEV_DB_PASSWORD`

Solucao validada:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DEV_DB_USERNAME="root"
$env:DEV_DB_PASSWORD="admin.2011"
.\mvnw.cmd -DskipTests spring-boot:run
```

### LazyInitializationException em `/api/v1/tipos-despesa`

Causa:

- serializacao direta de entidade JPA com relacionamento lazy

Status:

- endpoint ajustado para responder com DTO
