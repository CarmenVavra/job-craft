package com.jobcraft.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobcraft.config.SecurityConfig;
import com.jobcraft.dto.ApplicationDetailDto;
import com.jobcraft.dto.ApplicationSummaryDto;
import com.jobcraft.dto.LetterMetaDto;
import com.jobcraft.exception.NotFoundException;
import com.jobcraft.model.ApplicationStatus;
import com.jobcraft.model.LetterLength;
import com.jobcraft.model.LetterStyle;
import com.jobcraft.model.Tone;
import com.jobcraft.service.ApplicationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
class ApplicationControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID APP_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ApplicationService applicationService;

    private static RequestPostProcessor user() {
        return jwt().jwt(j -> j.subject(USER_ID.toString()));
    }

    @Test
    void listsApplications() throws Exception {
        when(applicationService.list(USER_ID)).thenReturn(List.of(new ApplicationSummaryDto(APP_ID,
                "Quantis Software GmbH", "Senior Fullstack Entwickler", ApplicationStatus.DRAFT, Instant.now(), 2)));

        mvc.perform(get("/api/applications").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyName").value("Quantis Software GmbH"))
                .andExpect(jsonPath("$[0].latestVersion").value(2));
    }

    @Test
    void startsAnalysis() throws Exception {
        when(applicationService.create(eq(USER_ID), any())).thenReturn(detail(ApplicationStatus.ANALYZING));

        mvc.perform(post("/api/applications").with(user()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://karriere.example.de/jobs/1\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ANALYZING"));
    }

    @Test
    void rejectsInvalidUrl() throws Exception {
        mvc.perform(post("/api/applications").with(user()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"kein-link\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bitte gib einen gültigen Link ein (beginnend mit https://)."));
    }

    @Test
    void returnsNotFoundForForeignApplication() throws Exception {
        when(applicationService.get(USER_ID, APP_ID)).thenThrow(new NotFoundException("Bewerbung nicht gefunden."));

        mvc.perform(get("/api/applications/" + APP_ID).with(user()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Bewerbung nicht gefunden."));
    }

    @Test
    void sendsChatMessage() throws Exception {
        when(applicationService.chat(USER_ID, APP_ID, "Einleitung kürzen")).thenReturn(detail(ApplicationStatus.DRAFT));

        mvc.perform(post("/api/applications/" + APP_ID + "/chat").with(user())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Einleitung kürzen\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void rejectsEmptyChatMessage() throws Exception {
        mvc.perform(post("/api/applications/" + APP_ID + "/chat").with(user())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void savesManuallyEditedParagraphs() throws Exception {
        when(applicationService.updateParagraphs(USER_ID, APP_ID, List.of("Eins", "Zwei")))
                .thenReturn(detail(ApplicationStatus.DRAFT));

        mvc.perform(put("/api/applications/" + APP_ID + "/paragraphs").with(user())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paragraphs\":[\"Eins\",\"Zwei\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsEmptyParagraphs() throws Exception {
        mvc.perform(put("/api/applications/" + APP_ID + "/paragraphs").with(user())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paragraphs\":[\"Eins\",\" \"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Absätze dürfen nicht leer sein."));
        mvc.perform(put("/api/applications/" + APP_ID + "/paragraphs").with(user())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paragraphs\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void downloadsPdf() throws Exception {
        when(applicationService.exportPdf(USER_ID, APP_ID, 2, LetterStyle.MODERN, true, false))
                .thenReturn(new ApplicationService.PdfFile("Bewerbung_Quantis_Hoffmann.pdf", new byte[] {1, 2, 3}));

        mvc.perform(get("/api/applications/" + APP_ID + "/pdf").with(user())
                        .param("version", "2").param("template", "MODERN").param("attachCv", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("Bewerbung_Quantis_Hoffmann.pdf")));
    }

    private static ApplicationDetailDto detail(ApplicationStatus status) {
        return new ApplicationDetailDto(APP_ID, "https://karriere.example.de/jobs/1", null, null, null, null, List.of(),
                List.of(), List.of(), Tone.FORMAL, LetterLength.STANDARD, status, 0, null, Instant.now(),
                new LetterMetaDto("", "", List.of(), "", "", "", ""), List.of(), List.of());
    }
}
