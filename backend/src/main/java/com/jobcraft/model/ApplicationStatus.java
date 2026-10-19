package com.jobcraft.model;

/**
 * Lebenszyklus einer Bewerbung – von der Analyse bis zur Rückmeldung.
 */
public enum ApplicationStatus {
    ANALYZING, ANALYSIS_FAILED, ANALYZED, DRAFT, SENT, INTERVIEW, REJECTED
}
