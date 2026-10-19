package com.jobcraft.service.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.jobcraft.TestPdfs;
import com.jobcraft.dto.LetterMetaDto;
import com.jobcraft.model.LetterStyle;
import com.jobcraft.model.Profile;
import com.jobcraft.service.BrowserManager;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class PdfGenerationServiceTest {

    private final PdfGenerationService service = new PdfGenerationService(engine(), mock(BrowserManager.class));

    private static TemplateEngine engine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        TemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private Document render(List<String> paragraphs) {
        Profile profile = new Profile();
        profile.setFullName("Lena Hoffmann");
        LetterMetaDto meta = new LetterMetaDto("Lena Hoffmann", "Lena Hoffmann · Köln", List.of("Quantis Software GmbH"),
                "Köln, 19. Oktober 2026", "Bewerbung als Entwickler", "Sehr geehrte Damen und Herren,",
                "Mit freundlichen Grüßen");
        return Jsoup.parse(service.renderHtml(
                new PdfGenerationService.LetterData(profile, meta, paragraphs, LetterStyle.CLASSIC)));
    }

    @Test
    void keepsLastParagraphClosingAndSignatureTogether() {
        Document html = render(List.of("Eins", "Zwei", "Drei"));

        assertThat(html.select(".final p").eachText())
                .containsExactly("Drei", "Mit freundlichen Grüßen", "Lena Hoffmann");
        assertThat(html.select(".page > p:not(.salutation)").eachText()).containsExactly("Eins", "Zwei");
        assertThat(html.select("style").html()).contains("@page :first").contains(".final { break-inside: avoid; }");
    }

    @Test
    void handlesSingleParagraph() {
        assertThat(render(List.of("Nur einer")).select(".final p").eachText())
                .containsExactly("Nur einer", "Mit freundlichen Grüßen", "Lena Hoffmann");
    }

    @Test
    void countsPages() {
        assertThat(PdfGenerationService.pageCount(TestPdfs.withLines(List.of("Seite")))).isEqualTo(1);
    }
}
