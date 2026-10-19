# Handoff: JobCraft Web-App (Angular + Spring Boot)

## Overview
JobCraft erstellt maßgeschneiderte Bewerbungsanschreiben: Nutzer pflegen Briefkopf-Stammdaten und Dokumente (Lebenslauf, Skill-Profil, Zeugnisse), fügen den Link zu einer Stellenanzeige ein, lassen sie analysieren, erhalten einen KI-Entwurf, verfeinern ihn im Chat (versioniert) und exportieren ein druckfertiges PDF. Fachkonzept: `Konzept.pdf` (Architektur, Datenmodell, API-Endpunkte).

## About the Design Files
Die Dateien in `prototype/` sind **Design-Referenzen in HTML** – ein klickbarer Prototyp, der Aussehen und Verhalten zeigt, kein Produktionscode. Aufgabe ist, diese Screens im Ziel-Stack nachzubauen: **Angular (Standalone Components, Signals)** mit Angular Material oder PrimeNG, Backend **Spring Boot** gemäß `Konzept.pdf`. `prototype/JobCraft.dc.html` im Browser öffnen (mit `support.js` im selben Ordner). Alle Styles sind inline; die Logik steht in der Klasse `Component` am Dateiende (Mock-Daten, Zustände, Chat-Regeln).

## Fidelity
**High-fidelity.** Farben, Typografie, Abstände und Interaktionen sind final gemeint. UI-Library-Komponenten (Inputs, Dialoge, Toggles) entsprechend themen, sodass sie den Mockups entsprechen.

## Screens / Views
Screenshots in `screenshots/` (aufgenommen bei ~1300 px Breite).

### 1. Login / Registrierung (`01-login.png`)
- Split-Layout, `flex-wrap`. Links (flex 1 1 440px) dunkles Panel `#2F2F28`, Padding 44/56 px: Logo; Headline „Jede Bewerbung maßgeschneidert.“ 46px/600, letter-spacing −0.025em, weiß; Subtext 16px/1.6 `#C9C8D6`; 4-Spalten-Schritte (Nummer 12px Mono `#31A2AC`, Label 13px `#E4E3EF`), Trennlinie oben `#4A4A42`.
- Rechts: Formular max. 380px zentriert. Segmented Tabs „Anmelden / Registrieren“ (Track `#E4E2F6`, r=10, aktiver Tab weiß + Shadow `0 1px 3px rgba(47,47,40,.12)`). Titel 26px/600 („Willkommen zurück“ / „Konto erstellen“). Felder: (Name nur bei Registrierung), E-Mail, Passwort mit Link „Vergessen?“. Primärbutton.
- Validierung: E-Mail-Regex, Passwort ≥ 8 Zeichen, Name Pflicht bei Registrierung. Fehlerbox: bg `#F8E4E4`, Text `#AF1C1C`, 13px, r=8. Enter im Passwortfeld sendet ab.

### App-Shell (alle weiteren Screens)
- Sidebar 232px, bg `#2F2F28`, Padding 22/14/16. Logo (30px Quadrat `#31A2AC`, r=8, „J“ weiß 700; roter Punkt 9px `#AF1C1C` oben rechts, 2px Rand in Sidebar-Farbe) + „JobCraft“ 18px/700 weiß.
- Nav: Übersicht, Neue Bewerbung, Dokumente, Briefkopf. Item: Padding 10/12, r=8, 14px/500; inaktiv Text `#BDBCCA`, Punkt `#5A5A51`; aktiv bg `#45453D`, Text weiß, Punkt `#31A2AC`. Der Editor zählt als „Übersicht“.
- Unten: Avatar 34px rund (bg `#F0EFFE`, Initialen), Name/E-Mail (ellipsis), „Abmelden“.
- Hauptbereich: bg `#F0EFFE`, scrollt unabhängig.

### 2. Übersicht / Dashboard (`02-dashboard.png`)
- Container Padding 36/44/56, max-width 1180. H1 „Guten Tag, {Vorname}“ 28px/600, Subline mit Anzahl Entwürfe.
- Grid `1.9fr 1fr`: links dunkle Karte (`#2F2F28`, r=16, Padding 28/30) mit Label „NEUE BEWERBUNG“ (Mono 11px, `#31A2AC`), H2 „Link zur Stellenanzeige einfügen“, URL-Input (bg `#F0EFFE`) + Button „Analysieren“, Hinweistext, Link „Beispiel-Link verwenden“ (`#7FD0D6`). Fehler: rote Pill (bg `#AF1C1C`, weiß).
- Rechts zwei Status-Karten (weiß, Rand `#E2E0F2`, r=14, Hover-Rand `#31A2AC`, klickbar): „Briefkopf“ mit Prozent + Progressbar (6px, Track `#ECEAF8`, Fill `#31A2AC`); „Dokumente“ mit Anzahl + Anzahl mit extrahiertem Text.
- Tabelle „Bewerbungen“: Spalten `2.4fr 1fr .9fr 70px 86px` (Unternehmen/Position, Status, Erstellt, Version, „Öffnen →“). Kopfzeile 11.5px uppercase `#8A8996` auf `#FAFAFF`. Zeile Hover `#F7F6FF`, Klick öffnet den Editor.
- Status-Pills (12px/600, r=20): Entwurf `#ECEBE4`/`#55544C` · Versendet `#DCEFF1`/`#1E6E75` · Gespräch `#2F2F28`/weiß · Abgesagt `#F8E4E4`/`#AF1C1C`.

### 3. Neue Bewerbung – Stepper
Schritte: Stellenanzeige → Analyse → Prüfen & Kontext → Anschreiben. Kreis 26px: erledigt `#31A2AC` mit ✓, aktiv `#2F2F28`, offen weiß mit Rand `#CFCDE3`. Verbinder 36×1.5px `#D4D2EA`.
- **Schritt 0 (URL):** Karte max. 720px, H1 „Welche Stelle reizt dich?“, Input, CTA „Stellenanzeige analysieren“. URL-Validierung `^https?://`.
- **Schritt 1 (Analyse, `03-analyse.png`):** URL in Mono; Liste: „Seite abrufen“, „HTML-Inhalt bereinigen“, „Eckdaten extrahieren“, „Mit deinem Profil abgleichen“. Je Item ✓ (erledigt), Spinner (aktiv; 14px, Rand `#CFE9EB`, Top `#31A2AC`, 0.8s linear) oder leerer Kreis (offen). Im Prototyp 750 ms pro Schritt, danach Schritt 2. Real: Fortschritt von `POST /api/applications` (z. B. per SSE/Polling).
- **Schritt 2 (Prüfen & Kontext, `04-pruefen-kontext.png`):** Grid `1.5fr 1fr`.
  - Links „Eckdaten prüfen“: Badge „5 von 6 Anforderungen erfüllt“; editierbare Felder Unternehmen, Position, Ansprechpartner:in, Adresse (2-Spalten-Grid); Anforderungs-Chips (erfüllt `#DCEFF1`/`#1E6E75` mit ✓, fehlend `#F8E4E4`/`#AF1C1C` mit –); Aufgabenliste (5px-Punkte `#31A2AC`).
  - Rechts (sticky) „Kontext für die KI“: Checkbox-Liste der Dokumente (18px, r=5, aktiv `#31A2AC`); Segmented „Tonalität“ (Formell/Modern/Locker) und „Länge“ (Kompakt/Standard/Ausführlich), Track `#F0EFFE`. CTA „Anschreiben generieren“ (volle Breite, r=12, 15px), Textlink „Andere Anzeige analysieren“.

### 4. Anschreiben-Editor (`05-editor-v1.png`, `06-editor-chat-v2.png`)
- Toolbar weiß, Rand unten `#E2E0F2`: „← Übersicht“, Unternehmen + Position, Versions-Chips (Mono 12px; aktiv `#2F2F28` weiß), Button „PDF exportieren“ (bg `#AF1C1C`, Hover `#961616`).
- Links: scrollbare Fläche (`align-items: safe center`) mit A4-Blatt 595×842 px, weiß, Shadow `0 1px 2px rgba(47,47,40,.06), 0 14px 36px rgba(47,47,40,.10)`, Padding 56/64/64. Brief-Schrift **Source Serif 4** 12.5px/1.62.
  - Briefkopf **Klassisch**: Name 25px/600 links, Kontakt rechts (DM Sans 9.5px `#55544C`), Linie 1.5px `#2F2F28`. **Modern**: Band `#31A2AC` randlos, Name DM Sans 24px/700 weiß + Adresse.
  - Absenderzeile 8.5px unterstrichen → Empfängerblock (aus Scraping) + Datum rechts („Köln, 19. Oktober 2026“) → Betreff fett („Bewerbung als {Position ohne (m/w/d)}“) → Anrede (aus Ansprechpartner abgeleitet: „Sehr geehrte Frau Dr. Kessler,“; Fallback „Sehr geehrte Damen und Herren,“) → 5 Absätze → „Mit freundlichen Grüßen“ + Name.
  - Geänderter Absatz wird 2.6 s mit `#D6EEF0` hinterlegt (Transition 0.8s).
  - Bei älterer Version: dunkles Banner „Du siehst eine ältere Version (vX).“ + „Zur aktuellen →“.
- Rechts: Chat-Panel 340px (shrinkt bis 300px), weiß. Header „KI-Assistent“ + „Tonalität · Länge“. Nachrichten: KI links bg `#F0EFFE` r=`12 12 12 4`; User rechts bg `#2F2F28` weiß r=`12 12 4 12`; 13.5px/1.5, max 88 %. Meta unter KI-Nachricht (Mono 10.5px `#217A82`, z. B. „v2 · Absatz 2 geändert“). Typing-Indikator: 3 Punkte, Animation `jc-dot` 1.2s, versetzt 0.15s. Einblend-Animation `jc-in` (6px nach oben, 0.25s).
  - Vorschlags-Chips: „Java-Absatz prägnanter“, „Soft Skills stärker betonen“, „Englischkenntnisse erwähnen“, „Einleitung kürzen“, „Kubernetes-Lücke adressieren“.
  - Textarea (2 Zeilen) + „Senden“. Enter sendet, Shift+Enter macht einen Zeilenumbruch.
  - Jede KI-Antwort erzeugt eine neue `CoverLetter`-Version (`POST /api/applications/{id}/chat`).

### 5. PDF-Export-Dialog (`07-pdf-export.png`)
Overlay `rgba(47,47,40,.55)`, Dialog max. 520px, r=18. Vorlagenwahl Klassisch/Modern als Karten mit Mini-Vorschau (aktiv Rand 2px `#31A2AC`). Toggles „Lebenslauf anhängen“, „Zeugnisse anhängen“ (36×20, an `#31A2AC`). Dateiname in Mono-Box: `Bewerbung_{Unternehmen}_{Nachname}.pdf`. Zustände: idle (roter CTA „PDF erzeugen & herunterladen“) → gen (Spinner „PDF wird gerendert …“) → done (bg `#DCEFF1`, „✓ PDF bereit · Größe“) + Toast. Backend: `GET /api/applications/{id}/pdf`.

### 6. Dokumente (`08-dokumente.png`, `09-dokument-vorschau.png`)
Dropzone (Rand 1.5px gestrichelt `#B9B7D6`, bg `#FAFAFF`, Hover Teal), „PDF oder DOCX · max. 10 MB“. Filter-Pills: Alle/Lebenslauf/Skill-Profil/Zeugnis/Sonstiges mit Anzahl (aktiv `#2F2F28`). Liste: Spalten `44px minmax(180px,1fr) auto auto auto`: Datei-Thumbnail (gestreift, Endung PDF rot / DOCX teal), Name + Meta (Größe · Seiten · Datum), Kategorie-Pill, Status („Wird verarbeitet …“ mit Spinner → „Text extrahiert“ mit Teal-Punkt), „Vorschau“, „Löschen“ (rot, Hover bg `#F8E4E4`) mit Toast. Kategorie-Farben: Lebenslauf `#DCEFF1`/`#1E6E75`, Skill-Profil `#E4E2FA`/`#3B3878`, Zeugnis `#ECEBE4`/`#55544C`, Sonstiges `#F8E4E4`/`#AF1C1C`. Vorschau-Dialog max. 820px: links Seitenvorschau (A4-Seitenverhältnis), rechts „Extrahierter Text“ in Mono. Das Kategorie-Feld fehlt beim Upload noch; vorschlagen: Auswahl nach dem Upload oder automatische Erkennung.

### 7. Briefkopf & Stammdaten (`10-briefkopf.png`)
Grid `1fr 1fr`. Links Karten: „Persönliche Daten“ (Name volle Breite; Straße, PLZ & Ort, Telefon, E-Mail, LinkedIn, GitHub), „Eigene Felder“ (Bezeichnung + Wert + „Entfernen“, „+ Feld hinzufügen“ → `Profile.customFields` JSON), Logo-Upload-Platzhalter 84px. Rechts sticky Live-Vorschau (420px hoch, Fade unten) mit Umschalter Klassisch/Modern; zeigt Briefkopf, Absenderzeile, Platzhalter-Empfänger, eigene Felder. „Speichern“ → `PUT /api/profile` + Toast „Briefkopf gespeichert“.

## Interactions & Behavior
- Toast: unten mittig, bg `#2F2F28`, weiß, r=10, 2.6 s, mit Teal-Punkt.
- Inputs: Rand `#D9D7EC`, r=9, Padding 11/13, Fokus: Rand `#31A2AC` + Ring `0 0 0 3px rgba(49,162,172,.18)`.
- Primärbutton: `#31A2AC`, Hover `#2A8F98`, weiß 600, r=9–10. Sekundär: weiß, Rand `#D9D7EC`, Hover Rand `#2F2F28`.
- Klicks auf Modal-Overlays schließen den Dialog. Die Chat-Liste scrollt bei neuen Nachrichten automatisch nach unten.
- Mock-Chat-Regeln (stichwortbasiert) stehen in `RULES` im Prototyp – nur Demo; real ersetzt durch den LLM-Aufruf mit Kontext (Profil, Dokument-Texte, Anzeige, aktuelle Version, Tonalität/Länge).

## State Management (Signals-Vorschlag)
- `auth`: user, jwt · `profile`: Felder + customFields · `documents[]`: id, name, category, size, pages, status (processing|ready), extractedText
- `applications[]`: id, company, role, contact, address, status, createdAt, latestVersion
- `newApplication`: url, step (0–2), analysisProgress, scraped job, requirements[{text, matched}], tasks[], contextDocIds, tone, length
- `editor`: versions[] (CoverLetter.content), currentVersion, messages[], typing, changedParagraph · `pdfExport`: template, attachCv, attachCerts, state

## Design Tokens
- Hauptfarben (vorgegeben): Teal `#31A2AC`, Rot `#AF1C1C`, Lavendel-Weiß `#F0EFFE` (App-Hintergrund), Ink `#2F2F28`
- Ergänzt: Teal Hover `#2A8F98`, Teal-Text `#217A82`/`#1E6E75`, Teal-Tint `#DCEFF1`/`#D6EEF0`, Rot Hover `#961616`, Rot-Tint `#F8E4E4`, Lavendel-Tint `#E4E2FA`, Segment-Track `#E4E2F6`, Ränder `#E2E0F2` / `#D9D7EC` / `#ECEAF8`, Zeilen-Hover `#F7F6FF`/`#FAFAFF`, Text sekundär `#6B6A62`, Label `#55544C`, tertiär `#8A8996`, Sidebar aktiv `#45453D`, Sidebar-Text `#BDBCCA`
- Schrift: **DM Sans** (UI; 400/500/600/700), **Source Serif 4** (Brief), ui-monospace (Labels, Versionen, Code)
- Typo-Skala: 46 / 28 / 26 / 22 / 18 / 16 / 15 / 14.5 / 14 / 13.5 / 13 / 12.5 / 12 / 11.5 / 11 px
- Radien: 5, 7, 8, 9, 10, 12, 14, 16, 18, 20 (Pills)
- Abstände: 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 28, 36, 44 px
- Shadows: Segment `0 1px 3px rgba(47,47,40,.12)` · Papier siehe oben · Toast `0 10px 30px rgba(47,47,40,.25)`

## Assets
Keine Bild-Assets. Das Logo ist ein Platzhalter aus CSS (Quadrat + Punkt), Dokument-Thumbnails und der Logo-Upload sind gestreifte Platzhalter. Icons sind nur Textzeichen (✓, ×, +, →); in der Umsetzung durch das Icon-Set der UI-Library (z. B. Material Symbols) ersetzen.

## Files
- `prototype/JobCraft.dc.html` – klickbarer Prototyp (alle Screens, Mock-Daten, Logik)
- `prototype/support.js` – Laufzeit für den Prototyp (nicht Teil der Umsetzung)
- `screenshots/*.png` – Screens 01–10
- `Konzept.pdf` – Fachkonzept (Architektur, Datenmodell, API)
