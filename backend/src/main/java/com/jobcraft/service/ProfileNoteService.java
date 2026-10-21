package com.jobcraft.service;

import com.jobcraft.dto.ProfileNoteDto;
import com.jobcraft.dto.ProfileNoteRequest;
import com.jobcraft.exception.NotFoundException;
import com.jobcraft.model.ProfileNote;
import com.jobcraft.repository.ProfileNoteRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verwaltung der weiteren Angaben zur Person, die die KI zusätzlich zu den Dokumenten berücksichtigt.
 */
@Service
@RequiredArgsConstructor
public class ProfileNoteService {

    private final ProfileNoteRepository noteRepository;

    /**
     * @param userId Benutzer
     * @return alle Angaben in Anlagereihenfolge
     */
    @Transactional(readOnly = true)
    public List<ProfileNoteDto> list(UUID userId) {
        return noteRepository.findByUserIdOrderByCreatedAtAsc(userId).stream().map(ProfileNoteService::toDto).toList();
    }

    /**
     * @param userId  Benutzer
     * @param request neue Angabe
     * @return gespeicherte Angabe
     */
    @Transactional
    public ProfileNoteDto create(UUID userId, ProfileNoteRequest request) {
        ProfileNote note = new ProfileNote();
        note.setUserId(userId);
        apply(note, request);
        return toDto(noteRepository.saveAndFlush(note));
    }

    /**
     * @param userId  Benutzer
     * @param noteId  Angabe
     * @param request neue Werte
     * @return gespeicherte Angabe
     */
    @Transactional
    public ProfileNoteDto update(UUID userId, UUID noteId, ProfileNoteRequest request) {
        ProfileNote note = load(userId, noteId);
        apply(note, request);
        return toDto(noteRepository.saveAndFlush(note));
    }

    /**
     * @param userId Benutzer
     * @param noteId Angabe
     */
    @Transactional
    public void delete(UUID userId, UUID noteId) {
        noteRepository.delete(load(userId, noteId));
    }

    private ProfileNote load(UUID userId, UUID noteId) {
        return noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new NotFoundException("Angabe nicht gefunden."));
    }

    private static void apply(ProfileNote note, ProfileNoteRequest request) {
        note.setTitle(request.title().trim());
        note.setContent(request.content().trim());
        note.setIncluded(request.included());
    }

    private static ProfileNoteDto toDto(ProfileNote note) {
        return new ProfileNoteDto(note.getId(), note.getTitle(), note.getContent(), note.isIncluded(),
                note.getCreatedAt(), note.getUpdatedAt());
    }
}
