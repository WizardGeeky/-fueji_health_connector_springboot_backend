package com.healthconnector.app.model;

import com.healthconnector.app.constants.AppConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Runtime system configuration for Gemini AI and FHIR integration.
 * Managed exclusively by SUPER_ADMIN. Only a single active config record exists.
 */
@Document(collection = AppConstants.COL_SYSTEM_CONFIGS)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {

    @Id
    private String id;

    @Field("config_key")
    private String configKey;

    // ── Gemini Settings ─────────────────────────────────────────────
    @Field("gemini_model")
    @Builder.Default
    private String geminiModel = "gemini-2.0-flash";

    @Field("gemini_temperature")
    @Builder.Default
    private Double geminiTemperature = 0.3;

    @Field("gemini_max_tokens")
    @Builder.Default
    private Integer geminiMaxTokens = 4096;

    @Field("gemini_enabled")
    @Builder.Default
    private boolean geminiEnabled = true;

    @Field("ai_review_required_before_submit")
    @Builder.Default
    private boolean aiReviewRequiredBeforeSubmit = true;

    // ── FHIR Settings ────────────────────────────────────────────────
    @Field("fhir_base_url")
    private String fhirBaseUrl;

    @Field("fhir_enabled")
    @Builder.Default
    private boolean fhirEnabled = true;

    @Field("fhir_version")
    @Builder.Default
    private String fhirVersion = "R4";

    // ── Security Settings ────────────────────────────────────────────
    @Field("max_failed_attempts")
    @Builder.Default
    private int maxFailedAttempts = 5;

    @Field("password_expiry_days")
    @Builder.Default
    private int passwordExpiryDays = 90;

    @Field("session_timeout_minutes")
    @Builder.Default
    private int sessionTimeoutMinutes = 60;

    // ── Audit ────────────────────────────────────────────────────────
    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    @LastModifiedBy
    @Field("updated_by")
    private String updatedBy;

    @Version
    private Long version;
}
