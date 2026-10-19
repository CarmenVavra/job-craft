package com.jobcraft.repository;

import com.jobcraft.model.Application;
import com.jobcraft.model.ApplicationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Zugriff auf Bewerbungen.
 */
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findByIdAndUserId(UUID id, UUID userId);

    List<Application> findByUserIdAndStatusInOrderByCreatedAtDesc(UUID userId, Collection<ApplicationStatus> statuses);
}
