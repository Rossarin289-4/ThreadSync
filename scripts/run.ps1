# Windows PowerShell runner: compile เป็น Java 17 bytecode แล้วเปิด ThreadSync
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
New-Item -ItemType Directory -Force -Path out | Out-Null
javac --release 17 -d out src\threadsync\Main.java
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Opening ThreadSync at http://localhost:8080"
Start-Process "http://localhost:8080"
java -cp out threadsync.Main
