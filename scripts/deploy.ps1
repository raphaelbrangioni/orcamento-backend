# 🚀 Deploy Script - Orçamento Application
# Uso: .\scripts\deploy.ps1 -Component backend|frontend|all -Version 1.0.8

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("backend", "frontend", "all")]
    [string]$Component,
    
    [Parameter(Mandatory=$false)]
    [string]$Version = "latest",
    
    [Parameter(Mandatory=$false)]
    [string]$Namespace = "orcamento",
    
    [Parameter(Mandatory=$false)]
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

# Cores para output
function Write-Success { param($Message) Write-Host "✅ $Message" -ForegroundColor Green }
function Write-Info { param($Message) Write-Host "ℹ️  $Message" -ForegroundColor Cyan }
function Write-Warning { param($Message) Write-Host "⚠️  $Message" -ForegroundColor Yellow }
function Write-Error { param($Message) Write-Host "❌ $Message" -ForegroundColor Red }

Write-Info "🚀 Iniciando deploy do Orçamento Application"
Write-Info "📦 Componente: $Component"
Write-Info "🏷️  Versão: $Version"
Write-Info "🎯 Namespace: $Namespace"

if ($DryRun) {
    Write-Warning "🧪 Modo DRY-RUN ativado - nenhuma alteração será feita"
}

# Verificar se kubectl está disponível
try {
    kubectl version --client | Out-Null
    Write-Success "kubectl encontrado"
} catch {
    Write-Error "kubectl não encontrado. Instale o kubectl primeiro."
    exit 1
}

# Verificar se helm está disponível (para backend)
if ($Component -eq "backend" -or $Component -eq "all") {
    try {
        helm version --short | Out-Null
        Write-Success "helm encontrado"
    } catch {
        Write-Error "helm não encontrado. Instale o helm primeiro."
        exit 1
    }
}

# Função para deploy do backend
function Deploy-Backend {
    Write-Info "🏗️  Fazendo deploy do backend..."
    
    $helmCommand = @(
        "helm", "upgrade", "--install", "orcamento-backend"
        "./helm-charts/orcamento-backend"
        "--namespace", $Namespace
        "--values", "values-orcamento.yaml"
        "--wait", "--timeout=300s"
    )
    
    if ($Version -ne "latest") {
        $helmCommand += "--set", "image.tag=$Version"
    }
    
    if ($DryRun) {
        $helmCommand += "--dry-run"
    }
    
    Write-Info "Executando: $($helmCommand -join ' ')"
    
    try {
        & $helmCommand[0] $helmCommand[1..($helmCommand.Length-1)]
        Write-Success "Backend deployado com sucesso!"
    } catch {
        Write-Error "Falha no deploy do backend: $_"
        throw
    }
}

# Função para deploy do frontend
function Deploy-Frontend {
    Write-Info "🎨 Fazendo deploy do frontend..."
    
    $imageName = "rbrangioni/orcamento-frontend:$Version"
    
    if ($DryRun) {
        Write-Info "DRY-RUN: kubectl set image deployment/orcamento-frontend orcamento-frontend=$imageName -n $Namespace"
        Write-Info "DRY-RUN: kubectl rollout status deployment/orcamento-frontend -n $Namespace"
    } else {
        try {
            kubectl set image deployment/orcamento-frontend orcamento-frontend=$imageName -n $Namespace
            kubectl rollout status deployment/orcamento-frontend -n $Namespace --timeout=300s
            Write-Success "Frontend deployado com sucesso!"
        } catch {
            Write-Error "Falha no deploy do frontend: $_"
            throw
        }
    }
}

# Função para verificar status
function Check-Status {
    Write-Info "🔍 Verificando status dos deployments..."
    
    try {
        Write-Info "📊 Pods:"
        kubectl get pods -n $Namespace
        
        Write-Info "📊 Services:"
        kubectl get services -n $Namespace
        
        Write-Info "📊 Ingress:"
        kubectl get ingress -n $Namespace
        
        Write-Success "Status verificado com sucesso!"
    } catch {
        Write-Warning "Erro ao verificar status: $_"
    }
}

# Executar deploy baseado no componente
try {
    switch ($Component) {
        "backend" {
            Deploy-Backend
        }
        "frontend" {
            Deploy-Frontend
        }
        "all" {
            Deploy-Backend
            Deploy-Frontend
        }
    }
    
    if (-not $DryRun) {
        Start-Sleep -Seconds 5
        Check-Status
        
        Write-Success "🎉 Deploy concluído com sucesso!"
        Write-Info "🌐 Frontend: http://orcamento.local"
        Write-Info "🌐 Backend: http://api.orcamento.local"
    }
    
} catch {
    Write-Error "❌ Deploy falhou: $_"
    exit 1
}
