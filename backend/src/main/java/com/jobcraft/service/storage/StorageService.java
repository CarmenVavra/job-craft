package com.jobcraft.service.storage;

/**
 * Ablage für hochgeladene Dateien (lokales Dateisystem oder S3-kompatibler Object Storage).
 */
public interface StorageService {

    /**
     * Speichert Bytes unter dem angegebenen Schlüssel.
     *
     * @param key         eindeutiger Schlüssel (z. B. {@code userId/documentId.pdf})
     * @param content     Inhalt
     * @param contentType MIME-Typ
     */
    void store(String key, byte[] content, String contentType);

    /**
     * @param key Schlüssel
     * @return gespeicherter Inhalt
     */
    byte[] load(String key);

    /**
     * Löscht eine Datei; fehlende Dateien werden ignoriert.
     *
     * @param key Schlüssel
     */
    void delete(String key);
}
