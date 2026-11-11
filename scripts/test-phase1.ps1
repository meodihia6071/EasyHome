# EasyHome V2 Phase 1 — run full local unit/integration + frontend build checks on Windows.
# Tests use H2 in-memory. Does not delete MariaDB/XAMPP data.
$ErrorActionPreference = 'Stop'
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
function Run-Step([string]$title, [scriptblock]$cmd) {
  Write-Host "`n=== $title ===" -ForegroundColor Cyan
  & $cmd
  if ($LASTEXITCODE -ne 0) { throw "$title FAILED with exit code $LASTEXITCODE" }
}
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Maven (mvn) not found in PATH.' }
if (-not (Get-Command npm.cmd -ErrorAction SilentlyContinue)) { throw 'npm.cmd not found in PATH. Install Node.js.' }
Push-Location (Join-Path $project 'backend')
try {
  Run-Step 'Backend Maven tests (H2 profile)' { mvn clean test }
} finally { Pop-Location }
Push-Location (Join-Path $project 'frontend')
try {
  Run-Step 'Install frontend dependencies' { npm.cmd install }
  Run-Step 'Build frontend' { npm.cmd run build }
} finally { Pop-Location }
Write-Host "`nPASS: Maven tests + frontend build. MariaDB, browser E2E and Gmail SMTP still need separate real-world testing." -ForegroundColor Green
