param(
    [ValidateSet("smoke", "load", "volume")]
    [string]$Profile = "smoke",
    [ValidateRange(1, 100000)]
    [int]$Rate = 100,
    [ValidatePattern('^\d+[smh]$')]
    [string]$Duration = "2m",
    [ValidateRange(1, 10000000)]
    [int]$TotalVotes = 200000,
    [ValidateRange(1, 10000)]
    [int]$VUs = 100,
    [ValidateRange(1, 10000)]
    [int]$PreAllocatedVUs = 100
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$runId = "$timestamp-$Profile"
$resultDirectory = Join-Path $repoRoot "performance-results\$runId"
$composeFile = Join-Path $repoRoot "compose.performance.yaml"

New-Item -ItemType Directory -Force -Path $resultDirectory | Out-Null

$env:PERF_PROFILE = $Profile
$env:PERF_RUN_ID = $runId
$env:PERF_RATE = $Rate
$env:PERF_DURATION = $Duration
$env:PERF_TOTAL_VOTES = $TotalVotes
$env:PERF_VUS = $VUs
$env:PERF_PREALLOCATED_VUS = $PreAllocatedVUs

$commit = (git -C $repoRoot rev-parse --short HEAD 2>$null)
$dirty = [bool](git -C $repoRoot status --porcelain 2>$null)
$metadata = [ordered]@{
    runId = $runId
    executedAt = (Get-Date).ToString("o")
    profile = $Profile
    gitCommit = $commit
    workingTreeDirty = $dirty
    parameters = [ordered]@{
        ratePerSecond = $Rate
        duration = $Duration
        totalVotes = $TotalVotes
        vus = $VUs
        preAllocatedVUs = $PreAllocatedVUs
    }
    docker = (docker version --format '{{.Server.Version}}' 2>$null)
    compose = (docker compose version --short 2>$null)
}
$metadata | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $resultDirectory "metadata.json")

Write-Host "Execucao: $runId"
Write-Host "Dashboard durante o teste: http://localhost:5665"
Write-Host "Resultados: $resultDirectory"

$exitCode = 1
try {
    docker compose -f $composeFile down -v --remove-orphans | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "Falha ao limpar o ambiente de performance." }
    docker compose -f $composeFile build perf-app | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "Falha ao construir a aplicacao de performance." }
    docker compose -f $composeFile up --no-build -d perf-app | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "Falha ao iniciar a aplicacao de performance." }
    # O Windows PowerShell 5 transforma stderr de processos nativos em
    # NativeCommandError. O cmd combina stdout/stderr antes de devolver a saída,
    # preservando o console ao vivo e o código real do processo sem falso erro.
    $dockerCommand = 'docker compose -f "{0}" run --rm --service-ports k6 2>&1' -f `
        $composeFile.Replace('"', '""')
    & $env:ComSpec /d /s /c $dockerCommand |
        Tee-Object -FilePath (Join-Path $resultDirectory "console.log") | Out-Host
    $exitCode = $LASTEXITCODE
}
finally {
    docker compose -f $composeFile down -v --remove-orphans | Out-Host
}

if ($exitCode -ne 0) {
    throw "O perfil $Profile falhou (codigo $exitCode). Consulte $resultDirectory."
}

Write-Host "Teste aprovado. Abra: $(Join-Path $resultDirectory 'report.html')"
