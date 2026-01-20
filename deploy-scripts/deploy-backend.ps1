# Script PowerShell para deploy do orcamento-backend
param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("dev", "hml", "prod")]
    [string]$Environment,
    
    [Parameter(Mandatory=$false)]
    [string]$Version = "1.0.7",
    
    [Parameter(Mandatory=$false)]
    [switch]$DryRun = $false,
    
    [Parameter(Mandatory=$false)]
    [string]$Namespace = "orcamento"
)

# Configurações
$ChartPath = "C:\Projetos\orcamento\helm-charts\orcamento-backend"
$ValuesFile = "C:\Projetos\orcamento\helm-values\$Environment\orcamento-backend-$Environment.yaml"
$ReleaseName = "orcamento-backend"

# Verificar se os arquivos existem
if (-not (Test-Path $ChartPath)) {
    Write-Error "Chart não encontrado em: $ChartPath"
    exit 1
}

if (-not (Test-Path $ValuesFile)) {
    Write-Error "Arquivo de values não encontrado em: $ValuesFile"
    exit 1
}

Write-Host "🚀 Iniciando deploy do orcamento-backend" -ForegroundColor Green
Write-Host "   Ambiente: $Environment" -ForegroundColor Cyan
Write-Host "   Versão: $Version" -ForegroundColor Cyan
Write-Host "   Namespace: $Namespace" -ForegroundColor Cyan
Write-Host "   Values: $ValuesFile" -ForegroundColor Cyan

# Atualizar a versão no values file se especificada
if ($Version -ne "1.0.7") {
    Write-Host "📝 Atualizando versão para $Version..." -ForegroundColor Yellow
    $valuesContent = Get-Content $ValuesFile -Raw
    $valuesContent = $valuesContent -replace 'tag: ".*"', "tag: `"$Version`""
    Set-Content $ValuesFile -Value $valuesContent
}

# Executar helm upgrade
$helmCommand = @(
    "helm", "upgrade", "-i", 
    "-n", $Namespace,
    $ReleaseName, 
    $ChartPath,
    "-f", $ValuesFile
)

if ($DryRun) {
    Write-Host "🔍 Executando dry-run..." -ForegroundColor Yellow
    $helmCommand += "--dry-run"
}

Write-Host "💻 Executando: $($helmCommand -join ' ')" -ForegroundColor Gray

try {
    & $helmCommand[0] $helmCommand[1..($helmCommand.Length-1)]
    
    if ($LASTEXITCODE -eq 0) {
        if ($DryRun) {
            Write-Host "✅ Dry-run executado com sucesso!" -ForegroundColor Green
        } else {
            Write-Host "✅ Deploy executado com sucesso!" -ForegroundColor Green
            
            # Aguardar rollout
            Write-Host "⏳ Aguardando rollout..." -ForegroundColor Yellow
            kubectl rollout status deployment -n $Namespace $ReleaseName --timeout=300s
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "🎉 Deploy concluído com sucesso!" -ForegroundColor Green
                
                # Mostrar status dos pods
                Write-Host "`n📊 Status dos pods:" -ForegroundColor Cyan
                kubectl get pods -n $Namespace -l "app.kubernetes.io/name=orcamento-backend"
            } else {
                Write-Error "❌ Falha no rollout do deployment"
                exit 1
            }
        }
    } else {
        Write-Error "❌ Falha no comando helm"
        exit 1
    }
} catch {
    Write-Error "❌ Erro durante execução: $_"
    exit 1
}

Write-Host "`n🔗 Comandos úteis:" -ForegroundColor Cyan
Write-Host "   Ver logs: kubectl logs -n $Namespace deployment/$ReleaseName -f" -ForegroundColor Gray
Write-Host "   Ver status: kubectl get pods -n $Namespace -l app.kubernetes.io/name=orcamento-backend" -ForegroundColor Gray
Write-Host "   Rollback: helm rollback -n $Namespace $ReleaseName" -ForegroundColor Gray
