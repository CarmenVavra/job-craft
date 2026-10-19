package com.jobcraft.service.scraping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jobcraft.exception.BadRequestException;
import java.net.URI;
import org.junit.jupiter.api.Test;

class HtmlCleanerTest {

    @Test
    void removesNoiseAndKeepsStructure() {
        String html = """
                <html><head><title>Job – Firma</title><meta property="og:site_name" content="Firma GmbH">
                <script>var x = 1;</script><style>p{}</style></head>
                <body><nav>Menü</nav><div class="cookie-banner">Cookies?</div>
                <main><h1>Backend Developer</h1><p>Einleitung</p>
                <h2>Aufgaben</h2><ul><li>APIs bauen</li><li><p>Reviews</p></li></ul></main>
                <footer>Impressum</footer></body></html>
                """;

        HtmlCleaner.CleanedPage page = HtmlCleaner.clean(html, "https://example.com");

        assertThat(page.title()).isEqualTo("Backend Developer");
        assertThat(page.siteName()).isEqualTo("Firma GmbH");
        assertThat(page.text()).isEqualTo("Backend Developer\nEinleitung\nAufgaben\n- APIs bauen\n- Reviews");
    }

    @Test
    void rejectsNonHttpUrls() {
        assertThatThrownBy(() -> UrlGuard.validate("ftp://example.com/job")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> UrlGuard.validate("kein link")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsInternalHosts() {
        assertThatThrownBy(() -> UrlGuard.ensurePublicHost(URI.create("http://127.0.0.1:8080/")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> UrlGuard.ensurePublicHost(URI.create("http://192.168.0.10/")))
                .isInstanceOf(BadRequestException.class);
    }
}
