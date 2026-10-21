package com.jobcraft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobcraft.service.pdf.PdfGenerationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Durchläuft die komplette User Journey gegen den echten Spring-Kontext (H2, Demo-KI, Demo-Stellenanzeige).
 * Nur das Browser-Rendering des PDFs wird ersetzt.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobCraftIntegrationTest {

    private static final String SAMPLE_URL = "https://karriere.quantis-software.de/jobs/senior-fullstack-entwickler";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private PdfGenerationService pdfGenerationService;

    @Test
    void fullJourneyFromRegistrationToPdf() throws Exception {
        String token = json(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Lena Hoffmann\",\"email\":\"journey@example.de\",\"password\":\"geheim123\"}"))
                .andExpect(status().isCreated())).get("token").asText();

        mvc.perform(auth(put("/api/profile"), token).contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Lena Hoffmann","street":"Lindenstraße 12","zipCode":"50674","city":"Köln",
                         "phone":"+49 171 2345678","email":"journey@example.de","letterStyle":"CLASSIC",
                         "customFields":[{"label":"Verfügbarkeit","value":"1. Januar 2027"}]}"""))
                .andExpect(status().isOk());

        String noteId = json(mvc.perform(auth(post("/api/notes"), token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Privatprojekt\",\"content\":\"Haushaltsbuch mit Angular\",\"included\":true}"))
                .andExpect(status().isCreated())).get("id").asText();
        mvc.perform(auth(post("/api/notes"), token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hobby\",\"content\":\"Klettern\",\"included\":false}"))
                .andExpect(status().isCreated());
        mvc.perform(auth(put("/api/notes/" + noteId), token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Privatprojekt\",\"content\":\"Haushaltsbuch mit Angular Signals\","
                                + "\"included\":true}"))
                .andExpect(status().isOk());
        JsonNode notes = json(mvc.perform(auth(get("/api/notes"), token)).andExpect(status().isOk()));
        assertThat(notes).hasSize(2);
        assertThat(notes.get(0).get("content").asText()).isEqualTo("Haushaltsbuch mit Angular Signals");

        byte[] cv = TestPdfs.withLines(List.of("LENA HOFFMANN", "Java 21, Spring Boot, Angular Signals", "PostgreSQL"));
        String docId = json(mvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "Lebenslauf_Lena.pdf", "application/pdf", cv))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())).get("id").asText();
        JsonNode doc = waitFor(token, "/api/documents/" + docId, d -> !"PROCESSING".equals(d.get("status").asText()));
        assertThat(doc.get("status").asText()).isEqualTo("READY");
        assertThat(doc.get("extractedText").asText()).contains("Spring Boot");

        String appId = json(mvc.perform(auth(post("/api/applications"), token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + SAMPLE_URL + "\"}"))
                .andExpect(status().isAccepted())).get("id").asText();
        JsonNode analyzed = waitFor(token, "/api/applications/" + appId,
                a -> !"ANALYZING".equals(a.get("status").asText()));
        assertThat(analyzed.get("status").asText()).isEqualTo("ANALYZED");
        assertThat(analyzed.get("companyName").asText()).isEqualTo("Quantis Software GmbH");
        assertThat(analyzed.get("contactPerson").asText()).isEqualTo("Frau Dr. Miriam Kessler");
        assertThat(analyzed.get("requirements")).hasSize(6);
        assertThat(analyzed.get("contextDocumentIds").get(0).asText()).isEqualTo(docId);

        JsonNode generated = json(mvc.perform(auth(post("/api/applications/" + appId + "/generate"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"companyName":"Quantis Software GmbH","jobTitle":"Senior Fullstack Entwickler (m/w/d)",
                                 "contactPerson":"Frau Dr. Miriam Kessler","recipientAddress":"Am Mediapark 8, 50670 Köln",
                                 "documentIds":["%s"],"tone":"FORMAL","length":"STANDARD"}""".formatted(docId)))
                .andExpect(status().isOk()));
        assertThat(generated.get("status").asText()).isEqualTo("DRAFT");
        assertThat(generated.get("versions")).hasSize(1);
        assertThat(generated.get("letter").get("salutation").asText()).isEqualTo("Sehr geehrte Frau Dr. Kessler,");
        assertThat(generated.get("messages").get(0).get("meta").asText()).isEqualTo("v1 erstellt");
        assertThat(generated.get("messages").get(0).get("message").asText())
                .contains("Grundlage: Lebenslauf, 1 weitere Angabe und");

        JsonNode revised = json(mvc.perform(auth(post("/api/applications/" + appId + "/chat"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Einleitung kürzen\"}"))
                .andExpect(status().isOk()));
        assertThat(revised.get("versions")).hasSize(2);
        assertThat(revised.get("messages")).hasSize(3);
        assertThat(revised.get("messages").get(1).get("role").asText()).isEqualTo("USER");
        assertThat(revised.get("messages").get(2).get("meta").asText()).isEqualTo("v2 · Absatz 1 geändert");

        List<String> current = new ArrayList<>();
        revised.get("versions").get(1).get("paragraphs").forEach(p -> current.add(p.asText()));
        current.set(1, "Mein eigener, direkt bearbeiteter Absatz.");
        JsonNode edited = json(mvc.perform(auth(put("/api/applications/" + appId + "/paragraphs"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("paragraphs", current))))
                .andExpect(status().isOk()));
        assertThat(edited.get("versions")).hasSize(3);
        assertThat(edited.get("versions").get(2).get("changedParagraphs").get(0).asInt()).isEqualTo(1);
        assertThat(edited.get("messages").get(3).get("meta").asText()).isEqualTo("v3 · Absatz 2 bearbeitet");

        mvc.perform(auth(put("/api/applications/" + appId + "/paragraphs"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("paragraphs", current))))
                .andExpect(status().isBadRequest());

        current.remove(0);
        JsonNode deleted = json(mvc.perform(auth(put("/api/applications/" + appId + "/paragraphs"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("paragraphs", current))))
                .andExpect(status().isOk()));
        assertThat(deleted.get("versions").get(3).get("paragraphs")).hasSize(4);
        assertThat(deleted.get("messages").get(4).get("meta").asText()).isEqualTo("v4 · Absatz 1 gelöscht");

        JsonNode greeted = json(mvc.perform(auth(put("/api/applications/" + appId + "/salutation"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"salutation\":\" Liebe Frau Kessler, \"}"))
                .andExpect(status().isOk()));
        assertThat(greeted.get("letter").get("salutation").asText()).isEqualTo("Liebe Frau Kessler,");
        assertThat(greeted.get("customSalutation").asText()).isEqualTo("Liebe Frau Kessler,");
        assertThat(greeted.get("versions")).hasSize(4);
        assertThat(greeted.get("messages").get(5).get("meta").asText()).isEqualTo("Anrede · Liebe Frau Kessler,");
        JsonNode reset = json(mvc.perform(auth(put("/api/applications/" + appId + "/salutation"), token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"salutation\":null}"))
                .andExpect(status().isOk()));
        assertThat(reset.get("letter").get("salutation").asText()).isEqualTo("Sehr geehrte Frau Dr. Kessler,");
        assertThat(reset.get("customSalutation").isNull()).isTrue();

        JsonNode list = json(mvc.perform(auth(get("/api/applications"), token)).andExpect(status().isOk()));
        assertThat(list.get(0).get("latestVersion").asInt()).isEqualTo(4);

        when(pdfGenerationService.render(any(), anyList())).thenReturn(new byte[] {'%', 'P', 'D', 'F'});
        mvc.perform(auth(get("/api/applications/" + appId + "/pdf"), token).param("attachCv", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("Bewerbung_Quantis_Software_GmbH_Hoffmann.pdf")));

        mvc.perform(auth(get("/api/applications/" + appId), "x" + token)).andExpect(status().isUnauthorized());

        // Löschen entfernt die Bewerbung samt Versionen und Chat-Verlauf (ON DELETE CASCADE).
        mvc.perform(auth(delete("/api/applications/" + appId), token)).andExpect(status().isNoContent());
        assertThat(json(mvc.perform(auth(get("/api/applications"), token)).andExpect(status().isOk()))).isEmpty();
    }

    @Test
    void usersCannotSeeEachOthersApplications() throws Exception {
        String owner = register("owner@example.de");
        String other = register("other@example.de");
        String appId = json(mvc.perform(auth(post("/api/applications"), owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"" + SAMPLE_URL + "\"}"))).get("id").asText();

        mvc.perform(auth(get("/api/applications/" + appId), other)).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/applications/" + appId), other)).andExpect(status().isNotFound());

        mvc.perform(auth(delete("/api/applications/" + appId), owner)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/applications/" + appId), owner)).andExpect(status().isNotFound());
    }

    private String register(String email) throws Exception {
        return json(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"email\":\"" + email + "\",\"password\":\"geheim123\"}")))
                .get("token").asText();
    }

    private JsonNode waitFor(String token, String path, java.util.function.Predicate<JsonNode> done) throws Exception {
        JsonNode node = null;
        for (int i = 0; i < 100; i++) {
            node = json(mvc.perform(auth(get(path), token)).andExpect(status().isOk()));
            if (done.test(node)) {
                return node;
            }
            Thread.sleep(100);
        }
        return node;
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private JsonNode json(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
