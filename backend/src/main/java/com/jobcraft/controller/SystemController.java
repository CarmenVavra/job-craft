package com.jobcraft.controller;

import com.jobcraft.dto.SystemInfoDto;
import com.jobcraft.service.ai.CoverLetterAi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Laufzeitinformationen für das Frontend (z. B. ob ein Sprachmodell oder der Demo-Modus aktiv ist).
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final CoverLetterAi ai;

    @GetMapping
    public ResponseEntity<SystemInfoDto> info() {
        return ResponseEntity.ok(new SystemInfoDto(ai.isLanguageModel()));
    }
}
