package com.jobcraft.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobcraft.config.SecurityConfig;
import com.jobcraft.dto.AuthResponse;
import com.jobcraft.dto.ChangePasswordRequest;
import com.jobcraft.dto.UserDto;
import com.jobcraft.exception.ConflictException;
import com.jobcraft.exception.InvalidCredentialsException;
import com.jobcraft.service.AuthService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registersNewUser() throws Exception {
        when(authService.register(any())).thenReturn(
                new AuthResponse("token", new UserDto(USER_ID, "lena@example.de", "Lena Hoffmann")));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lena Hoffmann\",\"email\":\"lena@example.de\",\"password\":\"geheim123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.user.name").value("Lena Hoffmann"));
    }

    @Test
    void rejectsShortPassword() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lena\",\"email\":\"lena@example.de\",\"password\":\"kurz\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Das Passwort muss mindestens 8 Zeichen lang sein."))
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void reportsDuplicateEmail() throws Exception {
        when(authService.register(any()))
                .thenThrow(new ConflictException("Für diese E-Mail-Adresse existiert bereits ein Konto."));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lena\",\"email\":\"lena@example.de\",\"password\":\"geheim123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsWrongCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException("E-Mail oder Passwort ist falsch."));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"lena@example.de\",\"password\":\"falsch123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("E-Mail oder Passwort ist falsch."));
    }

    @Test
    void requiresTokenForProtectedEndpoints() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void returnsCurrentUser() throws Exception {
        when(authService.me(USER_ID)).thenReturn(new UserDto(USER_ID, "lena@example.de", "Lena Hoffmann"));

        mvc.perform(get("/api/auth/me").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("lena@example.de"));
    }

    @Test
    void changesPassword() throws Exception {
        mvc.perform(put("/api/auth/password").with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"geheim123\",\"newPassword\":\"neuesPasswort1\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsWrongCurrentPassword() throws Exception {
        doThrow(new InvalidCredentialsException("Das aktuelle Passwort ist falsch."))
                .when(authService).changePassword(eq(USER_ID), any(ChangePasswordRequest.class));

        mvc.perform(put("/api/auth/password").with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"falsch123\",\"newPassword\":\"neuesPasswort1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Das aktuelle Passwort ist falsch."));
    }
}
