package com.jobcraft.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobcraft.config.SecurityConfig;
import com.jobcraft.dto.ProfileNoteDto;
import com.jobcraft.dto.ProfileNoteRequest;
import com.jobcraft.exception.NotFoundException;
import com.jobcraft.service.ProfileNoteService;
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

@WebMvcTest(ProfileNoteController.class)
@Import(SecurityConfig.class)
class ProfileNoteControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID NOTE_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProfileNoteService noteService;

    private final ProfileNoteDto note = new ProfileNoteDto(NOTE_ID, "Privatprojekt: Haushaltsbuch",
            "Angular 21 mit Signals und Spring Boot", true, Instant.now(), Instant.now());

    @Test
    void listsNotes() throws Exception {
        when(noteService.list(USER_ID)).thenReturn(List.of(note));

        mvc.perform(get("/api/notes").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Privatprojekt: Haushaltsbuch"))
                .andExpect(jsonPath("$[0].included").value(true));
    }

    @Test
    void createsNote() throws Exception {
        when(noteService.create(eq(USER_ID), any())).thenReturn(note);

        mvc.perform(post("/api/notes").with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Privatprojekt\",\"content\":\"Angular\",\"included\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(NOTE_ID.toString()));
    }

    @Test
    void updatesNote() throws Exception {
        when(noteService.update(USER_ID, NOTE_ID, new ProfileNoteRequest("Titel", "Text", false))).thenReturn(note);

        mvc.perform(put("/api/notes/" + NOTE_ID).with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Titel\",\"content\":\"Text\",\"included\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsEmptyContent() throws Exception {
        mvc.perform(post("/api/notes").with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Privatprojekt\",\"content\":\" \",\"included\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bitte beschreibe die Angabe."));
    }

    @Test
    void deletesNoteAndReportsUnknownOnes() throws Exception {
        mvc.perform(delete("/api/notes/" + NOTE_ID).with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isNoContent());
        verify(noteService).delete(USER_ID, NOTE_ID);

        UUID unknown = UUID.randomUUID();
        doThrow(new NotFoundException("Angabe nicht gefunden.")).when(noteService).delete(USER_ID, unknown);
        mvc.perform(delete("/api/notes/" + unknown).with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/notes")).andExpect(status().isUnauthorized());
    }
}
