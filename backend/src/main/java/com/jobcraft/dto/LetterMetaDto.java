package com.jobcraft.dto;

import java.util.List;

/**
 * Aus Profil und Bewerbung abgeleitete Briefbestandteile (DIN 5008).
 */
public record LetterMetaDto(String senderName, String senderLine, List<String> recipientLines, String dateLine,
                            String subject, String salutation, String closing) {
}
