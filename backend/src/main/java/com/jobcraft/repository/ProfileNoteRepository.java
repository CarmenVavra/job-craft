package com.jobcraft.repository;

import com.jobcraft.model.ProfileNote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Zugriff auf die weiteren Angaben zur Person.
 */
public interface ProfileNoteRepository extends JpaRepository<ProfileNote, UUID> {

    List<ProfileNote> findByUserIdOrderByCreatedAtAsc(UUID userId);

    List<ProfileNote> findByUserIdAndIncludedTrueOrderByCreatedAtAsc(UUID userId);

    Optional<ProfileNote> findByIdAndUserId(UUID id, UUID userId);
}
