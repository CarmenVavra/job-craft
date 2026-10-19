package com.jobcraft.controller;

import com.jobcraft.dto.DocumentDetailDto;
import com.jobcraft.dto.DocumentDto;
import com.jobcraft.dto.UpdateDocumentRequest;
import com.jobcraft.service.document.DocumentService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload und Verwaltung von Lebenslauf, Skill-Profilen, Zeugnissen &amp; Co.
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public ResponseEntity<List<DocumentDto>> list(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(documentService.list(CurrentUser.id(jwt)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDto> upload(@AuthenticationPrincipal Jwt jwt,
                                              @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.upload(CurrentUser.id(jwt), file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetailDto> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.get(CurrentUser.id(jwt), id));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> file(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        DocumentService.StoredFile file = documentService.loadFile(CurrentUser.id(jwt), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.content());
    }

    @GetMapping(value = "/{id}/preview", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> preview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return documentService.renderPreview(CurrentUser.id(jwt), id)
                .map(png -> ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DocumentDto> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                              @Valid @RequestBody UpdateDocumentRequest request) {
        return ResponseEntity.ok(documentService.updateCategory(CurrentUser.id(jwt), id, request.category()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        documentService.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
