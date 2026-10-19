package com.jobcraft.service.scraping;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Entfernt mit JSoup Skripte, Styles, Navigation, Footer &amp; Co. und liefert den reinen Fließtext einer
 * Stellenanzeige mit erhaltener Absatz- und Listenstruktur.
 */
public final class HtmlCleaner {

    private static final String NOISE = "script, style, noscript, template, svg, iframe, nav, footer, header, form, "
            + "button, aside, [role=navigation], [role=banner], [role=contentinfo], [aria-hidden=true], "
            + ".cookie, .cookies, #cookie, [class*=cookie], [id*=cookie], [class*=consent], [id*=consent]";
    private static final String BLOCKS = "h1, h2, h3, h4, h5, h6, p, li, dt, dd, td, th, blockquote, address";
    private static final int MAX_LENGTH = 30_000;

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

        doc.select(NOISE).remove();
        Element root = doc.selectFirst("main, article, [role=main]");
        if (root == null) {
            root = doc.body();
        }
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
        String text = lines.isEmpty() ? root.text() : String.join("\n", lines);
        if (text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH);
        }
        return new CleanedPage(title, siteName, text);
    }
}
