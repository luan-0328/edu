$ErrorActionPreference = 'Stop'

$healthUrl = 'http://localhost:8080/v3/api-docs'
$ready = $false
for ($attempt = 1; $attempt -le 60; $attempt++) {
    try {
        $response = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 3
        if ($response.StatusCode -eq 200) {
            $ready = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 3
    }
}
if (-not $ready) {
    throw "EduCore API did not become ready at $healthUrl. Check 'docker compose logs backend'."
}

$seedPath = Join-Path $PSScriptRoot 'init-demo.sql'
$utf8 = [System.Text.UTF8Encoding]::new($false)
function Invoke-MySqlScript([string] $Path) {
    $sql = [System.IO.File]::ReadAllText($Path, $utf8)
    $previousOutputEncoding = $OutputEncoding
    try {
        # Windows PowerShell otherwise sends native stdin using the active ANSI code page.
        $OutputEncoding = $utf8
        $sql | docker compose exec -T mysql sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" educore'
    } finally {
        $OutputEncoding = $previousOutputEncoding
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Loading SQL data failed: $Path"
    }
}

Invoke-MySqlScript (Join-Path $PSScriptRoot 'repair-demo-utf8.sql')
Invoke-MySqlScript $seedPath
Invoke-MySqlScript (Join-Path $PSScriptRoot 'extend-demo.sql')
Invoke-MySqlScript (Join-Path $PSScriptRoot 'simple-showcase-accounts.sql')
Write-Host 'Demo data loaded. Sign in with the demo accounts documented in README.md.'
