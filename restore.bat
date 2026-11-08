@echo off
setlocal EnableExtensions
rem ============================================================
rem  JobCraft-Sicherung zurueckspielen und JobCraft starten.
rem  Aufruf:  restore.bat backend\data\backups\jobcraft-JJJJMMTT-hhmmss.zip
rem  Der aktuelle Stand wird vorher automatisch als
rem  backend\data\backups\vor-wiederherstellung-....zip gesichert.
rem ============================================================
title JobCraft Wiederherstellung
cd /d "%~dp0"

if "%~1"=="" (
  echo Aufruf: restore.bat ^<Sicherung.zip^>
  echo.
  echo Vorhandene Sicherungen ^(neueste zuletzt^):
  dir /b /o:d "%~dp0backend\data\backups\*.zip"
  exit /b 1
)
if not exist "%~f1" (
  echo [FEHLER] Datei nicht gefunden: %~f1
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\jobcraft.ps1" -Action IsListening -Port 8090 && (
  echo [HINWEIS] JobCraft laeuft noch. Bitte zuerst stop.bat ausfuehren.
  exit /b 1
)

echo Spiele %~nx1 zurueck ...
set "JOBCRAFT_RESTORE_FROM=%~f1"
call "%~dp0start.bat"
endlocal
