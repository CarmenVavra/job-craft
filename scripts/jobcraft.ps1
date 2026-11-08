<#
  Hilfsfunktionen fuer start.bat / stop.bat (sprachunabhaengig, ohne netstat-Parsing).

  Aufrufe:
    jobcraft.ps1 -Action IsListening -Port 8090     # Exit 0, wenn der Port belegt ist
    jobcraft.ps1 -Action WaitFor -Port 8090 -TimeoutSeconds 300
    jobcraft.ps1 -Action Stop
    jobcraft.ps1 -Action StopDb                      # nur eine verwaiste lokale PostgreSQL-Instanz beenden
#>
param(
    [Parameter(Mandatory = $true)][ValidateSet('IsListening', 'WaitFor', 'Stop', 'StopDb')][string]$Action,
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

# Beendet eine noch laufende eingebettete PostgreSQL-Instanz von JobCraft (z. B. nach hartem Beenden des Backends):
# zuerst geordnet per pg_ctl, nur im Notfall hart. PostgreSQL ist auch nach hartem Beenden konsistent (WAL).
function Get-EmbeddedPostgres {
    # Erkennung über den Programmpfad (PostgreSQL selbst schreibt Pfade mit '/').
    $binDir = Join-Path $projectDir 'backend\data\postgres-bin'
    @(Get-CimInstance Win32_Process -Filter "Name='postgres.exe'" |
        Where-Object { $_.ExecutablePath -and $_.ExecutablePath.Replace('/', '\') -like "$binDir\*" })
}

function Stop-EmbeddedPostgres {
    $procs = Get-EmbeddedPostgres
    if ($procs.Count -eq 0) { return }
    $dataDir = Join-Path $projectDir 'backend\data\postgres'
    $pgCtl = Join-Path (Split-Path -Parent $procs[0].ExecutablePath) 'pg_ctl.exe'
    if (Test-Path $pgCtl) {
        & $pgCtl stop -D $dataDir -m fast -w -t 30 *> $null
    }
    Get-EmbeddedPostgres | ForEach-Object { Stop-Tree $_.ProcessId }
    Write-Host 'Lokale Datenbank beendet.'
}

switch ($Action) {
    'IsListening' {
        if (Test-Listening $Port) {
            # Zeigt, wer den Port belegt (z. B. ein Server eines anderen Projekts).
            $listener = Get-NetTCPConnection -State Listen -LocalPort $Port | Select-Object -First 1
            $proc = if ($listener) { Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)" }
            if ($proc) { Write-Host ('Port {0} belegt durch PID {1}: {2}' -f $Port, $proc.ProcessId, $proc.CommandLine) }
            exit 0
        } else { exit 1 }
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
        # 1. Backend geordnet herunterfahren, damit die Datenbank sauber geschlossen wird.
        if (Test-Listening 8090) {
            try {
                Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8091/actuator/shutdown' `
                    -ContentType 'application/json' -Body '{}' -TimeoutSec 10 | Out-Null
                $deadline = (Get-Date).AddSeconds(30)
                while ((Test-Listening 8090) -and (Get-Date) -lt $deadline) { Start-Sleep -Milliseconds 500 }
                if (-not (Test-Listening 8090)) { Write-Host 'Backend geordnet beendet.' }
            } catch {
                Write-Host 'Geordnetes Beenden nicht moeglich - Backend wird hart beendet.'
            }
        }
        # 2. Was noch laeuft (Frontend, haengendes Backend), hart beenden.
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
        Stop-EmbeddedPostgres
        exit 0
    }
    'StopDb' {
        Stop-EmbeddedPostgres
        exit 0
    }
}
