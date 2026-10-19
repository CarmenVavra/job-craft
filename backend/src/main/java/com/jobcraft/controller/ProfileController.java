package com.jobcraft.controller;

import com.jobcraft.dto.ProfileDto;
import com.jobcraft.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Briefkopf-Stammdaten des angemeldeten Benutzers.
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileDto> get(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(profileService.get(CurrentUser.id(jwt)));
    }

    @PutMapping
    public ResponseEntity<ProfileDto> update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileDto profile) {
        return ResponseEntity.ok(profileService.update(CurrentUser.id(jwt), profile));
    }
}
