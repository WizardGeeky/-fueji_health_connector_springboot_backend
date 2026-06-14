package com.healthconnector.app.model;

import com.healthconnector.app.constants.AppConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the full result of every Gemini AI review performed on an authorization request.
 * Records are append-only — history is never modified.
 */
@Document(collection = AppConstants.COL_AI_REVIEWS)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIReviewHistory {

    @Id
    private String id;

    @Indexed
    @Field("authorization_id")
    private String authorizationId;

    @Field("triggered_by_user_id")
    private String triggeredByUserId;

    @Field("gemini_model")
    private String geminiModel;

    /** Composite AI confidence score 0–100. */
    @Field("score")
    private Double score;

    /** Risk classification: LOW | MEDIUM | HIGH | CRITICAL. */
    @Field("risk_level")
    private String riskLevel;

    @Field("recommendation")
    private String recommendation;

    @Field("missing_fields")
    @Builder.Default
    private List<String> missingFields = new ArrayList<>();

    @Field("warnings")
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Field("suggestions")
    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    @Field("fhir_compliant")
    private Boolean fhirCompliant;

    @Field("duplicate_detected")
    @Builder.Default
    private boolean duplicateDetected = false;

    @Field("medical_necessity_met")
    @Builder.Default
    private boolean medicalNecessityMet = true;

    @Field("raw_gemini_response")
    private String rawGeminiResponse;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;
}
