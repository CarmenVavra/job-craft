package com.jobcraft.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobcraft.config.SecurityConfig;
import com.jobcraft.dto.DocumentDto;
import com.jobcraft.exception.BadRequestException;
import com.jobcraft.model.DocumentCategory;
import com.jobcraft.model.DocumentStatus;
import com.jobcraft.service.document.DocumentService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(DocumentController.class)
@Import(SecurityConfig.class)
class DocumentControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DOC_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DocumentService documentService;

    private static RequestPostProcessor user() {
        return jwt().jwt(j -> j.subject(USER_ID.toString()));
    }

    @Test
    void uploadsDocument() throws Exception {
        when(documentService.upload(eq(USER_ID), any())).thenReturn(dto(DocumentStatus.PROCESSING));
        MockMultipartFile file = new MockMultipartFile("file", "Lebenslauf.pdf", "application/pdf", new byte[] {1});

        mvc.perform(multipart("/api/documents").file(file).with(user()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("CV"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void reportsInvalidUpload() throws Exception {
        when(documentService.upload(eq(USER_ID), any()))
                .thenThrow(new BadRequestException("Nur PDF- oder DOCX-Dateien sind erlaubt."));
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[] {1});

        mvc.perform(multipart("/api/documents").file(file).with(user()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nur PDF- oder DOCX-Dateien sind erlaubt."));
    }

    @Test
    void listsDocuments() throws Exception {
        when(documentService.list(USER_ID)).thenReturn(List.of(dto(DocumentStatus.READY)));

        mvc.perform(get("/api/documents").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("Lebenslauf.pdf"));
    }

    @Test
    void changesCategory() throws Exception {
        when(documentService.updateCategory(USER_ID, DOC_ID, DocumentCategory.SKILLS)).thenReturn(dto(DocumentStatus.READY));

        mvc.perform(patch("/api/documents/" + DOC_ID).with(user()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"SKILLS\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void returnsNoContentWithoutPreview() throws Exception {
        when(documentService.renderPreview(USER_ID, DOC_ID)).thenReturn(Optional.empty());

        mvc.perform(get("/api/documents/" + DOC_ID + "/preview").with(user()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletesDocument() throws Exception {
        mvc.perform(delete("/api/documents/" + DOC_ID).with(user())).andExpect(status().isNoContent());
        verify(documentService).delete(USER_ID, DOC_ID);
    }

    private static DocumentDto dto(DocumentStatus status) {
        return new DocumentDto(DOC_ID, "Lebenslauf.pdf", "application/pdf", DocumentCategory.CV, 1024, 1, status,
                Instant.now());
    }
}
