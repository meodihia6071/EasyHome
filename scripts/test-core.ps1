$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$temp = Join-Path ([System.IO.Path]::GetTempPath()) ('easyhome-rules-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force $temp | Out-Null
try {
    & javac -Xlint:all -Werror -d $temp (Join-Path $root 'backend/src/main/java/com/easyhome/Rules.java') (Join-Path $root 'verification/RulesChecks.java')
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
    & java -cp $temp com.easyhome.RulesChecks
    if ($LASTEXITCODE -ne 0) { throw 'Business rule checks failed' }
} finally {
    Remove-Item -LiteralPath $temp -Recurse -Force -ErrorAction SilentlyContinue
}
