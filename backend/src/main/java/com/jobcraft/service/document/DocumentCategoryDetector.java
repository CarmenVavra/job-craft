package com.jobcraft.service.document;

import com.jobcraft.model.DocumentCategory;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Leitet die Dokument-Kategorie aus dem Dateinamen ab. Die Kategorie kann nach dem Upload geändert werden.
 */
public final class DocumentCategoryDetector {

    private static final Pattern CV = Pattern.compile("lebenslauf|curriculum|\\bcv\\b|resume|vita");
    private static final Pattern SKILLS = Pattern.compile("skill|kompetenz|qualifikation|profil");
    private static final Pattern CERTIFICATE = Pattern.compile("zeugnis|arbeitszeugnis|reference|referenz");

    private DocumentCategoryDetector() {
    }

    /**
     * @param fileName Dateiname
     * @return erkannte Kategorie, sonst {@link DocumentCategory#OTHER}
     */
    public static DocumentCategory detect(String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
        if (CV.matcher(name).find()) {
            return DocumentCategory.CV;
        }
        if (CERTIFICATE.matcher(name).find()) {
            return DocumentCategory.CERTIFICATE;
        }
        if (SKILLS.matcher(name).find()) {
            return DocumentCategory.SKILLS;
        }
        return DocumentCategory.OTHER;
    }
}
