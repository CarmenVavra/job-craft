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
import java.util.Locale;
import java.util.regex.Pattern;
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
    private static final Pattern KARRIERE_JOB_ID = Pattern.compile("\\d{5,}");

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
        uri = toJobAdUri(uri);
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

    /**
     * Links aus der Trefferliste von karriere.at ({@code /jobs?keywords=…#10009538}) zeigen die Liste; die
     * ausgewählte Anzeige steht nur im Fragment. Dann wird die Detailseite {@code /jobs/10009538} abgerufen.
     *
     * @param uri eingegebene URL
     * @return URL der eigentlichen Anzeige
     */
    static URI toJobAdUri(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        String fragment = uri.getFragment();
        boolean karriere = "karriere.at".equals(host) || host.endsWith(".karriere.at");
        if (karriere && fragment != null && KARRIERE_JOB_ID.matcher(fragment).matches()
                && !KARRIERE_JOB_ID.matcher(lastSegment(uri.getPath())).matches()) {
            return URI.create(uri.getScheme() + "://" + uri.getHost() + "/jobs/" + fragment);
        }
        return uri;
    }

    private static String lastSegment(String path) {
        if (path == null) {
            return "";
        }
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        return trimmed.substring(trimmed.lastIndexOf('/') + 1);
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
