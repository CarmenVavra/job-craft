package com.jobcraft.config;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.jobcraft.service.ai.AnthropicCoverLetterAi;
import com.jobcraft.service.ai.CoverLetterAi;
import com.jobcraft.service.ai.TemplateCoverLetterAi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wählt die KI-Implementierung: Claude (Anthropic SDK), wenn ein API-Key gesetzt ist, sonst Demo-Modus.
 */
@Slf4j
@Configuration
public class AiConfig {

    @Bean
    public CoverLetterAi coverLetterAi(JobCraftProperties properties) {
        JobCraftProperties.Ai ai = properties.ai();
        if (ai.enabled()) {
            log.info("KI-Modus: Anthropic Claude ({})", ai.model());
            return new AnthropicCoverLetterAi(AnthropicOkHttpClient.builder().apiKey(ai.apiKey()).build(),
                    ai.model(), ai.maxTokens());
        }
        log.info("KI-Modus: Demo (kein ANTHROPIC_API_KEY gesetzt)");
        return new TemplateCoverLetterAi();
    }
}
