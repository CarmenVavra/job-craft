package com.jobcraft.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobcraft.dto.LetterMetaDto;
import com.jobcraft.model.Application;
import com.jobcraft.model.Profile;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LetterFormatterTest {

    private final LetterFormatter formatter = new LetterFormatter(
            Clock.fixed(Instant.parse("2026-10-19T10:00:00Z"), ZoneId.of("Europe/Berlin")));

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Frau Dr. Miriam Kessler|Sehr geehrte Frau Dr. Kessler,",
        "Herr Jonas Albers|Sehr geehrter Herr Albers,",
        "Herr Prof. Dr. Felix Brückner|Sehr geehrter Herr Prof. Dr. Brückner,",
        "Personalabteilung|Sehr geehrte Damen und Herren,",
        "Frau|Sehr geehrte Damen und Herren,"
    })
    void derivesSalutationFromContact(String contact, String expected) {
        assertThat(LetterFormatter.salutation(contact)).isEqualTo(expected);
    }

    @Test
    void fallsBackToNeutralSalutationWithoutContact() {
        assertThat(LetterFormatter.salutation(null)).isEqualTo("Sehr geehrte Damen und Herren,");
        assertThat(LetterFormatter.salutation("  ")).isEqualTo("Sehr geehrte Damen und Herren,");
    }

    @Test
    void buildsLetterPartsFromProfileAndApplication() {
        Profile profile = new Profile();
        profile.setFullName("Lena Hoffmann");
        profile.setAddressLine1("Lindenstraße 12");
        profile.setZipCode("50674");
        profile.setCity("Köln");
        Application app = new Application();
        app.setCompanyName("Quantis Software GmbH");
        app.setJobTitle("Senior Fullstack Entwickler (m/w/d)");
        app.setContactPerson("Frau Dr. Miriam Kessler");
        app.setRecipientAddress("Am Mediapark 8, 50670 Köln");

        LetterMetaDto meta = formatter.format(profile, app);

        assertThat(meta.senderLine()).isEqualTo("Lena Hoffmann · Lindenstraße 12 · 50674 Köln");
        assertThat(meta.recipientLines()).containsExactly("Quantis Software GmbH", "Frau Dr. Miriam Kessler",
                "Am Mediapark 8", "50670 Köln");
        assertThat(meta.dateLine()).isEqualTo("Köln, 19. Oktober 2026");
        assertThat(meta.subject()).isEqualTo("Bewerbung als Senior Fullstack Entwickler");
        assertThat(meta.salutation()).isEqualTo("Sehr geehrte Frau Dr. Kessler,");
    }

    @Test
    void handlesMissingProfile() {
        Application app = new Application();
        app.setJobTitle("Backend Developer");

        LetterMetaDto meta = formatter.format(null, app);

        assertThat(meta.senderLine()).isEmpty();
        assertThat(meta.dateLine()).isEqualTo("19. Oktober 2026");
        assertThat(meta.recipientLines()).isEmpty();
    }
}
