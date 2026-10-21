package com.jobcraft.controller;

import com.jobcraft.dto.ApplicationDetailDto;
import com.jobcraft.dto.ApplicationSummaryDto;
import com.jobcraft.dto.ChatRequest;
import com.jobcraft.dto.CreateApplicationRequest;
import com.jobcraft.dto.GenerateRequest;
import com.jobcraft.dto.UpdateParagraphsRequest;
import com.jobcraft.dto.UpdateSalutationRequest;
import com.jobcraft.dto.UpdateStatusRequest;
import com.jobcraft.model.LetterStyle;
import com.jobcraft.service.ApplicationService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bewerbungen: Analyse der Stellenanzeige, Entwurf, Chat-Überarbeitung und PDF-Export.
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<List<ApplicationSummaryDto>> list(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(applicationService.list(CurrentUser.id(jwt)));
    }

    /**
     * Nimmt die Stellen-URL entgegen und startet Scraping und Analyse; der Fortschritt wird per GET abgefragt.
     */
    @PostMapping
    public ResponseEntity<ApplicationDetailDto> create(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(applicationService.create(CurrentUser.id(jwt),
                request.url()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDetailDto> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ResponseEntity.ok(applicationService.get(CurrentUser.id(jwt), id));
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<ApplicationDetailDto> generate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                         @Valid @RequestBody GenerateRequest request) {
        return ResponseEntity.ok(applicationService.generate(CurrentUser.id(jwt), id, request));
    }

    @PostMapping("/{id}/chat")
    public ResponseEntity<ApplicationDetailDto> chat(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                     @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(applicationService.chat(CurrentUser.id(jwt), id, request.message()));
    }

    /**
     * Speichert direkt im Editor bearbeitete oder gelöschte Absätze als neue Version.
     */
    @PutMapping("/{id}/paragraphs")
    public ResponseEntity<ApplicationDetailDto> updateParagraphs(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                                 @Valid @RequestBody UpdateParagraphsRequest request) {
        return ResponseEntity.ok(applicationService.updateParagraphs(CurrentUser.id(jwt), id, request.paragraphs()));
    }

    @PutMapping("/{id}/salutation")
    public ResponseEntity<ApplicationDetailDto> updateSalutation(@AuthenticationPrincipal Jwt jwt,
                                                                 @PathVariable UUID id,
                                                                 @Valid @RequestBody UpdateSalutationRequest request) {
        return ResponseEntity.ok(applicationService.updateSalutation(CurrentUser.id(jwt), id, request.salutation()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationDetailDto> updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                             @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(applicationService.updateStatus(CurrentUser.id(jwt), id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        applicationService.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                      @RequestParam(required = false) Integer version,
                                      @RequestParam(required = false) LetterStyle template,
                                      @RequestParam(defaultValue = "false") boolean attachCv,
                                      @RequestParam(defaultValue = "false") boolean attachCertificates) {
        ApplicationService.PdfFile file = applicationService.exportPdf(CurrentUser.id(jwt), id, version, template,
                attachCv, attachCertificates);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.content());
    }
}
