@echo off
rem Vorlage fuer lokale Einstellungen. Kopieren nach "jobcraft-env.bat" (wird nicht eingecheckt)
rem und Werte eintragen. start.bat laedt die Datei automatisch.

rem Claude-Anbindung (ohne Key laeuft JobCraft im Demo-Modus mit regelbasierten Texten)
rem set ANTHROPIC_API_KEY=sk-ant-...
rem set JOBCRAFT_AI_MODEL=claude-opus-5-5

rem Eigenes JWT-Geheimnis (mind. 32 Zeichen) - in Produktion zwingend setzen
rem set JOBCRAFT_JWT_SECRET=bitte-ein-langes-zufaelliges-geheimnis-eintragen

rem PostgreSQL statt H2 verwenden
rem set SPRING_PROFILES_ACTIVE=postgres
rem set JOBCRAFT_DB_URL=jdbc:postgresql://localhost:5432/jobcraft
rem set JOBCRAFT_DB_USER=jobcraft
rem set JOBCRAFT_DB_PASSWORD=jobcraft

rem S3 / MinIO statt lokaler Dateiablage
rem set JOBCRAFT_STORAGE=s3
rem set JOBCRAFT_S3_ENDPOINT=http://localhost:9000
rem set JOBCRAFT_S3_BUCKET=jobcraft-documents
rem set JOBCRAFT_S3_ACCESS_KEY=minioadmin
rem set JOBCRAFT_S3_SECRET_KEY=minioadmin
