# EasyHome local XAMPP database bootstrap. Non-destructive: never DROP or DELETE.
$ErrorActionPreference = 'Stop'
$url = if ($env:DB_URL) { $env:DB_URL } else { 'jdbc:mariadb://127.0.0.1:3306/easyhome' }
$pattern = '^jdbc:mariadb://(?<server>127\.0\.0\.1|localhost):(?<port>\d{1,5})/(?<database>[A-Za-z][A-Za-z0-9_]*)(\?.*)?$'
if ($url -notmatch $pattern) {
    Write-Host '[ERROR] init-local-db only supports local MariaDB (127.0.0.1 or localhost).' -ForegroundColor Red
    Write-Host ('DB_URL is: ' + $url)
    exit 2
}
$server = $Matches['server']
$port = [int]$Matches['port']
$dbName = $Matches['database']
if ($port -lt 1 -or $port -gt 65535) {
    Write-Host '[ERROR] MariaDB port must be between 1 and 65535.' -ForegroundColor Red
    exit 2
}
$dbUser = if ($env:DB_USER) { $env:DB_USER } else { 'root' }
$mysqlCandidates = @()
foreach ($var in @('XAMPP_HOME','XAMPP_DIR')) {
    $val = [Environment]::GetEnvironmentVariable($var)
    if ($val) {
        $mysqlCandidates += (Join-Path $val 'mysql\bin\mysql.exe')
        $mysqlCandidates += (Join-Path $val 'mysql\bin\mariadb.exe')
    }
}
$mysqlCandidates += 'C:\xampp\mysql\bin\mysql.exe'
$mysqlCandidates += 'D:\xampp\mysql\bin\mysql.exe'
$mysqlCandidates += 'E:\xampp\mysql\bin\mysql.exe'
$command = Get-Command 'mysql.exe' -ErrorAction SilentlyContinue
if ($command) { $mysqlCandidates += $command.Source }
$command = Get-Command 'mariadb.exe' -ErrorAction SilentlyContinue
if ($command) { $mysqlCandidates += $command.Source }
$mysql = $mysqlCandidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
if (-not $mysql) {
    Write-Host '[ERROR] MySQL CLI not found. Set XAMPP_HOME or install XAMPP at C:\xampp.' -ForegroundColor Red
    exit 2
}
# DB name is strictly validated above, so SQL cannot be injected.
$sql = 'CREATE DATABASE IF NOT EXISTS ' + $dbName + ' CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
$previousPwd = $env:MYSQL_PWD
try {
    if ($null -ne $env:DB_PASSWORD) { $env:MYSQL_PWD = $env:DB_PASSWORD }
    & $mysql '--protocol=TCP' "--host=$server" "--port=$port" "--user=$dbUser" '--default-character-set=utf8mb4' '--execute' $sql
    if ($LASTEXITCODE -ne 0) {
        Write-Host '[ERROR] Could not create/check the database. Start MySQL in XAMPP, check port and DB credentials.' -ForegroundColor Red
        exit 1
    }
} finally {
    $env:MYSQL_PWD = $previousPwd
}
Write-Host ("[OK] Database '$dbName' exists on $server`:$port. Existing data was not dropped.") -ForegroundColor Green
Write-Host '[INFO] Flyway and Hibernate update the tables when Spring Boot starts. Back up existing databases first.'
