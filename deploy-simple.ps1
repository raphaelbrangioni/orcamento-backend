# Script PowerShell simplificado para deploy no namespace orcamento
param(
    [Parameter(Mandatory=$false)]
    [string]$Version = "1.0.6",
    
    [Parameter(Mandatory=$false)]
    [switch]$DryRun = $false
)

# Configurações
$ChartPath = "C:\Projetos\orcamento\helm-charts\orcamento-backend"
$ValuesFile = "C:\Projetos\orcamento\values-orcamento.yaml"
$ReleaseName = "orcamento-backend"
$Namespace = "orcamento"

# Verificar se os arquivos existem
if (-not (Test-Path $ChartPath)) {
    Write-Error "Chart não encontrado em: $ChartPath"
    exit 1
}

if (-not (Test-Path $ValuesFile)) {
    Write-Error "Arquivo de values não encontrado em: $ValuesFile"
    exit 1
}

Write-Host "🚀 Deploy do orcamento-backend" -ForegroundColor Green
Write-Host "   Versão: $Version" -ForegroundColor Cyan
Write-Host "   Namespace: $Namespace" -ForegroundColor Cyan

# Atualizar a versão se especificada
if ($Version -ne "1.0.6") {
    Write-Host "📝 Atualizando versão para $Version..." -ForegroundColor Yellow
    $valuesContent = Get-Content $ValuesFile -Raw
    $valuesContent = $valuesContent -replace 'tag: ".*"', "tag: `"$Version`""
    Set-Content $ValuesFile -Value $valuesContent
}

# Comando helm
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
            Write-Host "✅ Dry-run OK!" -ForegroundColor Green
        } else {
            Write-Host "✅ Deploy executado!" -ForegroundColor Green
            
            Write-Host "⏳ Aguardando rollout..." -ForegroundColor Yellow
            kubectl rollout status deployment -n $Namespace $ReleaseName --timeout=300s
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "🎉 Deploy concluído!" -ForegroundColor Green
                kubectl get pods -n $Namespace -l "app.kubernetes.io/name=orcamento-backend"
            }
        }
    }
} catch {
    Write-Error "❌ Erro: $_"
    exit 1
}

Write-Host "`n🔗 Comandos úteis:" -ForegroundColor Cyan
Write-Host "   Ver logs: kubectl logs -n $Namespace deployment/$ReleaseName -f" -ForegroundColor Gray
Write-Host "   Ver pods: kubectl get pods -n $Namespace" -ForegroundColor Gray
