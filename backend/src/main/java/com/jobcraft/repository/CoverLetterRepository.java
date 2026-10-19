package com.jobcraft.repository;

import com.jobcraft.model.CoverLetter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Zugriff auf Anschreiben-Versionen.
 */
public interface CoverLetterRepository extends JpaRepository<CoverLetter, UUID> {

    List<CoverLetter> findByApplicationIdOrderByVersionAsc(UUID applicationId);

    Optional<CoverLetter> findFirstByApplicationIdOrderByVersionDesc(UUID applicationId);

    Optional<CoverLetter> findByApplicationIdAndVersion(UUID applicationId, int version);

    /**
     * Ermittelt die höchste Version für mehrere Bewerbungen in einer Abfrage (vermeidet N+1 in der Übersicht).
     *
     * @param applicationIds Bewerbungen
     * @return höchste Version je Bewerbung (nur Bewerbungen mit mindestens einem Anschreiben)
     */
    @Query("""
            select new com.jobcraft.repository.LatestVersion(c.applicationId, max(c.version))
            from CoverLetter c
            where c.applicationId in :ids
            group by c.applicationId
            """)
    List<LatestVersion> findLatestVersions(@Param("ids") Collection<UUID> applicationIds);
}
