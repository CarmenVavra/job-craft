-- Eigene Anrede je Bewerbung; leer = automatisch aus der Ansprechperson abgeleitet.
ALTER TABLE applications ADD COLUMN salutation VARCHAR(200);
