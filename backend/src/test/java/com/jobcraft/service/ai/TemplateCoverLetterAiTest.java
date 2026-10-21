package com.jobcraft.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobcraft.model.LetterLength;
import com.jobcraft.model.Requirement;
import com.jobcraft.model.Tone;
import java.util.List;
import org.junit.jupiter.api.Test;

class TemplateCoverLetterAiTest {

    private static final String JOB_TEXT = """
            Senior Fullstack Entwickler (m/w/d)
            Deine Aufgaben
            - Konzeption neuer Features
            - Weiterentwicklung der Microservices
            - Code-Reviews
            Dein Profil
            - Java 17+ & Spring Boot
            - Kubernetes / Docker
            - Englisch fließend
            Kontakt
            Deine Ansprechpartnerin: Frau Dr. Miriam Kessler, Head of Engineering
            Quantis Software GmbH Am Mediapark 8 50670 Köln
            """;

    private final TemplateCoverLetterAi ai = new TemplateCoverLetterAi();

    @Test
    void extractsKeyFactsFromJobText() {
        JobExtraction job = ai.extractJob(JOB_TEXT, "Senior Fullstack Entwickler (m/w/d)", "Quantis Software GmbH");

        assertThat(job.companyName()).isEqualTo("Quantis Software GmbH");
        assertThat(job.jobTitle()).isEqualTo("Senior Fullstack Entwickler (m/w/d)");
        assertThat(job.contactPerson()).isEqualTo("Frau Dr. Miriam Kessler");
        assertThat(job.recipientAddress()).isEqualTo("Am Mediapark 8, 50670 Köln");
        assertThat(job.tasks()).containsExactly("Konzeption neuer Features", "Weiterentwicklung der Microservices",
                "Code-Reviews");
        assertThat(job.requirements()).containsExactly("Java 17+ & Spring Boot", "Kubernetes / Docker",
                "Englisch fließend");
    }

    @Test
    void findsCompanyInTextWithoutSiteName() {
        JobExtraction job = ai.extractJob(JOB_TEXT, "Senior Fullstack Entwickler", "");
        assertThat(job.companyName()).isEqualTo("Quantis Software GmbH");
    }

    @Test
    void matchesRequirementsAgainstProfileText() {
        List<Boolean> matches = ai.matchRequirements(
                List.of("Java 17+ & Spring Boot", "Kubernetes / Docker", "Englisch fließend", "Go"),
                "Java 21, Spring Boot, Docker, Englisch (C1)");

        assertThat(matches).containsExactly(true, false, true, false);
    }

    @Test
    void matchesNothingWithoutProfileText() {
        assertThat(ai.matchRequirements(List.of("Java"), "")).containsExactly(false);
    }

    @Test
    void generatesStandardLetterWithFiveParagraphs() {
        List<String> paragraphs = ai.generate(context(LetterLength.STANDARD));

        assertThat(paragraphs).hasSize(5);
        assertThat(paragraphs.get(0)).startsWith("mit großem Interesse")
                .contains("Senior Fullstack Entwickler bei der Quantis Software GmbH")
                .doesNotContain("(m/w/d)");
        assertThat(paragraphs.get(1)).contains("Java 17+ & Spring Boot");
        assertThat(paragraphs.get(4)).contains("1. Januar 2027");
    }

    @Test
    void respectsLengthSetting() {
        assertThat(ai.generate(context(LetterLength.COMPACT))).hasSize(3);
        assertThat(ai.generate(context(LetterLength.DETAILED))).hasSize(6);
    }

    @Test
    void revisesOnlyTheAffectedParagraph() {
        List<String> current = ai.generate(context(LetterLength.STANDARD));

        LetterRevision revision = ai.revise(context(LetterLength.STANDARD), current, "Einleitung kürzen");

        assertThat(revision.changedParagraphs()).containsExactly(0);
        assertThat(revision.paragraphs().get(0)).isNotEqualTo(current.get(0)).endsWith("gelesen.");
        assertThat(revision.paragraphs().subList(1, 5)).isEqualTo(current.subList(1, 5));
    }

    @Test
    void addressesGapsWhenAsked() {
        List<String> current = ai.generate(context(LetterLength.STANDARD));

        LetterRevision revision = ai.revise(context(LetterLength.STANDARD), current, "Kubernetes-Lücke adressieren");

        assertThat(revision.paragraphs().get(2)).contains("Kubernetes / Docker");
        assertThat(revision.reply()).isNotBlank();
    }

    @Test
    void cleansGenderSuffixFromRole() {
        assertThat(TemplateCoverLetterAi.cleanRole("Backend Developer (m/w/d)")).isEqualTo("Backend Developer");
        assertThat(TemplateCoverLetterAi.cleanRole("Architect (all genders)")).isEqualTo("Architect");
    }

    private static GenerationContext context(LetterLength length) {
        return new GenerationContext("Lena Hoffmann", List.of("Verfügbarkeit: 1. Januar 2027"),
                List.of(new GenerationContext.DocumentText("cv.pdf", "Lebenslauf", "Englisch (C1)")), List.of(),
                "Quantis Software GmbH", "Senior Fullstack Entwickler (m/w/d)",
                List.of(new Requirement("Java 17+ & Spring Boot", true), new Requirement("Kubernetes / Docker", false)),
                List.of("Konzeption neuer Features", "Weiterentwicklung der Microservices", "Code-Reviews"),
                "Anzeigentext", Tone.FORMAL, length);
    }
}
