package com.jobcraft.service;

import com.jobcraft.dto.LetterMetaDto;
import com.jobcraft.model.Application;
import com.jobcraft.model.Profile;
import com.jobcraft.service.ai.TemplateCoverLetterAi;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Leitet die festen Briefbestandteile (Absenderzeile, Empfänger, Datum, Betreff, Anrede) aus Profil und Bewerbung ab.
 * Wird für die Editor-Vorschau und das PDF gleichermaßen genutzt.
 */
@Component
@RequiredArgsConstructor
public class LetterFormatter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN);
    private static final String FALLBACK_SALUTATION = "Sehr geehrte Damen und Herren,";

    private final Clock clock;

    /**
     * @param profile     Briefkopf (darf {@code null} sein)
     * @param application Bewerbung
     * @return Briefbestandteile
     */
    public LetterMetaDto format(Profile profile, Application application) {
        String name = profile == null ? "" : nullToEmpty(profile.getFullName());
        String zipCity = profile == null ? "" : joinNonBlank(" ", profile.getZipCode(), profile.getCity());
        String senderLine = profile == null ? "" : joinNonBlank(" · ", name, profile.getAddressLine1(), zipCity);

        List<String> recipient = new ArrayList<>();
        addIfPresent(recipient, application.getCompanyName());
        addIfPresent(recipient, application.getContactPerson());
        if (application.getRecipientAddress() != null) {
            Arrays.stream(application.getRecipientAddress().split("\\s*,\\s*")).forEach(l -> addIfPresent(recipient, l));
        }

        String date = LocalDate.now(clock).format(DATE);
        String city = profile == null ? "" : nullToEmpty(profile.getCity());
        String dateLine = city.isEmpty() ? date : city + ", " + date;

        String subject = "Bewerbung als " + TemplateCoverLetterAi.cleanRole(application.getJobTitle());
        return new LetterMetaDto(name, senderLine, recipient, dateLine, subject.trim(),
                application.getSalutation() == null || application.getSalutation().isBlank()
                        ? salutation(application.getContactPerson()) : application.getSalutation().trim(),
                "Mit freundlichen Grüßen");
    }

    /**
     * Leitet die Anrede aus der Ansprechperson ab, z. B. „Frau Dr. Miriam Kessler“ → „Sehr geehrte Frau Dr. Kessler,“.
     *
     * @param contact Ansprechpartner:in inkl. Anrede
     * @return Anrede mit Komma
     */
    public static String salutation(String contact) {
        if (contact == null || contact.isBlank()) {
            return FALLBACK_SALUTATION;
        }
        String[] parts = contact.trim().split("\\s+");
        if (parts.length < 2) {
            return FALLBACK_SALUTATION;
        }
        String prefix;
        if ("Frau".equals(parts[0])) {
            prefix = "Sehr geehrte Frau";
        } else if ("Herr".equals(parts[0])) {
            prefix = "Sehr geehrter Herr";
        } else {
            return FALLBACK_SALUTATION;
        }
        String titles = Stream.of(parts).skip(1).limit(parts.length - 2L)
                .filter(p -> p.matches("(?i)(dr|prof)\\.?(-ing\\.)?"))
                .map(p -> p.endsWith(".") ? p : p + ".")
                .collect(Collectors.joining(" "));
        String lastName = parts[parts.length - 1];
        return prefix + (titles.isEmpty() ? "" : " " + titles) + " " + lastName + ",";
    }

    private static void addIfPresent(List<String> lines, String value) {
        if (value != null && !value.isBlank()) {
            lines.add(value.trim());
        }
    }

    private static String joinNonBlank(String separator, String... values) {
        return Stream.of(values).filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(separator));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
