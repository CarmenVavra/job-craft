package com.jobcraft.repository;

import com.jobcraft.model.Document;
import com.jobcraft.model.DocumentCategory;
import com.jobcraft.model.DocumentStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Zugriff auf hochgeladene Dokumente.
 */
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<Document> findByIdAndUserId(UUID id, UUID userId);

    List<Document> findByUserIdAndStatus(UUID userId, DocumentStatus status);

    List<Document> findByUserIdAndIdIn(UUID userId, Collection<UUID> ids);

    List<Document> findByUserIdAndCategoryInAndStatus(UUID userId, Collection<DocumentCategory> categories,
                                                       DocumentStatus status);
}
