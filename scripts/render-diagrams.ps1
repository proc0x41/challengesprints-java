param([string]$BrowserPath, [string]$Python = 'python')
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & $Python scripts/split_class_diagrams.py
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao dividir diagramas de classes.' }
    $browserArguments = @()
    $browserConfig = Join-Path ([IO.Path]::GetTempPath()) ('alpha4-puppeteer-' + [Guid]::NewGuid() + '.json')
    if ($BrowserPath) {
        $json = @{ executablePath = $BrowserPath; headless = $true } | ConvertTo-Json
        [IO.File]::WriteAllText($browserConfig, $json, (New-Object Text.UTF8Encoding($false)))
        $browserArguments = @('-p', $browserConfig)
    }
    $sources = @((Get-Item docs/mer.mmd)) + @(Get-ChildItem docs/diagrams/*.mmd)
    foreach ($source in $sources) {
        & npx.cmd --yes --package @mermaid-js/mermaid-cli@12.2.0 mmdc -i $source.FullName -o ($source.FullName -replace '\.mmd$', '.svg') -c docs/mermaid-config.json -b white @browserArguments
        if ($LASTEXITCODE -ne 0) { throw "Falha ao renderizar $($source.Name)." }
    }
    & npm.cmd ci --prefix scripts
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao instalar renderizador de documentação.' }
    & node scripts/rasterize-diagrams.mjs $BrowserPath
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao rasterizar diagramas.' }
} finally {
    if ($browserConfig -and (Test-Path -LiteralPath $browserConfig)) { Remove-Item -LiteralPath $browserConfig }
    Pop-Location
}
