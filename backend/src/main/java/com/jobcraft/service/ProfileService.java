package com.jobcraft.service;

import com.jobcraft.dto.CustomFieldDto;
import com.jobcraft.dto.ProfileDto;
import com.jobcraft.exception.NotFoundException;
import com.jobcraft.model.CustomField;
import com.jobcraft.model.Profile;
import com.jobcraft.repository.ProfileRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verwaltung der Briefkopf-Stammdaten.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    /**
     * @param userId Benutzer
     * @return Profil-Entity, falls vorhanden
     */
    @Transactional(readOnly = true)
    public Optional<Profile> findEntity(UUID userId) {
        return profileRepository.findByUserId(userId);
    }

    /**
     * @param userId Benutzer
     * @return Briefkopf-Daten
     */
    @Transactional(readOnly = true)
    public ProfileDto get(UUID userId) {
        return toDto(load(userId));
    }

    /**
     * Speichert alle Briefkopf-Daten. Leere eigene Felder werden verworfen.
     *
     * @param userId Benutzer
     * @param dto    neue Daten
     * @return gespeicherte Daten
     */
    @Transactional
    public ProfileDto update(UUID userId, ProfileDto dto) {
        Profile profile = load(userId);
        profile.setFullName(dto.fullName().trim());
        profile.setAddressLine1(trim(dto.street()));
        profile.setZipCode(trim(dto.zipCode()));
        profile.setCity(trim(dto.city()));
        profile.setPhone(trim(dto.phone()));
        profile.setEmail(trim(dto.email()));
        profile.setLinkedin(trim(dto.linkedin()));
        profile.setGithub(trim(dto.github()));
        profile.setLetterStyle(dto.letterStyle());
        List<CustomField> fields = new ArrayList<>();
        if (dto.customFields() != null) {
            for (CustomFieldDto field : dto.customFields()) {
                String label = trim(field.label());
                String value = trim(field.value());
                if (label != null || value != null) {
                    fields.add(new CustomField(label == null ? "" : label, value == null ? "" : value));
                }
            }
        }
        profile.setCustomFields(fields);
        return toDto(profileRepository.save(profile));
    }

    private Profile load(UUID userId) {
        return profileRepository.findByUserId(userId).orElseThrow(() -> new NotFoundException("Profil nicht gefunden."));
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static ProfileDto toDto(Profile p) {
        List<CustomFieldDto> fields = p.getCustomFields() == null ? List.of()
                : p.getCustomFields().stream().map(f -> new CustomFieldDto(f.label(), f.value())).toList();
        return new ProfileDto(p.getFullName(), p.getAddressLine1(), p.getZipCode(), p.getCity(), p.getPhone(),
                p.getEmail(), p.getLinkedin(), p.getGithub(), p.getLetterStyle(), fields);
    }
}
