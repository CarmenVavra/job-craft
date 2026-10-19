package com.jobcraft.repository;

import com.jobcraft.model.ChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Zugriff auf Chat-Nachrichten.
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByApplicationIdOrderByCreatedAtAscRoleDesc(UUID applicationId);
}
