<#
  Hilfsfunktionen fuer start.bat / stop.bat (sprachunabhaengig, ohne netstat-Parsing).

  Aufrufe:
    jobcraft.ps1 -Action IsListening -Port 8090     # Exit 0, wenn der Port belegt ist
    jobcraft.ps1 -Action WaitFor -Port 8090 -TimeoutSeconds 300
    jobcraft.ps1 -Action Stop
#>
param(
    [Parameter(Mandatory = $true)][ValidateSet('IsListening', 'WaitFor', 'Stop')][string]$Action,
    [int]$Port = 0,
    [int]$TimeoutSeconds = 300
)

$ErrorActionPreference = 'SilentlyContinue'
$projectDir = Split-Path -Parent $PSScriptRoot

function Test-Listening([int]$p) {
    return [bool](Get-NetTCPConnection -State Listen -LocalPort $p)
}

function Stop-Tree([int]$processId) {
    if ($processId -gt 0) {
        & taskkill.exe /PID $processId /T /F *> $null
    }
}

switch ($Action) {
    'IsListening' {
        if (Test-Listening $Port) { exit 0 } else { exit 1 }
    }
    'WaitFor' {
        $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
        while ((Get-Date) -lt $deadline) {
            if (Test-Listening $Port) { exit 0 }
            Start-Sleep -Seconds 2
        }
        exit 1
    }
    'Stop' {
        foreach ($p in 8090, 4300) {
            $listeners = Get-NetTCPConnection -State Listen -LocalPort $p
            if ($listeners) {
                $listeners | ForEach-Object { Stop-Tree $_.OwningProcess }
                Write-Host "Port $p gestoppt."
            } else {
                Write-Host "Port $p war nicht belegt."
            }
        }
        # Von start.bat geoeffnete Konsolenfenster (inkl. Maven-/npm-Prozessbaum) schliessen
        Get-CimInstance Win32_Process -Filter "Name='cmd.exe'" |
            Where-Object {
                $_.CommandLine -like "*$projectDir\backend\mvnw.cmd*spring-boot:run*" -or
                ($_.CommandLine -like '*npm*' -and $_.CommandLine -like "*$projectDir\frontend*")
            } |
            ForEach-Object { Stop-Tree $_.ProcessId }
        exit 0
    }
}
