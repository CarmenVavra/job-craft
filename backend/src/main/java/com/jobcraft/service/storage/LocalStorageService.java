package com.jobcraft.service.storage;

import com.jobcraft.exception.ExternalServiceException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Speichert Dateien im lokalen Dateisystem (Standard für die Entwicklung).
 */
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new ExternalServiceException("Die Datei konnte nicht gespeichert werden.", e);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            throw new ExternalServiceException("Die Datei konnte nicht gelesen werden.", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new ExternalServiceException("Die Datei konnte nicht gelöscht werden.", e);
        }
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Ungültiger Speicherschlüssel");
        }
        return path;
    }
}
