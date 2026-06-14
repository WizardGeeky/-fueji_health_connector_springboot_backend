package com.healthconnector.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIReviewResponse {
    private String reviewId;
    private String authorizationId;
    private String geminiModel;
    private Double score;
    private String riskLevel;
    private String recommendation;
    private List<String> missingFields;
    private List<String> warnings;
    private List<String> suggestions;
    private Boolean fhirCompliant;
    private boolean duplicateDetected;
    private boolean medicalNecessityMet;
    private Instant createdAt;
}
