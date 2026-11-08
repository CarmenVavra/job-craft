# JobCraft – Jede Bewerbung maßgeschneidert

JobCraft erstellt maßgeschneiderte Bewerbungsanschreiben: Briefkopf, Dokumente (Lebenslauf, Skill-Profil,
Zeugnisse) und weitere Angaben „Über mich“ (z. B. Privatprojekte) hinterlegen, Link zur Stellenanzeige einfügen, Analyse prüfen, KI-Entwurf im Chat verfeinern
(versioniert) und als druckfertiges PDF nach DIN 5008 exportieren.

Grundlagen: `Konzept.pdf`, `claude_code_spezifikation.md`, Design-Handoff unter `UI_Mockups/design_handoff_jobcraft/`.

## Schnellstart (Windows)

```bat
start.bat
```

Startet Backend (http://localhost:8090) und Frontend (http://localhost:4300) in eigenen Fenstern und öffnet den
Browser. Beenden mit `stop.bat` (beide nutzen `scripts/jobcraft.ps1` für sprachunabhängige Port-Prüfungen).

Voraussetzungen: **Java 21+** (getestet mit JDK 26), **Node.js 22+**. Maven wird über den Wrapper (`mvnw.cmd`)
geladen. Ohne weitere Konfiguration läuft alles lokal: eingebettete **PostgreSQL**-Datenbank in
`backend/data/postgres/` (ohne Installation oder Docker), Uploads in `backend/data/uploads/`, KI im **Demo-Modus**.

> Hinweis: Die Ports 8080/4200 waren auf dem Entwicklungsrechner belegt, deshalb nutzt JobCraft 8090/4300.
> Änderbar über `JOBCRAFT_PORT` bzw. `frontend/angular.json` + `frontend/proxy.conf.json`.

### Konfiguration

Lokale Einstellungen in `jobcraft-env.bat` (Vorlage: `jobcraft-env.example.bat`, wird nicht eingecheckt):

| Variable | Bedeutung | Standard |
|---|---|---|
| `ANTHROPIC_API_KEY` | Aktiviert Claude für Analyse, Entwurf und Chat | leer → Demo-Modus |
| `JOBCRAFT_AI_MODEL` | Modell-ID | `claude-opus-5-5` |
| `JOBCRAFT_JWT_SECRET` | HMAC-Schlüssel für JWTs (≥ 32 Zeichen) | Entwicklungswert – **in Produktion setzen** |
| `JOBCRAFT_DB_PORT` | Port der eingebetteten lokalen PostgreSQL (nur 127.0.0.1) | `54329` |
| `SPRING_PROFILES_ACTIVE=postgres` + `JOBCRAFT_DB_URL/USER/PASSWORD` | Externe PostgreSQL statt der eingebetteten | `dev` |
| `JOBCRAFT_STORAGE=s3` + `JOBCRAFT_S3_*` | S3/MinIO statt lokaler Ablage | `local` |
| `JOBCRAFT_SCRAPER_BROWSER` | Headless-Chromium für Stellenanzeigen | `true` |

**Demo-Modus:** Ohne API-Key extrahiert JobCraft die Eckdaten per Heuristik, baut den Entwurf aus Textbausteinen
und setzt typische Chat-Wünsche regelbasiert um (wie im Prototyp). Der Link „Beispiel-Link verwenden“ liefert eine
eingebaute Demo-Anzeige (Quantis Software GmbH). Im Editor steht dann „Demo“ neben Tonalität/Länge.

## Architektur

```
job-craft-app/
├── backend/   Spring Boot 3.5 (Java 21): REST, Security/JWT, JPA + Flyway, Playwright, Thymeleaf, PDFBox/POI
└── frontend/  Angular 21 (Standalone Components, Signals, zoneless), Angular CDK (Dialoge)
```

**Backend** (`com.jobcraft`) – strikt Controller → Service → Repository, DTOs als Records, zentrale
Fehlerbehandlung über `@RestControllerAdvice` (`ApiError`, keine Stacktraces).

| Bereich | Umsetzung |
|---|---|
| Auth | Registrierung/Login mit BCrypt, zustandslose JWTs (HS256), Passwort ändern |
| Profil | Briefkopf inkl. eigener Felder (JSON-Spalte), Vorlage Klassisch/Modern |
| Über mich | Weitere Angaben, die nicht im Lebenslauf stehen (Privatprojekte, Weiterbildungen …), je Angabe ein-/ausschaltbar; aktive Angaben fließen in Anforderungsabgleich und Anschreiben ein (`<weitere_angaben>` im Prompt) |
| Dokumente | Upload PDF/DOCX ≤ 10 MB (Signaturprüfung), Kategorie-Erkennung, asynchrone Textextraktion (PDFBox/POI), Seitenvorschau als PNG, Ablage lokal oder S3/MinIO |
| Analyse | Asynchron in 4 Schritten (Seite abrufen → bereinigen → Eckdaten → Profilabgleich), Fortschritt per Polling. Abruf mit Playwright (Chromium headless), Fallback JSoup; Schutz vor internen Adressen (SSRF) |
| KI | Offizielles Anthropic Java SDK mit strukturierten JSON-Ausgaben (`AnthropicCoverLetterAi`), sonst `TemplateCoverLetterAi` (Demo) |
| Anschreiben | Jede KI-Antwort erzeugt eine neue `CoverLetter`-Version; Chat-Verlauf mit Metazeile („v2 · Absatz 2 geändert“); Absätze und Anrede direkt im Editor bearbeitbar |
| PDF | Thymeleaf-Template (`templates/cover-letter.html`, DIN 5008) → Chromium-PDF, optional Lebenslauf/Zeugnisse (PDF) per PDFBox angehängt |
| Datenbank | Lokal eingebettetes PostgreSQL 17 (`EmbeddedPostgresConfig`, Profil `dev`), Tests mit H2 im Speicher; Flyway-Migrationen je Datenbank (`db/migration/postgresql` mit JSONB, `db/migration/h2`) |

**Frontend** – `core/` (Auth, Interceptor, Guards, API-Services/Signal-Stores, Hilfsfunktionen), `shared/` (Shell,
Briefbogen, Logo, Toast), `features/` (Login, Übersicht, Neue Bewerbung, Editor + PDF-Dialog, Dokumente + Vorschau,
Über mich, Briefkopf). Design-Tokens aus dem Handoff stehen in `src/styles.scss`.

### REST-API

| Methode & Pfad | Zweck |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | Konto anlegen / anmelden (JWT) |
| `GET /api/auth/me`, `PUT /api/auth/password` | Benutzer / Passwort ändern |
| `GET`/`PUT /api/profile` | Briefkopf-Stammdaten |
| `GET`/`POST /api/notes`, `PUT`/`DELETE /api/notes/{id}` | Weitere Angaben „Über mich“ |
| `GET`/`POST /api/documents` | Dokumente auflisten / hochladen (multipart `file`) |
| `GET /api/documents/{id}` · `/file` · `/preview` | Details mit Text · Originaldatei · erste Seite als PNG |
| `PATCH`/`DELETE /api/documents/{id}` | Kategorie ändern / löschen |
| `GET`/`POST /api/applications` | Bewerbungen / Stellen-URL analysieren (202, asynchron) |
| `GET /api/applications/{id}` | Zustand inkl. Analysefortschritt, Versionen, Chat, Briefdaten |
| `POST /api/applications/{id}/generate` | Geprüfte Eckdaten + Kontext → Entwurf |
| `POST /api/applications/{id}/chat` | Änderungswunsch → neue Version |
| `PUT /api/applications/{id}/salutation` | Eigene Anrede (`null` = automatisch aus der Ansprechperson) |
| `PATCH /api/applications/{id}/status`, `DELETE /api/applications/{id}` | Status (Entwurf/Versendet/Gespräch/Abgesagt) / löschen |
| `GET /api/applications/{id}/pdf?version&template&attachCv&attachCertificates` | PDF-Download |
| `GET /api/system` | Demo-Modus oder Sprachmodell |

## Entwicklung & Qualitätssicherung

```bat
cd backend
mvnw.cmd clean test
mvnw.cmd checkstyle:check
mvnw.cmd spring-boot:run

cd frontend
npm run build
npm run test -- --watch=false
npm run lint
npm run format
npm start
```

- Backend: JUnit 5 – `@WebMvcTest` (Controller), `@DataJpaTest` (Repositories), `@SpringBootTest` (komplette User
  Journey von der Registrierung bis zum PDF), Unit-Tests für Briefaufbau, Demo-KI, HTML-Bereinigung, Dokumente.
- Frontend: Jasmine/Karma (Chrome headless) für alle Services und Komponenten.

**Lokales Testkonto** (nur in der lokalen Datenbank vorhanden, mit Beispieldaten befüllt):
`lena.hoffmann@jobcraft.test` / `JobCraftDemo2026`.

## Datensicherung

- Die Daten liegen in einer lokalen **PostgreSQL**-Datenbank (`backend/data/postgres/`). PostgreSQL schreibt jede
  Änderung sofort sicher auf die Platte und übersteht Standby, Stromausfall und hartes Beenden.
- Bis November 2026 nutzte JobCraft eine H2-Datei (`backend/data/jobcraft.mv.db`). Sie fiel zweimal nach einem
  Standby still auf einen alten Stand zurück. Beim ersten Start mit PostgreSQL werden ihre Daten automatisch
  übernommen, danach heißt die Datei `jobcraft.mv.db.uebernommen-…`.
- **Am besten mit `stop.bat` beenden** (geordnetes Herunterfahren über `127.0.0.1:8091/actuator/shutdown`). Eine nach
  hartem Beenden weiterlaufende Datenbank beenden `stop.bat` und `start.bat` automatisch.
- **Automatische Backups** beim Start und stündlich in `backend/data/backups/jobcraft-JJJJMMTT-hhmmss.zip` (alle
  Tabellen, unabhängig von der PostgreSQL-Version lesbar); die letzten 48 bleiben erhalten (`jobcraft.backup.*`).
- **Wiederherstellen:** `stop.bat` ausführen, dann
  ```bat
  restore.bat backend\data\backups\jobcraft-JJJJMMTT-hhmmss.zip
  ```
  Ohne Argument listet `restore.bat` die vorhandenen Sicherungen. Der aktuelle Stand wird vorher automatisch als
  `vor-wiederherstellung-….zip` gesichert; die Wiederherstellung läuft in einer Transaktion (alles oder nichts).

## Abweichungen & offene Punkte

- **KI-Anbindung:** statt Spring AI das offizielle Anthropic Java SDK (das Konzept erlaubt „Spring AI oder direkte
  REST-Clients“). Strukturierte Ausgaben sind typisiert, Inhalte aus Anzeigen/Dokumenten werden als Daten markiert.
- **Datenbank lokal:** eingebettetes PostgreSQL (zonky `embedded-postgres`), damit die App ohne Installation/Docker
  startet; eine externe PostgreSQL per Profil `postgres`. Die Binärdateien (~25 MB) werden beim ersten Start nach
  `backend/data/postgres-bin/` entpackt.
- **Logo-Upload** (Briefkopf) ist wie im Mockup als Platzhalter sichtbar, aber noch ohne Funktion.
- **„Passwort vergessen“** benötigt einen E-Mail-Versand und ist noch nicht umgesetzt; angemeldet lässt sich das
  Passwort unter „Briefkopf“ ändern.
- Beim allerersten PDF-Export/Scraping lädt Playwright ggf. Chromium herunter (~150 MB). Der Browser wird beim
  Backend-Start im Hintergrund vorbereitet.
