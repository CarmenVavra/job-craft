package com.jobcraft.service.scraping;

import com.jobcraft.config.JobCraftProperties;
import com.jobcraft.exception.ApiException;
import com.jobcraft.exception.ExternalServiceException;
import com.jobcraft.service.BrowserManager;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Ruft Stellenanzeigen ab: bevorzugt per Headless-Chromium (Playwright, inkl. JavaScript/SPAs), mit JSoup als
 * Fallback für einfache Seiten.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobScraperService {

    /** Beispiel-URL aus dem UI; liefert im Demo-Modus eine eingebaute Anzeige. */
    public static final String DEMO_HOST = "karriere.quantis-software.de";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) JobCraft/1.0";

    private final BrowserManager browserManager;
    private final JobCraftProperties properties;

    /**
     * Lädt das HTML der Stellenanzeige.
     *
     * @param url URL der Anzeige
     * @return HTML-Quelltext
     */
    public String fetchHtml(String url) {
        URI uri = UrlGuard.validate(url);
        if (properties.scraper().demoEnabled() && DEMO_HOST.equalsIgnoreCase(uri.getHost())) {
            return loadDemo();
        }
        UrlGuard.ensurePublicHost(uri);
        if (properties.scraper().useBrowser()) {
            try {
                return fetchWithBrowser(uri.toString());
            } catch (ApiException | com.microsoft.playwright.PlaywrightException e) {
                log.info("Browser-Abruf fehlgeschlagen, nutze JSoup-Fallback: {}", e.getMessage());
            }
        }
        return fetchWithJsoup(uri.toString());
    }

    private String fetchWithBrowser(String url) {
        int timeout = (int) properties.scraper().timeout().toMillis();
        return browserManager.execute(browser -> {
            try (BrowserContext context = browser.newContext(new com.microsoft.playwright.Browser.NewContextOptions()
                    .setUserAgent(USER_AGENT).setLocale("de-DE"));
                 Page page = context.newPage()) {
                page.navigate(url, new Page.NavigateOptions().setTimeout(timeout)
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                try {
                    page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE,
                            new Page.WaitForLoadStateOptions().setTimeout(Math.min(timeout, 8000)));
                } catch (com.microsoft.playwright.TimeoutError ignored) {
                    // Seiten mit Dauer-Polling erreichen nie „networkidle“ – der DOM-Stand genügt.
                }
                return page.content();
            }
        }, properties.scraper().timeout().plusSeconds(10));
    }

    private String fetchWithJsoup(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout((int) properties.scraper().timeout().toMillis())
                    .followRedirects(true)
                    .get()
                    .outerHtml();
        } catch (IOException e) {
            throw new ExternalServiceException("Die Stellenanzeige konnte nicht abgerufen werden.", e);
        }
    }

    private String loadDemo() {
        try (InputStream in = new ClassPathResource("demo/quantis-job.html").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExternalServiceException("Demo-Anzeige nicht verfügbar.", e);
        }
    }
}
