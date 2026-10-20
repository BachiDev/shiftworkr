#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Runs the same IntelliJ inspections as the Qodana CI job, locally.
.DESCRIPTION
  Downloads the pinned Qodana CLI once (into %LOCALAPPDATA%\QodanaCli) and runs
  the JVM Community linter in native mode, so no Docker image pull is needed.
  Results (SARIF + HTML report) land in .qodana/.
  Exits non-zero when Qodana finds more problems than -FailThreshold.
#>
param(
    [int]$FailThreshold = 0
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$results = Join-Path $root '.qodana'
$cliDir = Join-Path $env:LOCALAPPDATA 'QodanaCli'
$cli = Join-Path $cliDir 'qodana.exe'
$cliVersion = '2026.2.2'

if (-not (Test-Path $cli)) {
    New-Item -ItemType Directory -Force -Path $cliDir | Out-Null
    Write-Host "Downloading Qodana CLI $cliVersion..."
    Invoke-WebRequest `
        -Uri "https://github.com/JetBrains/qodana-cli/releases/download/v$cliVersion/qodana_windows_x86_64.exe" `
        -OutFile $cli
}

New-Item -ItemType Directory -Force -Path $results | Out-Null

& $cli scan `
    --within-docker false `
    --linter qodana-jvm-community `
    --project-dir $root `
    --results-dir $results `
    --fail-threshold $FailThreshold `
    --print-problems

exit $LASTEXITCODE
