package com.jobcraft.service;

import com.jobcraft.exception.ApiException;
import com.jobcraft.model.Application;
import com.jobcraft.model.ApplicationStatus;
import com.jobcraft.model.Document;
import com.jobcraft.model.DocumentCategory;
import com.jobcraft.model.DocumentStatus;
import com.jobcraft.model.Requirement;
import com.jobcraft.repository.ApplicationRepository;
import com.jobcraft.repository.DocumentRepository;
import com.jobcraft.repository.ProfileNoteRepository;
import com.jobcraft.service.ai.CoverLetterAi;
import com.jobcraft.service.ai.JobExtraction;
import com.jobcraft.service.scraping.HtmlCleaner;
import com.jobcraft.service.scraping.JobScraperService;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Analysiert eine Stellenanzeige asynchron in vier Schritten. Der Fortschritt steht in
 * {@link Application#getAnalysisStep()} und wird vom Frontend abgefragt:
 * 0 Seite abrufen · 1 HTML bereinigen · 2 Eckdaten extrahieren · 3 Profilabgleich · 4 fertig.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationAnalysisWorker {

    /** Anzahl der Analyseschritte. */
    public static final int STEPS = 4;

    private static final Set<DocumentCategory> PROFILE_CATEGORIES = EnumSet.of(DocumentCategory.CV,
            DocumentCategory.SKILLS);
    private static final Set<DocumentCategory> DEFAULT_CONTEXT = EnumSet.of(DocumentCategory.CV,
            DocumentCategory.SKILLS, DocumentCategory.CERTIFICATE);

    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final ProfileNoteRepository noteRepository;
    private final JobScraperService scraperService;
    private final CoverLetterAi ai;

    /**
     * Führt die Analyse aus. Fehler werden als {@link ApplicationStatus#ANALYSIS_FAILED} gespeichert.
     *
     * @param applicationId Bewerbung
     */
    @Async
    public void analyze(UUID applicationId) {
        Application app = applicationRepository.findById(applicationId).orElse(null);
        if (app == null) {
            return;
        }
        try {
            String html = scraperService.fetchHtml(app.getJobUrl());
            app = step(app, 1);

            HtmlCleaner.CleanedPage page = HtmlCleaner.clean(html, app.getJobUrl());
            if (page.text().isBlank()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Auf der Seite wurde kein Text einer Stellenanzeige gefunden.");
            }
            app.setRawJobText(page.text());
            app = step(app, 2);

            JobExtraction job = ai.extractJob(page.text(), page.title(), page.siteName());
            app.setCompanyName(job.companyName());
            app.setJobTitle(job.jobTitle().isBlank() ? page.title() : job.jobTitle());
            app.setContactPerson(job.contactPerson());
            app.setRecipientAddress(job.recipientAddress());
            app.setTasks(new ArrayList<>(job.tasks()));
            app = step(app, 3);

            List<Document> ready = documentRepository.findByUserIdAndStatus(app.getUserId(), DocumentStatus.READY);
            List<Document> profileDocs = ready.stream().filter(d -> PROFILE_CATEGORIES.contains(d.getCategory()))
                    .toList();
            String applicantText = Stream.concat((profileDocs.isEmpty() ? ready : profileDocs).stream()
                            .map(Document::getExtractedText),
                    noteRepository.findByUserIdAndIncludedTrueOrderByCreatedAtAsc(app.getUserId()).stream()
                            .map(n -> n.getTitle() + ":\n" + n.getContent()))
                    .filter(t -> t != null && !t.isBlank())
                    .collect(Collectors.joining("\n\n"));
            List<Boolean> matches = ai.matchRequirements(job.requirements(), applicantText);
            List<Requirement> requirements = new ArrayList<>();
            for (int i = 0; i < job.requirements().size(); i++) {
                requirements.add(new Requirement(job.requirements().get(i), i < matches.size() && matches.get(i)));
            }
            app.setRequirements(requirements);
            app.setContextDocumentIds(new ArrayList<>(ready.stream()
                    .filter(d -> DEFAULT_CONTEXT.contains(d.getCategory())).map(Document::getId).toList()));
            app.setStatus(ApplicationStatus.ANALYZED);
            step(app, STEPS);
        } catch (AnalysisCancelledException e) {
            log.info("Analyse der Bewerbung {} abgebrochen: Bewerbung wurde gelöscht", applicationId);
        } catch (ApiException e) {
            fail(app, e.getMessage());
        } catch (OptimisticLockingFailureException e) {
            if (applicationRepository.existsById(applicationId)) {
                log.error("Analyse der Bewerbung {} fehlgeschlagen", applicationId, e);
                fail(app, "Die Stellenanzeige konnte nicht analysiert werden.");
            }
        } catch (RuntimeException e) {
            log.error("Analyse der Bewerbung {} fehlgeschlagen", applicationId, e);
            fail(app, "Die Stellenanzeige konnte nicht analysiert werden.");
        }
    }

    private Application step(Application app, int step) {
        ensureNotDeleted(app);
        app.setAnalysisStep(step);
        return applicationRepository.save(app);
    }

    private void fail(Application app, String message) {
        if (!applicationRepository.existsById(app.getId())) {
            return;
        }
        app.setStatus(ApplicationStatus.ANALYSIS_FAILED);
        app.setErrorMessage(message);
        applicationRepository.save(app);
    }

    /** Die Bewerbung kann gelöscht werden, während die Analyse noch läuft – dann wird still abgebrochen. */
    private void ensureNotDeleted(Application app) {
        if (!applicationRepository.existsById(app.getId())) {
            throw new AnalysisCancelledException();
        }
    }

    /** Signalisiert, dass die Bewerbung während der Analyse gelöscht wurde. */
    private static final class AnalysisCancelledException extends RuntimeException {
        AnalysisCancelledException() {
            super(null, null, false, false);
        }
    }
}
