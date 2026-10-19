@echo off
setlocal EnableExtensions
rem ============================================================
rem  JobCraft starten: Backend (Spring Boot, Port 8090) und
rem  Frontend (Angular, Port 4300) in eigenen Fenstern.
rem ============================================================
title JobCraft Start
cd /d "%~dp0"
set "JC_PS=powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\jobcraft.ps1""

rem Optionale lokale Einstellungen (z. B. ANTHROPIC_API_KEY), siehe jobcraft-env.example.bat
if exist "%~dp0jobcraft-env.bat" call "%~dp0jobcraft-env.bat"

where java >nul 2>nul || (echo [FEHLER] Java 21+ wurde nicht gefunden. & pause & exit /b 1)
where npm  >nul 2>nul || (echo [FEHLER] Node.js/npm wurde nicht gefunden. & pause & exit /b 1)

%JC_PS% -Action IsListening -Port 8090 && (
  echo [HINWEIS] Port 8090 ist bereits belegt - laeuft JobCraft schon? Ggf. zuerst stop.bat ausfuehren.
  pause
  exit /b 1
)

if not exist "%~dp0frontend\node_modules" (
  echo Installiere Frontend-Abhaengigkeiten ^(einmalig^) ...
  pushd "%~dp0frontend"
  call npm install || (popd & echo [FEHLER] npm install fehlgeschlagen. & pause & exit /b 1)
  popd
)

echo Starte Backend auf http://localhost:8090 ...
start "JobCraft Backend" /D "%~dp0backend" cmd /k call "%~dp0backend\mvnw.cmd" spring-boot:run

rem Frontend erst starten, wenn das Backend lauscht - sonst laufen API-Aufrufe ins Leere (ECONNREFUSED).
echo Warte auf das Backend (beim ersten Start werden Abhaengigkeiten geladen) ...
%JC_PS% -Action WaitFor -Port 8090 -TimeoutSeconds 300 || echo [WARNUNG] Backend antwortet noch nicht - bitte das Fenster "JobCraft Backend" pruefen.

echo Starte Frontend auf http://localhost:4300 ...
start "JobCraft Frontend" /D "%~dp0frontend" cmd /k call npm --prefix "%~dp0frontend" start

echo Warte auf das Frontend ...
%JC_PS% -Action WaitFor -Port 4300 -TimeoutSeconds 180 || echo [WARNUNG] Frontend antwortet noch nicht - bitte das Fenster "JobCraft Frontend" pruefen.

echo JobCraft laeuft: http://localhost:4300
start "" "http://localhost:4300"
endlocal
exit /b 0
