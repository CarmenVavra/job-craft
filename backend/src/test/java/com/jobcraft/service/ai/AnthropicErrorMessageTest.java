package com.jobcraft.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import org.junit.jupiter.api.Test;

class AnthropicErrorMessageTest {

    private static <T extends AnthropicException> T error(Class<T> type, String message) {
        T e = mock(type);
        when(e.getMessage()).thenReturn(message);
        return e;
    }

    @Test
    void explainsMissingCredit() {
        String message = AnthropicCoverLetterAi.userMessage(error(BadRequestException.class,
                "400: Your credit balance is too low to access the Anthropic API."));
        assertThat(message).contains("kein Guthaben").contains("Billing");
    }

    @Test
    void explainsInvalidKey() {
        assertThat(AnthropicCoverLetterAi.userMessage(error(UnauthorizedException.class, "401")))
                .contains("API-Key ist ungültig");
    }

    @Test
    void explainsRateLimit() {
        assertThat(AnthropicCoverLetterAi.userMessage(error(RateLimitException.class, "429")))
                .contains("Limit");
    }

    @Test
    void fallsBackToGenericMessage() {
        assertThat(AnthropicCoverLetterAi.userMessage(error(AnthropicIoException.class, "timeout")))
                .contains("nicht erreichbar");
    }
}
