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
    void prefersTheFullJobPostingFromJsonLd() {
        String description = "<h4>Deine Aufgaben</h4><ul><li>Entwicklung mit Spring und Angular</li>"
                + "<li>Code Reviews im Team</li></ul><h4>Dein Profil</h4><ul><li>Mindestens 3 Jahre Erfahrung mit"
                + " Java</li><li>Erfahrung mit TypeScript</li></ul><p>Wir bieten flexible Arbeitszeiten, Homeoffice"
                + " und ein motiviertes Team in Wiener Neudorf.</p>";
        String html = """
                <html><head><script type="application/ld+json">%s</script></head>
                <body><article><h1>Senior Software Engineer</h1><p>WALTER GROUP</p></article>
                <article><p>Teil der Anzeige</p></article></body></html>
                """.formatted(jobPosting(description));

        HtmlCleaner.CleanedPage page = HtmlCleaner.clean(html, "https://www.stepstone.at/job");

        assertThat(page.title()).isEqualTo("Senior Software Engineer (m/w/d)");
        assertThat(page.text()).startsWith("Senior Software Engineer (m/w/d)\nWALTER GROUP\nWiener Neudorf\n"
                + "Deine Aufgaben\n- Entwicklung mit Spring und Angular\n- Code Reviews im Team\nDein Profil\n");
    }

    @Test
    void readsEscapedHtmlDescriptionsInsideAGraph() {
        String description = "&lt;p&gt;Wir suchen Verstärkung für unser wachsendes Team in Wien und Graz.&lt;/p&gt;"
                + "&lt;ul&gt;&lt;li&gt;Du entwickelst Webanwendungen mit Angular und TypeScript&lt;/li&gt;"
                + "&lt;li&gt;Du hast Erfahrung mit REST-Schnittstellen und Spring Boot&lt;/li&gt;"
                + "&lt;li&gt;Du arbeitest gerne agil im Team&lt;/li&gt;&lt;/ul&gt;";
        String html = "<html><head><script type=\"application/ld+json\">{\"@graph\":[{\"@type\":\"Organization\"},"
                + jobPosting(description) + "]}</script></head><body><main><p>Kurz</p></main></body></html>";

        HtmlCleaner.CleanedPage page = HtmlCleaner.clean(html, "https://example.com");

        assertThat(page.text()).contains("Wir suchen Verstärkung", "- Du entwickelst Webanwendungen mit Angular");
    }

    @Test
    void ignoresAShortTeaserInJsonLd() {
        String html = "<html><head><script type=\"application/ld+json\">" + jobPosting("<p>Kurzer Teaser</p>")
                + "</script></head><body><main><h1>Java Developer</h1><p>Vollständige Anzeige</p></main></body></html>";

        assertThat(HtmlCleaner.clean(html, "https://example.com").text())
                .isEqualTo("Java Developer\nVollständige Anzeige");
    }

    @Test
    void usesTheWholePageWhenTheFirstArticleIsOnlyTheHeader() {
        String html = """
                <html><body>
                <article><h1>Senior Software Engineer</h1><p>WALTER GROUP</p></article>
                <article><h4>Deine Aufgaben</h4><ul><li>Entwicklung von internen Tools und Prototypen</li></ul></article>
                <article><h4>Dein Profil</h4><ul><li>Mindestens drei Jahre Erfahrung in der Softwareentwicklung</li></ul>
                </article></body></html>
                """;

        String text = HtmlCleaner.clean(html, "https://example.com").text();

        assertThat(text).contains("Deine Aufgaben", "- Entwicklung von internen Tools und Prototypen",
                "- Mindestens drei Jahre Erfahrung in der Softwareentwicklung");
    }

    @Test
    void opensTheSelectedAdOfAKarriereAtResultList() {
        assertThat(JobScraperService.toJobAdUri(URI.create(
                "https://www.karriere.at/jobs?keywords=angular&locations=Wien+und+Umgebung#10009538")))
                .isEqualTo(URI.create("https://www.karriere.at/jobs/10009538"));
        assertThat(JobScraperService.toJobAdUri(URI.create("https://www.karriere.at/jobs/10009538#top")))
                .isEqualTo(URI.create("https://www.karriere.at/jobs/10009538#top"));
        assertThat(JobScraperService.toJobAdUri(URI.create("https://example.com/jobs#12345678")))
                .isEqualTo(URI.create("https://example.com/jobs#12345678"));
    }

    private static String jobPosting(String description) {
        return "{\"@context\":\"https://schema.org\",\"@type\":\"JobPosting\","
                + "\"title\":\"Senior Software Engineer (m/w/d)\",\"description\":\"" + description + "\","
                + "\"hiringOrganization\":{\"@type\":\"Organization\",\"name\":\"WALTER GROUP\"},"
                + "\"jobLocation\":{\"@type\":\"Place\",\"address\":{\"addressLocality\":\"Wiener Neudorf\"}}}";
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
