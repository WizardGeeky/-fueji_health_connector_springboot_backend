package com.healthconnector.app.controller;

import com.healthconnector.app.dto.request.SystemConfigRequest;
import com.healthconnector.app.dto.response.ApiResponse;
import com.healthconnector.app.exception.ResourceNotFoundException;
import com.healthconnector.app.model.SystemConfig;
import com.healthconnector.app.repository.SystemConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * System configuration controller — SUPER_ADMIN only.
 * Manages runtime Gemini AI and FHIR settings.
 */
@RestController
@RequestMapping("/api/system/config")
@Tag(name = "System Config", description = "Runtime system configuration management — SUPER_ADMIN only")
@SecurityRequirement(name = "bearerAuth")
public class SystemConfigController {

    private static final String CONFIG_KEY = "GLOBAL";
    
    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get Config", description = "Get current system configuration")
    public ResponseEntity<ApiResponse<SystemConfig>> getConfig() {
        SystemConfig config = systemConfigRepository.findByConfigKey(CONFIG_KEY)
                .orElseGet(() -> SystemConfig.builder().configKey(CONFIG_KEY).build());
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @CacheEvict(value = {"analytics", "systemConfig"}, allEntries = true)
    @Operation(summary = "Update Config", description = "Update Gemini AI and FHIR runtime settings")
    public ResponseEntity<ApiResponse<SystemConfig>> updateConfig(
            @Valid @RequestBody SystemConfigRequest request) {
        SystemConfig config = systemConfigRepository.findByConfigKey(CONFIG_KEY)
                .orElse(SystemConfig.builder().configKey(CONFIG_KEY).build());

        if (request.getGeminiModel()        != null) config.setGeminiModel(request.getGeminiModel());
        if (request.getGeminiTemperature()  != null) config.setGeminiTemperature(request.getGeminiTemperature());
        if (request.getGeminiMaxTokens()    != null) config.setGeminiMaxTokens(request.getGeminiMaxTokens());
        if (request.getGeminiEnabled()      != null) config.setGeminiEnabled(request.getGeminiEnabled());
        if (request.getAiReviewRequiredBeforeSubmit() != null)
            config.setAiReviewRequiredBeforeSubmit(request.getAiReviewRequiredBeforeSubmit());
        if (request.getFhirBaseUrl()        != null) config.setFhirBaseUrl(request.getFhirBaseUrl());
        if (request.getFhirEnabled()        != null) config.setFhirEnabled(request.getFhirEnabled());
        if (request.getMaxFailedAttempts()  != null) config.setMaxFailedAttempts(request.getMaxFailedAttempts());
        if (request.getPasswordExpiryDays() != null) config.setPasswordExpiryDays(request.getPasswordExpiryDays());

        SystemConfig saved = systemConfigRepository.save(config);
        return ResponseEntity.ok(ApiResponse.success("System configuration updated", saved));
    }
}
