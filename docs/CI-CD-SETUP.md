# 🚀 CI/CD Pipeline - Orçamento Application

## 📋 Visão Geral

Este documento explica como configurar e usar o pipeline CI/CD completo para a aplicação Orçamento.

## 🎯 O que foi implementado

### ✅ Pipelines Automáticos
- **Backend CI/CD**: Build, test e deploy automático do Spring Boot
- **Frontend CI/CD**: Build, test e deploy automático do Vue.js
- **Release Pipeline**: Versionamento e deploy de releases
- **Health Check**: Monitoramento automático da aplicação

### ✅ Funcionalidades
- 🧪 **Testes automáticos** em cada push
- 🐳 **Build de imagens Docker** automático
- 🚀 **Deploy automático** para Kubernetes
- 🏷️ **Versionamento automático** baseado em data/commit
- 📊 **Monitoramento** de saúde da aplicação
- 🔄 **Rollback** automático em caso de falha

## 🛠️ Configuração Inicial

### 1. Secrets do GitHub

Acesse: `Settings` → `Secrets and variables` → `Actions` e adicione:

```
DOCKER_PASSWORD=sua_senha_do_docker_hub
KUBE_CONFIG=base64_do_seu_kubeconfig
GITHUB_TOKEN=token_automatico_ja_existe
```

### 2. Configurar KUBE_CONFIG

```powershell
# No seu Windows, execute:
kubectl config view --raw | base64 -w 0
# Cole o resultado no secret KUBE_CONFIG
```

### 3. Verificar Docker Hub

Certifique-se que existe:
- `rbrangioni/orcamento-backend`
- `rbrangioni/orcamento-frontend`

## 🚀 Como Usar

### Deploy Automático (Recomendado)

```bash
# 1. Fazer alterações no código
git add .
git commit -m "feat: nova funcionalidade"
git push origin main

# 2. O pipeline executa automaticamente:
# ✅ Testes
# ✅ Build
# ✅ Docker
# ✅ Deploy
```

### Deploy Manual

```powershell
# Deploy completo
.\scripts\deploy.ps1 -Component all -Version latest

# Deploy apenas backend
.\scripts\deploy.ps1 -Component backend -Version 1.0.8

# Deploy apenas frontend
.\scripts\deploy.ps1 -Component frontend -Version latest

# Dry-run (testar sem executar)
.\scripts\deploy.ps1 -Component all -DryRun
```

### Criar Release

```bash
# Criar tag para release
git tag v1.0.8
git push origin v1.0.8

# Ou usar interface do GitHub:
# Actions → Release Pipeline → Run workflow
```

## 📊 Monitoramento

### Health Check Automático
- Executa a cada 15 minutos
- Verifica se pods estão rodando
- Alerta em caso de falha

### Verificação Manual
```powershell
# Status dos pods
kubectl get pods -n orcamento

# Logs do backend
kubectl logs -f deployment/orcamento-backend -n orcamento

# Logs do frontend
kubectl logs -f deployment/orcamento-frontend -n orcamento
```

## 🔄 Fluxo de Trabalho

### 1. Desenvolvimento
```
feature-branch → PR → main
```

### 2. Pipeline Automático
```
Push → Tests → Build → Docker → Deploy → ✅
```

### 3. Versionamento
```
main: v1.0.7 → v1.0.8 → v1.0.9
```

## 📁 Estrutura dos Workflows

```
.github/workflows/
├── backend-ci-cd.yml     # Pipeline do backend
├── frontend-ci-cd.yml    # Pipeline do frontend
├── release.yml           # Pipeline de release
└── health-check.yml      # Monitoramento
```

## 🐛 Troubleshooting

### Pipeline Falhou?

1. **Verifique os logs** no GitHub Actions
2. **Secrets configurados?** DOCKER_PASSWORD, KUBE_CONFIG
3. **Cluster acessível?** kubectl funciona localmente?
4. **Imagens existem?** Docker Hub tem os repositórios?

### Deploy Manual Falhou?

```powershell
# Verificar cluster
kubectl cluster-info

# Verificar namespace
kubectl get namespaces

# Verificar deployments
kubectl get deployments -n orcamento

# Rollback se necessário
kubectl rollout undo deployment/orcamento-backend -n orcamento
```

## 🎯 Próximos Passos

### Melhorias Sugeridas
- [ ] Testes de integração
- [ ] Análise de qualidade (SonarQube)
- [ ] Notificações Slack/Email
- [ ] Ambiente de staging
- [ ] Backup automático

### Comandos Úteis

```powershell
# Ver histórico de deployments
kubectl rollout history deployment/orcamento-backend -n orcamento

# Rollback para versão anterior
kubectl rollout undo deployment/orcamento-backend -n orcamento

# Escalar aplicação
kubectl scale deployment/orcamento-backend --replicas=3 -n orcamento

# Ver recursos
kubectl top pods -n orcamento
```

## 📞 Suporte

Em caso de dúvidas:
1. Verifique os logs do GitHub Actions
2. Execute deploy manual para debug
3. Verifique status do cluster Kubernetes

---

✅ **Pipeline CI/CD configurado com sucesso!**

🌐 **URLs da aplicação:**
- Frontend: http://orcamento.local
- Backend: http://api.orcamento.local
