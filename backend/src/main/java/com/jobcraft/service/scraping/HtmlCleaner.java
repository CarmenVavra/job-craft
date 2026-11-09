package com.jobcraft.service.scraping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

/**
 * Entfernt mit JSoup Skripte, Styles, Navigation, Footer &amp; Co. und liefert den reinen Fließtext einer
 * Stellenanzeige mit erhaltener Absatz- und Listenstruktur.
 *
 * <p>Enthält die Seite strukturierte Daten nach schema.org ({@code JobPosting} als JSON-LD, z. B. StepStone,
 * karriere.at, Indeed), wird deren vollständige Beschreibung verwendet – unabhängig vom Seitenlayout.
 */
public final class HtmlCleaner {

    private static final String NOISE = "script, style, noscript, template, svg, iframe, nav, footer, header, form, "
            + "button, aside, [role=navigation], [role=banner], [role=contentinfo], [aria-hidden=true], "
            + ".cookie, .cookies, #cookie, [class*=cookie], [id*=cookie], [class*=consent], [id*=consent]";
    private static final String BLOCKS = "h1, h2, h3, h4, h5, h6, p, li, dt, dd, td, th, blockquote, address";
    private static final int MAX_LENGTH = 30_000;
    /** Kürzere JSON-LD-Beschreibungen sind meist nur ein Teaser – dann zählt die Seite selbst. */
    private static final int MIN_POSTING_DESCRIPTION = 200;
    /** Ein Inhaltsbereich mit weniger Text als dieser Anteil der Seite ist nur ein Teil der Anzeige (z. B. Kopf). */
    private static final double MIN_ROOT_SHARE = 0.3;
    private static final ObjectMapper JSON = new ObjectMapper();

    private HtmlCleaner() {
    }

    /**
     * Bereinigte Seite.
     *
     * @param title       Seitentitel bzw. erste Überschrift
     * @param siteName    {@code og:site_name}, falls vorhanden
     * @param text        bereinigter Text (Absätze durch Leerzeilen, Listenpunkte mit „- “)
     */
    public record CleanedPage(String title, String siteName, String text) {
    }

    /**
     * @param html    HTML-Quelltext
     * @param baseUri Basis-URL
     * @return bereinigter Inhalt
     */
    public static CleanedPage clean(String html, String baseUri) {
        Document doc = Jsoup.parse(html, baseUri);
        String siteName = doc.select("meta[property=og:site_name]").attr("content").trim();
        Element h1 = doc.selectFirst("h1");
        String title = h1 != null ? h1.text().trim() : doc.title().trim();

        Optional<CleanedPage> posting = fromJobPosting(doc, title, siteName);
        if (posting.isPresent()) {
            return posting.get();
        }

        doc.select(NOISE).remove();
        Element body = doc.body();
        Element root = doc.selectFirst("main, article, [role=main]");
        if (root == null || root.text().length() < body.text().length() * MIN_ROOT_SHARE) {
            root = body;
        }
        return new CleanedPage(title, siteName, limit(extractText(root)));
    }

    /** Text der Blockelemente, Listenpunkte mit „- “, ohne direkt aufeinanderfolgende Dubletten. */
    private static String extractText(Element root) {
        List<String> lines = new ArrayList<>();
        Elements blocks = root.select(BLOCKS);
        for (Element block : blocks) {
            boolean listItem = "li".equals(block.tagName());
            boolean insideListItem = block.parents().stream().anyMatch(p -> "li".equals(p.tagName()));
            boolean container = block.select(BLOCKS).size() > 1;
            if (insideListItem || (!listItem && container)) {
                continue;
            }
            String text = block.text().trim();
            if (text.isEmpty()) {
                continue;
            }
            String line = listItem ? "- " + text : text;
            if (lines.isEmpty() || !lines.get(lines.size() - 1).equals(line)) {
                lines.add(line);
            }
        }
        return lines.isEmpty() ? root.text() : String.join("\n", lines);
    }

    /** Anzeige aus JSON-LD ({@code JobPosting}): Titel, Unternehmen, Ort und vollständige Beschreibung. */
    private static Optional<CleanedPage> fromJobPosting(Document doc, String fallbackTitle, String siteName) {
        for (Element script : doc.select("script[type=application/ld+json]")) {
            Optional<JsonNode> posting = parse(script.data()).flatMap(HtmlCleaner::findJobPosting);
            if (posting.isEmpty()) {
                continue;
            }
            JsonNode job = posting.get();
            String description = job.path("description").asText("");
            if (!description.contains("<") && description.contains("&lt;")) {
                description = Parser.unescapeEntities(description, false);
            }
            Document body = Jsoup.parseBodyFragment(description);
            String text = extractText(body.body()).trim();
            if (text.length() < MIN_POSTING_DESCRIPTION) {
                continue;
            }
            String title = plain(job.path("title").asText("")).trim();
            String company = job.path("hiringOrganization").path("name").asText("").trim();
            String header = Stream.of(title, company, locations(job.path("jobLocation")))
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining("\n"));
            String full = header.isEmpty() ? text : header + "\n" + text;
            return Optional.of(new CleanedPage(title.isEmpty() ? fallbackTitle : title, siteName, limit(full)));
        }
        return Optional.empty();
    }

    private static Optional<JsonNode> parse(String json) {
        try {
            return Optional.ofNullable(JSON.readTree(json));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    /** Sucht ein {@code JobPosting} auch in Arrays und {@code @graph}. */
    private static Optional<JsonNode> findJobPosting(JsonNode node) {
        if (node.isArray()) {
            for (JsonNode child : node) {
                Optional<JsonNode> found = findJobPosting(child);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }
        if (!node.isObject()) {
            return Optional.empty();
        }
        JsonNode type = node.path("@type");
        boolean jobPosting = "JobPosting".equals(type.asText());
        for (JsonNode t : type) {
            jobPosting |= "JobPosting".equals(t.asText());
        }
        if (jobPosting) {
            return Optional.of(node);
        }
        return node.has("@graph") ? findJobPosting(node.get("@graph")) : Optional.empty();
    }

    /** Orte aus {@code jobLocation} (einzeln oder als Liste), z. B. „Wiener Neudorf“. */
    private static String locations(JsonNode jobLocation) {
        List<JsonNode> places = new ArrayList<>();
        if (jobLocation.isArray()) {
            jobLocation.forEach(places::add);
        } else if (jobLocation.isObject()) {
            places.add(jobLocation);
        }
        return places.stream()
                .map(p -> p.path("address").path("addressLocality").asText("").trim())
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private static String plain(String text) {
        return Jsoup.parseBodyFragment(text).text();
    }

    private static String limit(String text) {
        return text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text;
    }
}
