# PROJEKT-SPEZIFIKATION: ApplyFlow (KI-Bewerbungsassistent)

Dieses Dokument dient als technische und architektonische Spezifikation für die Entwicklung der Web-App **ApplyFlow** durch Claude Code.

---

## 1. Tech Stack & Architektur
* **Backend:** Java 21, Spring Boot 3.x, Spring Security (JWT), Spring AI, Spring Data JPA, Flyway (DB-Migrationen), Playwright for Java (Scraping & PDF-Rendering), Thymeleaf (PDF-Templates).
* **Frontend:** Angular (aktuelle Version, Standalone Components, Signals), Angular Material oder PrimeNG, RxJS.
* **Datenbank:** PostgreSQL (inkl. JSONB Support).
* **File Storage:** S3-kompatibler Object Storage (MinIO / AWS S3) für hochgeladene Dokumente.

---

## 2. Projekt-Struktur (Monorepo)

```text
applyflow/
├── backend/                  # Spring Boot Maven Projekt
│   ├── src/main/java/com/applyflow/
│   │   ├── config/           # Security, Async, AI Config
│   │   ├── controller/       # REST Endpoints
│   │   ├── model/            # JPA Entities
│   │   ├── repository/       # Spring Data Repositories
│   │   ├── service/          # Business Logic (Scraper, PDF, AI, Document)
│   │   └── dto/              # Data Transfer Objects
│   ├── src/main/resources/
│   │   ├── db/migration/     # Flyway SQL Scripts
│   │   └── templates/        # Thymeleaf HTML Templates für PDFs
│   └── pom.xml
│
└── frontend/                 # Angular Projekt
    ├── src/app/
    │   ├── core/             # Interceptors, Guards, Auth Service
    │   ├── features/         # Feature Modules (Dashboard, Profile, Application, Chat)
    │   ├── shared/           # UI Components, Layouts
    │   └── app.routes.ts
    └── package.json
```

---

## 3. Datenbank-Schema (PostgreSQL / Flyway)

Führe folgende SQL-Migration (`V1__init_schema.sql`) im Backend aus:

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    address_line1 VARCHAR(255),
    zip_code VARCHAR(20),
    city VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(255),
    custom_fields JSONB DEFAULT '{}'::jsonb,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL, -- CV, SKILLS
    storage_path VARCHAR(512) NOT NULL,
    extracted_text TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_url VARCHAR(2048) NOT NULL,
    job_title VARCHAR(255),
    company_name VARCHAR(255),
    raw_job_text TEXT,
    recipient_data JSONB, 
    current_cover_letter JSONB,
    status VARCHAR(50) DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL, -- USER, ASSISTANT, SYSTEM
    message TEXT NOT NULL,
    resulting_cover_letter JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## 4. Kern-Features & Implementierungsanweisungen

### A. Web-Scraper Service (`JobScraperService.java`)
1. Nutzen von **Playwright for Java**, um Links/URLs im Headless-Modus zu laden (inkl. SPA JavaScript-Execution).
2. Nutze **JSoup**, um Skripte, Styles, Navigationen und Footer aus dem HTML-String zu entfernen und reinen Fließtext der Stellenanzeige zu extrahieren.
3. Integriere **Spring AI**, um den Text via LLM in strukturierte JSON-Metadaten zu überführen (Unternehmen, Ansprechpartner, Kernanforderungen).

### B. PDF-Generierungs Service (`PdfGenerationService.java`)
1. Verwende **Thymeleaf**, um ein HTML-Dokument (`cover-letter-template.html`) mit den Stammdaten des Nutzers, dem Empfänger und den Absätzen des Anschreibens zu befüllen.
2. Nutze **Playwright (Chromium Headless)**, um das HTML-Template pixelgenau im DIN 5008 Format als PDF-Byte-Stream zu rendern.

### C. REST API Endpunkte
* `POST /api/auth/register` & `POST /api/auth/login` (JWT Authentication)
* `GET /PUT /api/profile` (Stammdaten & dynamische JSONB-Felder für den Briefkopf)
* `POST /api/documents` (Multipart File Upload, Textextraktion via Apache PDFBox/Tika, Speicherung in S3/MinIO)
* `DELETE /api/documents/{id}` (Löschen hochgeladener Dokumente)
* `POST /api/applications` (Empfang der Job-URL -> Trigger Scraping & initialen KI-Anschreiben-Entwurf)
* `POST /api/applications/{id}/chat` (Chat-Nachricht an KI senden, Anschreiben iterativ anpassen, JSON-State speichern)
* `GET /api/applications/{id}/pdf` (Generierung & Download des finalen DIN 5008 PDFs)

### D. Angular Frontend Struktur
* **Routing & Layout:** Auth Guard, Sidebar-Layout mit Navigation (Dashboard, Dokumente, Neue Bewerbung, Profileinstellungen).
* **State Management:** Nutzung von Angular Signals (`signal`, `computed`) für reaktive Datenströme im Chat und bei der Dokumentenverwaltung.
* **Split-Screen Ansicht:** Links eine Chat-Oberfläche zur Interaktion mit der KI, rechts eine Live-Vorschau des Anschreibens.

---

## 5. Coding Guidelines für Claude Code
1. Schreibe sauberen, gut dokumentierten Code (JavaDoc für Services, TypeScript-Typisierung im Frontend).
2. Verwende Lombok im Backend für DTOs und Entities.
3. Fange Exceptions in Services sauber ab und liefere aussagekräftige HTTP-Statuscodes (`ResponseEntity`).
4. Verwende für asynchrone Prozesse im Backend `@Async` wo sinnvoll.