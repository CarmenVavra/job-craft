package com.jobcraft.controller;

import com.jobcraft.dto.ProfileNoteDto;
import com.jobcraft.dto.ProfileNoteRequest;
import com.jobcraft.service.ProfileNoteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Weitere Angaben zur Person („Über mich“), z. B. Privatprojekte, die nicht im Lebenslauf stehen.
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class ProfileNoteController {

    private final ProfileNoteService noteService;

    @GetMapping
    public ResponseEntity<List<ProfileNoteDto>> list(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(noteService.list(CurrentUser.id(jwt)));
    }

    @PostMapping
    public ResponseEntity<ProfileNoteDto> create(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody ProfileNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.create(CurrentUser.id(jwt), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileNoteDto> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                 @Valid @RequestBody ProfileNoteRequest request) {
        return ResponseEntity.ok(noteService.update(CurrentUser.id(jwt), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        noteService.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
