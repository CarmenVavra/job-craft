package com.jobcraft.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobcraft.model.LetterLength;
import com.jobcraft.model.Requirement;
import com.jobcraft.model.Tone;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnthropicContextBlockTest {

    private static GenerationContext context(List<GenerationContext.NoteText> notes) {
        return new GenerationContext("Lena Hoffmann", List.of(),
                List.of(new GenerationContext.DocumentText("cv.pdf", "Lebenslauf", "Java 21")), notes,
                "Quantis Software GmbH", "Entwicklerin", List.of(new Requirement("Angular", true)), List.of(),
                "Anzeigentext", Tone.FORMAL, LetterLength.STANDARD);
    }

    @Test
    void includesNotesAsSeparateDataBlock() {
        String block = AnthropicCoverLetterAi.contextBlock(context(List.of(
                new GenerationContext.NoteText("Privatprojekt \"Haushaltsbuch\"", "Angular 21 mit Signals"))));

        assertThat(block).contains("<weitere_angaben>\n<angabe titel=\"Privatprojekt 'Haushaltsbuch'\">\n"
                + "Angular 21 mit Signals\n</angabe>\n</weitere_angaben>");
        assertThat(block.indexOf("<weitere_angaben>")).isBetween(block.indexOf("</dokument>"),
                block.indexOf("<stelle>"));
    }

    @Test
    void omitsNotesBlockWithoutNotes() {
        assertThat(AnthropicCoverLetterAi.contextBlock(context(List.of()))).doesNotContain("weitere_angaben");
    }
}
