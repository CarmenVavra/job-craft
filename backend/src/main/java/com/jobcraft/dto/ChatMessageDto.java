package com.jobcraft.dto;

import com.jobcraft.model.ChatRole;
import java.time.Instant;
import java.util.UUID;

/**
 * Chat-Nachricht.
 */
public record ChatMessageDto(UUID id, ChatRole role, String message, String meta, Integer coverLetterVersion,
                             Instant createdAt) {
}
