package com.jobcraft.service;

import com.jobcraft.dto.ApplicationDetailDto;
import com.jobcraft.dto.ApplicationSummaryDto;
import com.jobcraft.dto.ChatMessageDto;
import com.jobcraft.dto.CoverLetterDto;
import com.jobcraft.dto.GenerateRequest;
import com.jobcraft.dto.LetterMetaDto;
import com.jobcraft.dto.RequirementDto;
import com.jobcraft.exception.BadRequestException;
import com.jobcraft.exception.ConflictException;
import com.jobcraft.exception.NotFoundException;
import com.jobcraft.model.Application;
import com.jobcraft.model.ApplicationStatus;
import com.jobcraft.model.ChatMessage;
import com.jobcraft.model.ChatRole;
import com.jobcraft.model.CoverLetter;
import com.jobcraft.model.Document;
import com.jobcraft.model.DocumentCategory;
import com.jobcraft.model.DocumentStatus;
import com.jobcraft.model.LetterStyle;
import com.jobcraft.model.Profile;
import com.jobcraft.model.Requirement;
import com.jobcraft.repository.ApplicationRepository;
import com.jobcraft.repository.ChatMessageRepository;
import com.jobcraft.repository.CoverLetterRepository;
import com.jobcraft.repository.DocumentRepository;
import com.jobcraft.repository.LatestVersion;
import com.jobcraft.repository.ProfileNoteRepository;
import com.jobcraft.repository.ProfileRepository;
import com.jobcraft.service.ai.CoverLetterAi;
import com.jobcraft.service.ai.GenerationContext;
import com.jobcraft.service.ai.LetterRevision;
import com.jobcraft.service.pdf.PdfGenerationService;
import com.jobcraft.service.scraping.UrlGuard;
import com.jobcraft.service.storage.StorageService;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Geschäftslogik rund um Bewerbungen: Analyse starten, Entwurf generieren, im Chat überarbeiten, PDF exportieren.
 * KI- und Browser-Aufrufe laufen bewusst außerhalb von Datenbanktransaktionen.
 */
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final Set<ApplicationStatus> LISTED = EnumSet.of(ApplicationStatus.DRAFT, ApplicationStatus.SENT,
            ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED);
    private static final Set<ApplicationStatus> WRITABLE = EnumSet.of(ApplicationStatus.ANALYZED,
            ApplicationStatus.DRAFT, ApplicationStatus.SENT, ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED);
    private static final Map<DocumentCategory, String> CATEGORY_LABELS = Map.of(
            DocumentCategory.CV, "Lebenslauf", DocumentCategory.SKILLS, "Skill-Profil",
            DocumentCategory.CERTIFICATE, "Zeugnis", DocumentCategory.OTHER, "Sonstiges");

    private final ApplicationRepository applicationRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DocumentRepository documentRepository;
    private final ProfileNoteRepository noteRepository;
    private final ProfileRepository profileRepository;
    private final ApplicationAnalysisWorker analysisWorker;
    private final LetterFormatter letterFormatter;
    private final CoverLetterAi ai;
    private final PdfGenerationService pdfGenerationService;
    private final StorageService storageService;
    private final TransactionTemplate tx;

    /**
     * Fertiges PDF.
     *
     * @param fileName Dateiname
     * @param content  Inhalt
     */
    public record PdfFile(String fileName, byte[] content) {
    }

    /**
     * Legt die Bewerbung an und startet die asynchrone Analyse der Stellenanzeige.
     *
     * @param userId Benutzer
     * @param url    URL der Stellenanzeige
     * @return Bewerbung im Status {@code ANALYZING}
     */
    public ApplicationDetailDto create(UUID userId, String url) {
        UrlGuard.validate(url);
        Application app = new Application();
        app.setUserId(userId);
        app.setJobUrl(url.trim());
        app.setStatus(ApplicationStatus.ANALYZING);
        app = applicationRepository.save(app);
        analysisWorker.analyze(app.getId());
        return toDetail(app, null, List.of(), List.of());
    }

    /**
     * @param userId Benutzer
     * @return Bewerbungen mit mindestens einem Anschreiben, neueste zuerst
     */
    public List<ApplicationSummaryDto> list(UUID userId) {
        return tx.execute(status -> {
            List<Application> apps = applicationRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(userId, LISTED);
            Map<UUID, Integer> versions = apps.isEmpty() ? Map.of()
                    : coverLetterRepository.findLatestVersions(apps.stream().map(Application::getId).toList())
                    .stream().collect(Collectors.toMap(LatestVersion::applicationId, LatestVersion::latestVersion));
            return apps.stream().map(a -> new ApplicationSummaryDto(a.getId(), a.getCompanyName(), a.getJobTitle(),
                    a.getStatus(), a.getCreatedAt(), versions.getOrDefault(a.getId(), 0))).toList();
        });
    }

    /**
     * @param userId        Benutzer
     * @param applicationId Bewerbung
     * @return vollständiger Zustand inkl. Versionen und Chat
     */
    public ApplicationDetailDto get(UUID userId, UUID applicationId) {
        return tx.execute(status -> detail(load(userId, applicationId)));
    }

    /**
     * Übernimmt die geprüften Eckdaten und erzeugt einen (neuen) Entwurf.
     *
     * @param userId        Benutzer
     * @param applicationId Bewerbung
     * @param request       Eckdaten, Kontext-Dokumente, Tonalität, Länge
     * @return Zustand inkl. neuer Version
     */
    public ApplicationDetailDto generate(UUID userId, UUID applicationId, GenerateRequest request) {
        GenerationContext context = tx.execute(status -> {
            Application app = load(userId, applicationId);
            if (!WRITABLE.contains(app.getStatus())) {
                throw new ConflictException("Die Stellenanzeige ist noch nicht analysiert.");
            }
            app.setCompanyName(request.companyName().trim());
            app.setJobTitle(request.jobTitle().trim());
            app.setContactPerson(trimToNull(request.contactPerson()));
            app.setRecipientAddress(trimToNull(request.recipientAddress()));
            if (request.requirements() != null) {
                app.setRequirements(new ArrayList<>(request.requirements().stream()
                        .map(r -> new Requirement(r.text().trim(), r.matched())).toList()));
            }
            if (request.tasks() != null) {
                app.setTasks(new ArrayList<>(request.tasks().stream().filter(t -> t != null && !t.isBlank()).toList()));
            }
            List<UUID> docIds = request.documentIds() == null ? List.of()
                    : new ArrayList<>(new LinkedHashSet<>(request.documentIds()));
            app.setContextDocumentIds(new ArrayList<>(docIds));
            app.setTone(request.tone());
            app.setLetterLength(request.length());
            return buildContext(app);
        });

        List<String> paragraphs = ai.generate(context);

        return tx.execute(status -> {
            Application app = load(userId, applicationId);
            int version = nextVersion(app.getId());
            saveVersion(app.getId(), version, paragraphs, List.of());
            List<String> sources = new ArrayList<>(context.documents().stream()
                    .map(GenerationContext.DocumentText::category).distinct().toList());
            if (!context.notes().isEmpty()) {
                sources.add(context.notes().size() == 1 ? "1 weitere Angabe"
                        : context.notes().size() + " weitere Angaben");
            }
            String basis = sources.isEmpty() ? "deine Briefkopf-Daten" : String.join(", ", sources);
            String text = version == 1
                    ? "Dein erster Entwurf ist fertig. Grundlage: " + basis + " und die " + context.requirements().size()
                    + " Anforderungen der Anzeige. Was soll ich anpassen?"
                    : "Ich habe einen neuen Entwurf mit den aktuellen Einstellungen erstellt. Was soll ich anpassen?";
            saveMessage(app.getId(), ChatRole.ASSISTANT, text, "v" + version + " erstellt", version);
            if (app.getStatus() == ApplicationStatus.ANALYZED) {
                app.setStatus(ApplicationStatus.DRAFT);
            }
            return detail(app);
        });
    }

    /**
     * Überarbeitet das Anschreiben gemäß Chat-Nachricht; jede Antwort erzeugt eine neue Version.
     *
     * @param userId        Benutzer
     * @param applicationId Bewerbung
     * @param message       Änderungswunsch
     * @return Zustand inkl. neuer Version und Antwort
     */
    public ApplicationDetailDto chat(UUID userId, UUID applicationId, String message) {
        record Input(GenerationContext context, List<String> current, int version) {
        }
        Input input = tx.execute(status -> {
            Application app = load(userId, applicationId);
            CoverLetter latest = coverLetterRepository.findFirstByApplicationIdOrderByVersionDesc(app.getId())
                    .orElseThrow(() -> new ConflictException("Es gibt noch keinen Entwurf."));
            return new Input(buildContext(app), latest.getParagraphs(), latest.getVersion());
        });

        LetterRevision revision = ai.revise(input.context(), input.current(), message.trim());

        return tx.execute(status -> {
            Application app = load(userId, applicationId);
            int version = nextVersion(app.getId());
            saveMessage(app.getId(), ChatRole.USER, message.trim(), null, null);
            saveVersion(app.getId(), version, revision.paragraphs(), revision.changedParagraphs());
            saveMessage(app.getId(), ChatRole.ASSISTANT, revision.reply(),
                    "v" + version + " · " + changedLabel(revision.changedParagraphs()), version);
            return detail(app);
        });
    }

    /**
     * Speichert manuell bearbeitete Absätze (Ändern oder Löschen im Editor) als neue Version.
     *
     * @param userId        Benutzer
     * @param applicationId Bewerbung
     * @param paragraphs    vollständige neue Absätze
     * @return Zustand inkl. neuer Version
     */
    public ApplicationDetailDto updateParagraphs(UUID userId, UUID applicationId, List<String> paragraphs) {
        List<String> next = paragraphs.stream().map(String::trim).toList();
        return tx.execute(status -> {
            Application app = load(userId, applicationId);
            CoverLetter latest = coverLetterRepository.findFirstByApplicationIdOrderByVersionDesc(app.getId())
                    .orElseThrow(() -> new ConflictException("Es gibt noch keinen Entwurf."));
            List<String> current = latest.getParagraphs();
            if (current.equals(next)) {
                throw new BadRequestException("Es wurde nichts geändert.");
            }
            int deleted = deletedIndex(current, next);
            List<Integer> changed = deleted >= 0 ? List.of() : IntStream.range(0, next.size())
                    .filter(i -> i >= current.size() || !next.get(i).equals(current.get(i))).boxed().toList();
            String label = deleted >= 0 ? "Absatz " + (deleted + 1) + " gelöscht"
                    : changedLabel(changed).replace("geändert", "bearbeitet");
            int version = latest.getVersion() + 1;
            saveVersion(app.getId(), version, next, changed);
            saveMessage(app.getId(), ChatRole.SYSTEM, deleted >= 0
                    ? "Du hast Absatz " + (deleted + 1) + " gelöscht."
                    : "Du hast den Text direkt bearbeitet.", "v" + version + " · " + label, version);
            return detail(app);
        });
    }

    /**
     * Setzt eine eigene Anrede oder stellt die automatische wieder her. Die Anrede gilt für alle Versionen.
     *
     * @param userId        Benutzer
     * @param applicationId Bewerbung
     * @param salutation    eigene Anrede; leer oder {@code null} = automatisch aus der Ansprechperson
     * @return Zustand inkl. neuer Briefdaten
     */
    public ApplicationDetailDto updateSalutation(UUID userId, UUID applicationId, String salutation) {
        String next = trimToNull(salutation);
        return tx.execute(status -> {
            Application app = load(userId, applicationId);
            if (Objects.equals(next, app.getSalutation())) {
                return detail(app);
            }
            app.setSalutation(next);
            String text = next == null
                    ? "Die Anrede wird wieder automatisch aus der Ansprechperson gebildet."
                    : "Du hast die Anrede geändert.";
            saveMessage(app.getId(), ChatRole.SYSTEM, text, "Anrede · " + letterFormatter.format(null, app)
                    .salutation(), null);
            return detail(app);
        });
    }

    /**
     * @return Index des entfernten Absatzes, wenn {@code next} genau {@code current} ohne einen Absatz ist, sonst -1
     */
    private static int deletedIndex(List<String> current, List<String> next) {
        if (next.size() != current.size() - 1) {
            return -1;
        }
        for (int i = 0; i < current.size(); i++) {
            List<String> without = new ArrayList<>(current);
            without.remove(i);
            if (without.equals(next)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param userId        Benutzer
     * @param applicationId Bewerbung
     * @param newStatus     neuer Status (nur Entwurf, Versendet, Gespräch, Abgesagt)
     * @return aktualisierte Bewerbung
     */
    public ApplicationDetailDto updateStatus(UUID userId, UUID applicationId, ApplicationStatus newStatus) {
        if (!LISTED.contains(newStatus)) {
            throw new BadRequestException("Dieser Status kann nicht manuell gesetzt werden.");
        }
        return tx.execute(status -> {
            Application app = load(userId, applicationId);
            if (!LISTED.contains(app.getStatus())) {
                throw new ConflictException("Für diese Bewerbung gibt es noch kein Anschreiben.");
            }
            app.setStatus(newStatus);
            return detail(app);
        });
    }

    /**
     * Löscht Bewerbung, Versionen und Chat.
     *
     * @param userId        Benutzer
     * @param applicationId Bewerbung
     */
    public void delete(UUID userId, UUID applicationId) {
        tx.executeWithoutResult(status -> applicationRepository.delete(load(userId, applicationId)));
    }

    /**
     * Erzeugt das PDF einer Version.
     *
     * @param userId        Benutzer
     * @param applicationId Bewerbung
     * @param version       Version (null = neueste)
     * @param style         Vorlage (null = Profil-Einstellung)
     * @param attachCv      Lebenslauf anhängen
     * @param attachCerts   Zeugnisse anhängen
     * @return PDF inkl. Dateiname
     */
    public PdfFile exportPdf(UUID userId, UUID applicationId, Integer version, LetterStyle style, boolean attachCv,
                             boolean attachCerts) {
        record Input(PdfGenerationService.LetterData data, List<String> attachmentKeys, String fileName) {
        }
        Input input = tx.execute(status -> {
            Application app = load(userId, applicationId);
            CoverLetter letter = (version == null
                    ? coverLetterRepository.findFirstByApplicationIdOrderByVersionDesc(app.getId())
                    : coverLetterRepository.findByApplicationIdAndVersion(app.getId(), version))
                    .orElseThrow(() -> new NotFoundException("Version nicht gefunden."));
            Profile profile = profileRepository.findByUserId(userId)
                    .orElseThrow(() -> new NotFoundException("Profil nicht gefunden."));
            LetterMetaDto meta = letterFormatter.format(profile, app);
            LetterStyle effective = style != null ? style : profile.getLetterStyle();
            Set<DocumentCategory> categories = EnumSet.noneOf(DocumentCategory.class);
            if (attachCv) {
                categories.add(DocumentCategory.CV);
            }
            if (attachCerts) {
                categories.add(DocumentCategory.CERTIFICATE);
            }
            List<String> keys = categories.isEmpty() ? List.of()
                    : documentRepository.findByUserIdAndCategoryInAndStatus(userId, categories, DocumentStatus.READY)
                    .stream()
                    .filter(d -> "application/pdf".equals(d.getContentType()))
                    .sorted((a, b) -> Integer.compare(a.getCategory().ordinal(), b.getCategory().ordinal()))
                    .map(Document::getStoragePath).toList();
            return new Input(new PdfGenerationService.LetterData(profile, meta, letter.getParagraphs(), effective),
                    keys, fileName(app.getCompanyName(), profile.getFullName()));
        });
        List<byte[]> attachments = input.attachmentKeys().stream().map(storageService::load).toList();
        return new PdfFile(input.fileName(), pdfGenerationService.render(input.data(), attachments));
    }

    /**
     * Baut den Dateinamen {@code Bewerbung_{Unternehmen}_{Nachname}.pdf}.
     *
     * @param company  Unternehmen
     * @param fullName Name des Bewerbers
     * @return Dateiname
     */
    public static String fileName(String company, String fullName) {
        String c = company == null ? "" : company.replaceAll("[^A-Za-z0-9ÄÖÜäöüß]+", "_").replaceAll("^_+|_+$", "");
        String[] names = fullName == null ? new String[0] : fullName.trim().split("\\s+");
        String last = names.length == 0 ? "" : names[names.length - 1].replaceAll("[^A-Za-z0-9ÄÖÜäöüß-]+", "");
        return "Bewerbung" + (c.isEmpty() ? "" : "_" + c) + (last.isEmpty() ? "" : "_" + last) + ".pdf";
    }

    private GenerationContext buildContext(Application app) {
        Profile profile = profileRepository.findByUserId(app.getUserId()).orElse(null);
        List<Document> docs = app.getContextDocumentIds() == null || app.getContextDocumentIds().isEmpty() ? List.of()
                : documentRepository.findByUserIdAndIdIn(app.getUserId(), app.getContextDocumentIds());
        Map<UUID, Document> byId = docs.stream().collect(Collectors.toMap(Document::getId, Function.identity()));
        List<GenerationContext.DocumentText> documents = app.getContextDocumentIds() == null ? List.of()
                : app.getContextDocumentIds().stream().map(byId::get)
                .filter(d -> d != null && d.getStatus() == DocumentStatus.READY && d.getExtractedText() != null)
                .map(d -> new GenerationContext.DocumentText(d.getFileName(), CATEGORY_LABELS.get(d.getCategory()),
                        d.getExtractedText()))
                .toList();
        List<String> customFields = profile == null || profile.getCustomFields() == null ? List.of()
                : profile.getCustomFields().stream()
                .filter(f -> f.label() != null && !f.label().isBlank() && f.value() != null && !f.value().isBlank())
                .map(f -> f.label() + ": " + f.value()).toList();
        List<GenerationContext.NoteText> notes = noteRepository
                .findByUserIdAndIncludedTrueOrderByCreatedAtAsc(app.getUserId()).stream()
                .map(n -> new GenerationContext.NoteText(n.getTitle(), n.getContent())).toList();
        return new GenerationContext(profile == null ? "" : profile.getFullName(), customFields, documents, notes,
                nullToEmpty(app.getCompanyName()), nullToEmpty(app.getJobTitle()),
                app.getRequirements() == null ? List.of() : List.copyOf(app.getRequirements()),
                app.getTasks() == null ? List.of() : List.copyOf(app.getTasks()),
                app.getRawJobText(), app.getTone(), app.getLetterLength());
    }

    private ApplicationDetailDto detail(Application app) {
        Profile profile = profileRepository.findByUserId(app.getUserId()).orElse(null);
        List<CoverLetterDto> versions = coverLetterRepository.findByApplicationIdOrderByVersionAsc(app.getId()).stream()
                .map(c -> new CoverLetterDto(c.getVersion(), c.getParagraphs(),
                        c.getChangedParagraphs() == null ? List.of() : c.getChangedParagraphs(), c.getCreatedAt()))
                .toList();
        List<ChatMessageDto> messages = chatMessageRepository.findByApplicationIdOrderByCreatedAtAscRoleDesc(app.getId())
                .stream().map(m -> new ChatMessageDto(m.getId(), m.getRole(), m.getMessage(), m.getMeta(),
                        m.getCoverLetterVersion(), m.getCreatedAt())).toList();
        return toDetail(app, profile, versions, messages);
    }

    private ApplicationDetailDto toDetail(Application app, Profile profile, List<CoverLetterDto> versions,
                                          List<ChatMessageDto> messages) {
        List<RequirementDto> requirements = app.getRequirements() == null ? List.of()
                : app.getRequirements().stream().map(r -> new RequirementDto(r.text(), r.matched())).toList();
        return new ApplicationDetailDto(app.getId(), app.getJobUrl(), app.getJobTitle(), app.getCompanyName(),
                app.getContactPerson(), app.getRecipientAddress(), app.getSalutation(), requirements,
                app.getTasks() == null ? List.of() : app.getTasks(),
                app.getContextDocumentIds() == null ? List.of() : app.getContextDocumentIds(),
                app.getTone(), app.getLetterLength(), app.getStatus(), app.getAnalysisStep(), app.getErrorMessage(),
                app.getCreatedAt(), letterFormatter.format(profile, app), versions, messages);
    }

    private Application load(UUID userId, UUID applicationId) {
        return applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new NotFoundException("Bewerbung nicht gefunden."));
    }

    private int nextVersion(UUID applicationId) {
        return coverLetterRepository.findFirstByApplicationIdOrderByVersionDesc(applicationId)
                .map(CoverLetter::getVersion).orElse(0) + 1;
    }

    private void saveVersion(UUID applicationId, int version, List<String> paragraphs, List<Integer> changed) {
        CoverLetter letter = new CoverLetter();
        letter.setApplicationId(applicationId);
        letter.setVersion(version);
        letter.setParagraphs(new ArrayList<>(paragraphs));
        letter.setChangedParagraphs(new ArrayList<>(changed));
        coverLetterRepository.save(letter);
    }

    private void saveMessage(UUID applicationId, ChatRole role, String text, String meta, Integer version) {
        ChatMessage msg = new ChatMessage();
        msg.setApplicationId(applicationId);
        msg.setRole(role);
        msg.setMessage(text);
        msg.setMeta(meta);
        msg.setCoverLetterVersion(version);
        chatMessageRepository.saveAndFlush(msg);
    }

    private static String changedLabel(List<Integer> changed) {
        if (changed.isEmpty()) {
            return "keine Änderung";
        }
        String numbers = changed.stream().sorted().map(i -> String.valueOf(i + 1)).collect(Collectors.joining(", "));
        return (changed.size() == 1 ? "Absatz " : "Absätze ") + numbers + " geändert";
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
