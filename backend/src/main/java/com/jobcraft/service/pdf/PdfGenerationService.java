package com.jobcraft.service.pdf;

import com.jobcraft.dto.LetterMetaDto;
import com.jobcraft.exception.ExternalServiceException;
import com.jobcraft.model.CustomField;
import com.jobcraft.model.LetterStyle;
import com.jobcraft.model.Profile;
import com.jobcraft.service.BrowserManager;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.WaitUntilState;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Erzeugt das druckfertige Anschreiben: Thymeleaf füllt das HTML-Template, Headless-Chromium (Playwright) rendert
 * es pixelgenau als A4-PDF (DIN 5008). Optionale Anhänge (PDF) werden mit PDFBox angefügt.
 */
@Service
@RequiredArgsConstructor
public class PdfGenerationService {

    private static final Duration RENDER_TIMEOUT = Duration.ofSeconds(60);
    /** Fußzeile mehrseitiger Briefe (Chromium füllt pageNumber/totalPages). */
    private static final String PAGE_FOOTER = "<div style=\"width:100%;font-family:Arial,sans-serif;font-size:8px;"
            + "color:#6B6A62;text-align:right;padding:0 20mm 0 25mm;\">Seite <span class=\"pageNumber\"></span> "
            + "von <span class=\"totalPages\"></span></div>";

    private final TemplateEngine templateEngine;
    private final BrowserManager browserManager;

    /**
     * Daten für das Anschreiben.
     *
     * @param profile    Briefkopf
     * @param meta       Briefbestandteile
     * @param paragraphs Absätze
     * @param style      Vorlage
     */
    public record LetterData(Profile profile, LetterMetaDto meta, List<String> paragraphs, LetterStyle style) {
    }

    /**
     * Füllt das Template.
     *
     * @param data Briefdaten
     * @return HTML-Dokument
     */
    public String renderHtml(LetterData data) {
        Profile p = data.profile();
        Context ctx = new Context();
        ctx.setVariable("meta", data.meta());
        ctx.setVariable("paragraphs", data.paragraphs());
        ctx.setVariable("modern", data.style() == LetterStyle.MODERN);
        ctx.setVariable("contactLines", nonBlank(p.getPhone(), p.getEmail(), p.getLinkedin(), p.getGithub()));
        String zipCity = String.join(" ", nonBlank(p.getZipCode(), p.getCity()));
        ctx.setVariable("modernAddress", String.join(" · ", nonBlank(p.getAddressLine1(), zipCity)));
        List<CustomField> custom = p.getCustomFields() == null ? List.of() : p.getCustomFields().stream()
                .filter(f -> f.label() != null && !f.label().isBlank() && f.value() != null && !f.value().isBlank())
                .toList();
        ctx.setVariable("customFields", custom);
        return templateEngine.process("cover-letter", ctx);
    }

    /**
     * Rendert das Anschreiben als PDF und hängt optional weitere PDF-Dokumente an.
     *
     * @param data        Briefdaten
     * @param attachments PDF-Anhänge (Lebenslauf, Zeugnisse)
     * @return PDF-Bytes
     */
    public byte[] render(LetterData data, List<byte[]> attachments) {
        String html = renderHtml(data);
        byte[] letter = browserManager.execute(browser -> {
            try (Page page = browser.newPage()) {
                try {
                    page.setContent(html, new Page.SetContentOptions().setWaitUntil(WaitUntilState.NETWORKIDLE)
                            .setTimeout(15_000));
                } catch (com.microsoft.playwright.TimeoutError e) {
                    // Webfonts nicht erreichbar (offline) – die Systemschriften genügen als Fallback.
                }
                byte[] pdf = page.pdf(pdfOptions(false));
                // Mehrseitige Briefe erhalten nach DIN 5008 eine Seitenangabe („Seite 1 von 2“).
                return pageCount(pdf) > 1 ? page.pdf(pdfOptions(true)) : pdf;
            }
        }, RENDER_TIMEOUT);
        return attachments.isEmpty() ? letter : merge(letter, attachments);
    }

    private static Page.PdfOptions pdfOptions(boolean pageNumbers) {
        Page.PdfOptions options = new Page.PdfOptions()
                .setFormat("A4")
                .setPrintBackground(true)
                .setPreferCSSPageSize(true)
                .setMargin(new Margin().setTop("0").setBottom("0").setLeft("0").setRight("0"));
        if (pageNumbers) {
            options.setDisplayHeaderFooter(true)
                    .setHeaderTemplate("<span></span>")
                    .setFooterTemplate(PAGE_FOOTER);
        }
        return options;
    }

    /**
     * @param pdf PDF-Bytes
     * @return Seitenzahl
     */
    static int pageCount(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new ExternalServiceException("Das PDF konnte nicht gelesen werden.", e);
        }
    }

    private static byte[] merge(byte[] letter, List<byte[]> attachments) {
        try (PDDocument target = Loader.loadPDF(letter)) {
            PDFMergerUtility merger = new PDFMergerUtility();
            for (byte[] attachment : attachments) {
                try (PDDocument source = Loader.loadPDF(new RandomAccessReadBuffer(attachment))) {
                    merger.appendDocument(target, source);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            target.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ExternalServiceException("Die Anhänge konnten nicht angefügt werden.", e);
        }
    }

    private static List<String> nonBlank(String... values) {
        return Stream.of(values).filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
