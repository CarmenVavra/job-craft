@echo off
setlocal EnableExtensions
rem ============================================================
rem  JobCraft beenden: stoppt Backend (Port 8090) und Frontend
rem  (Port 4300) inklusive der von start.bat geoeffneten Fenster.
rem ============================================================
title JobCraft Stop

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\jobcraft.ps1" -Action Stop

echo JobCraft wurde beendet.
endlocal
exit /b 0
