package com.jobcraft.service;

import com.jobcraft.config.JobCraftProperties;
import com.jobcraft.exception.ExternalServiceException;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Verwaltet eine geteilte Headless-Chromium-Instanz (Playwright). Playwright ist nicht thread-safe, daher
 * laufen alle Browser-Aufgaben nacheinander auf einem eigenen Thread.
 */
@Slf4j
@Component
public class BrowserManager {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "playwright");
        t.setDaemon(true);
        return t;
    });
    private final JobCraftProperties properties;
    private Playwright playwright;
    private Browser browser;

    public BrowserManager(JobCraftProperties properties) {
        this.properties = properties;
    }

    /**
     * Startet den Browser beim Hochfahren im Hintergrund (lädt Chromium beim ersten Start ggf. herunter).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        if (properties.pdf().warmUp()) {
            executor.submit(() -> {
                try {
                    ensureBrowser();
                    log.info("Headless-Browser für Scraping und PDF ist bereit.");
                } catch (RuntimeException e) {
                    log.warn("Headless-Browser konnte nicht gestartet werden: {}", e.getMessage());
                }
            });
        }
    }

    /**
     * Führt eine Aufgabe mit dem geteilten Browser aus.
     *
     * @param task    Aufgabe
     * @param timeout maximale Laufzeit
     * @param <T>     Ergebnistyp
     * @return Ergebnis der Aufgabe
     */
    public <T> T execute(Function<Browser, T> task, Duration timeout) {
        Future<T> future = executor.submit(() -> task.apply(ensureBrowser()));
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ExternalServiceException("Zeitüberschreitung im Headless-Browser.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Browser-Aufgabe wurde unterbrochen.", e);
        } catch (ExecutionException e) {
            throw new ExternalServiceException("Fehler im Headless-Browser.", e.getCause());
        }
    }

    private Browser ensureBrowser() {
        if (browser == null || !browser.isConnected()) {
            closeQuietly();
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        }
        return browser;
    }

    private void closeQuietly() {
        try {
            if (browser != null) {
                browser.close();
            }
            if (playwright != null) {
                playwright.close();
            }
        } catch (RuntimeException e) {
            log.debug("Fehler beim Schließen des Browsers", e);
        }
        browser = null;
        playwright = null;
    }

    /**
     * Schließt den Browser beim Herunterfahren.
     */
    @PreDestroy
    public void shutdown() {
        try {
            executor.submit(this::closeQuietly).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            log.debug("Browser konnte nicht sauber beendet werden", e);
        }
        executor.shutdownNow();
    }
}
