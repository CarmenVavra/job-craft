package com.jobcraft.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobcraft.config.SecurityConfig;
import com.jobcraft.dto.CustomFieldDto;
import com.jobcraft.dto.ProfileDto;
import com.jobcraft.model.LetterStyle;
import com.jobcraft.service.ProfileService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
class ProfileControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProfileService profileService;

    private final ProfileDto profile = new ProfileDto("Lena Hoffmann", "Lindenstraße 12", "50674", "Köln",
            "+49 171 2345678", "lena@example.de", null, null, LetterStyle.CLASSIC,
            List.of(new CustomFieldDto("Führerschein", "Klasse B")));

    @Test
    void returnsProfile() throws Exception {
        when(profileService.get(USER_ID)).thenReturn(profile);

        mvc.perform(get("/api/profile").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Lena Hoffmann"))
                .andExpect(jsonPath("$.customFields[0].label").value("Führerschein"));
    }

    @Test
    void updatesProfile() throws Exception {
        when(profileService.update(eq(USER_ID), any())).thenReturn(profile);

        mvc.perform(put("/api/profile").with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Lena Hoffmann\",\"letterStyle\":\"CLASSIC\",\"customFields\":[]}"))
                .andExpect(status().isOk());
    }

    @Test
    void requiresName() throws Exception {
        mvc.perform(put("/api/profile").with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"letterStyle\":\"CLASSIC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bitte gib deinen vollständigen Namen ein."));
    }
}
