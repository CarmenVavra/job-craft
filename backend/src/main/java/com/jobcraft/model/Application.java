package com.jobcraft.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * Eine konkrete Bewerbung auf eine Stellenanzeige.
 */
@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "job_url", nullable = false)
    private String jobUrl;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "recipient_address")
    private String recipientAddress;

    @Column(name = "raw_job_text")
    private String rawJobText;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<Requirement> requirements = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> tasks = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_document_ids")
    private List<UUID> contextDocumentIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tone tone = Tone.FORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "letter_length", nullable = false)
    private LetterLength letterLength = LetterLength.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.ANALYZING;

    @Column(name = "analysis_step", nullable = false)
    private int analysisStep;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
