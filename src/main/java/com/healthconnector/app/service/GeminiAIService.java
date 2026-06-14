package com.healthconnector.app.service;

import com.healthconnector.app.dto.response.AIReviewResponse;
import com.healthconnector.app.exception.GeminiIntegrationException;
import com.healthconnector.app.model.AIReviewHistory;
import com.healthconnector.app.model.AuthorizationRequest;
import com.healthconnector.app.repository.AIReviewHistoryRepository;
import com.healthconnector.app.repository.AuthorizationRequestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemini AI integration service for prior authorization clinical review.
 */
@Service
public class GeminiAIService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAIService.class);

    @Autowired(required = false)
    private ChatClient chatClient;

    @Autowired
    private AIReviewHistoryRepository aiReviewHistoryRepository;

    @Autowired
    private AuthorizationRequestRepository authorizationRequestRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-2.5-flash}")
    private String geminiModel;

    public AIReviewResponse reviewAuthorization(String authorizationId, String triggeredByUserId) {
        if (chatClient == null) {
            log.warn("Gemini AI service not configured — returning rule-based fallback for authorization {}", authorizationId);
            return buildFallbackReview(authorizationId, triggeredByUserId);
        }
        
        AuthorizationRequest request = authorizationRequestRepository.findByIdAndDeletedFalse(authorizationId)
                .orElseThrow(() -> new GeminiIntegrationException("Authorization not found: " + authorizationId));

        List<AuthorizationRequest> potentialDuplicates =
                authorizationRequestRepository.findPotentialDuplicates(
                        request.getProviderId(),
                        request.getPrimaryDiagnosisCode(),
                        request.getProcedureCode());
        boolean isDuplicate = potentialDuplicates.stream()
                .anyMatch(d -> !d.getId().equals(authorizationId));

        String prompt = buildReviewPrompt(request, isDuplicate);
        log.info("Sending authorization to Gemini AI | authorizationId={}", authorizationId);

        String rawResponse;
        try {
            rawResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            log.debug("Gemini response received | authorizationId={}", authorizationId);
        } catch (Exception e) {
            log.error("Gemini API call failed | authorizationId={} error={}", authorizationId, e.getMessage(), e);
            throw new GeminiIntegrationException("Gemini AI service unavailable: " + e.getMessage(), e);
        }

        AIReviewResponse reviewResponse = parseGeminiResponse(rawResponse, authorizationId, isDuplicate);

        AIReviewHistory history = AIReviewHistory.builder()
                .authorizationId(authorizationId)
                .triggeredByUserId(triggeredByUserId)
                .geminiModel(geminiModel)
                .score(reviewResponse.getScore())
                .riskLevel(reviewResponse.getRiskLevel())
                .recommendation(reviewResponse.getRecommendation())
                .missingFields(reviewResponse.getMissingFields())
                .warnings(reviewResponse.getWarnings())
                .suggestions(reviewResponse.getSuggestions())
                .fhirCompliant(reviewResponse.getFhirCompliant())
                .duplicateDetected(isDuplicate)
                .medicalNecessityMet(reviewResponse.isMedicalNecessityMet())
                .rawGeminiResponse(rawResponse)
                .build();
        AIReviewHistory saved = aiReviewHistoryRepository.save(history);

        request.setLatestAiReviewId(saved.getId());
        request.setAiScore(reviewResponse.getScore());
        request.setAiRiskLevel(reviewResponse.getRiskLevel());
        request.setFhirCompliant(reviewResponse.getFhirCompliant());
        authorizationRequestRepository.save(request);

        reviewResponse.setReviewId(saved.getId());
        log.info("AI review completed | authorizationId={} score={} risk={}",
                authorizationId, reviewResponse.getScore(), reviewResponse.getRiskLevel());
        return reviewResponse;
    }

    private AIReviewResponse buildFallbackReview(String authorizationId, String triggeredByUserId) {
        AuthorizationRequest request = authorizationRequestRepository.findByIdAndDeletedFalse(authorizationId)
                .orElseThrow(() -> new GeminiIntegrationException("Authorization not found: " + authorizationId));

        List<AuthorizationRequest> potentialDuplicates =
                authorizationRequestRepository.findPotentialDuplicates(
                        request.getProviderId(),
                        request.getPrimaryDiagnosisCode(),
                        request.getProcedureCode());
        boolean isDuplicate = potentialDuplicates.stream().anyMatch(d -> !d.getId().equals(authorizationId));

        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        double score = 72.0;
        String riskLevel = "LOW";
        String recommendation = "APPROVE";

        if (isDuplicate) {
            warnings.add("Potential duplicate request detected for this patient and procedure");
            score -= 20;
            riskLevel = "HIGH";
            recommendation = "REVIEW";
        }
        if (request.getClinicalNotes() == null || request.getClinicalNotes().isBlank()) {
            warnings.add("Clinical notes are missing — medical necessity may be unclear");
            suggestions.add("Add detailed clinical notes to support medical necessity");
            score -= 15;
            if (riskLevel.equals("LOW")) riskLevel = "MEDIUM";
            recommendation = "REQUEST_MORE_INFO";
        }
        if (request.getPriority() != null && request.getPriority().equals("EMERGENT")) {
            warnings.add("Emergent priority — expedited review required");
            score += 5;
        }
        if (request.getPrimaryDiagnosisCode() == null || request.getProcedureCode() == null) {
            warnings.add("Missing diagnosis or procedure code");
            score -= 10;
        }
        suggestions.add("Ensure all supporting clinical documentation is attached");

        score = Math.max(10, Math.min(100, score));

        AIReviewResponse response = AIReviewResponse.builder()
                .authorizationId(authorizationId)
                .geminiModel("rule-based-fallback")
                .score(score)
                .riskLevel(riskLevel)
                .recommendation(recommendation)
                .medicalNecessityMet(score >= 60)
                .fhirCompliant(true)
                .missingFields(List.of())
                .warnings(warnings)
                .suggestions(suggestions)
                .duplicateDetected(isDuplicate)
                .build();

        AIReviewHistory history = AIReviewHistory.builder()
                .authorizationId(authorizationId)
                .triggeredByUserId(triggeredByUserId)
                .geminiModel("rule-based-fallback")
                .score(response.getScore())
                .riskLevel(response.getRiskLevel())
                .recommendation(response.getRecommendation())
                .missingFields(List.of())
                .warnings(warnings)
                .suggestions(suggestions)
                .fhirCompliant(true)
                .duplicateDetected(isDuplicate)
                .medicalNecessityMet(response.isMedicalNecessityMet())
                .rawGeminiResponse("{\"fallback\":true}")
                .build();
        AIReviewHistory saved = aiReviewHistoryRepository.save(history);

        request.setLatestAiReviewId(saved.getId());
        request.setAiScore(response.getScore());
        request.setAiRiskLevel(response.getRiskLevel());
        request.setFhirCompliant(true);
        authorizationRequestRepository.save(request);

        response.setReviewId(saved.getId());
        log.info("Fallback AI review completed | authorizationId={} score={} risk={}",
                authorizationId, response.getScore(), response.getRiskLevel());
        return response;
    }

    private String buildReviewPrompt(AuthorizationRequest req, boolean isDuplicate) {
        return """
            You are a clinical AI reviewer for healthcare prior authorization requests.
            Analyze the following prior authorization request and return ONLY a valid JSON response.

            Authorization Details:
            - Reference: %s
            - Primary Diagnosis Code (ICD-10): %s
            - Procedure Code (CPT): %s
            - Priority: %s
            - Duplicate Detected: %s
            - Has Clinical Notes: %s
            - Has Attachments: %s

            Evaluate for:
            1. Missing or incomplete required fields
            2. ICD-10 and CPT code validity and appropriateness
            3. Medical necessity based on diagnosis and procedure
            4. FHIR R4 compliance
            5. Duplicate request detection
            6. Coding mismatches between diagnosis and procedure

            Return ONLY this JSON (no markdown, no explanation):
            {
              "score": <0-100 number>,
              "riskLevel": "<LOW|MEDIUM|HIGH|CRITICAL>",
              "recommendation": "<APPROVE|REVIEW|REJECT|REQUEST_MORE_INFO>",
              "medicalNecessityMet": <true|false>,
              "fhirCompliant": <true|false>,
              "missingFields": ["<field1>"],
              "warnings": ["<warning1>"],
              "suggestions": ["<suggestion1>"]
            }
            """.formatted(
                req.getReferenceNumber(),
                req.getPrimaryDiagnosisCode(),
                req.getProcedureCode(),
                req.getPriority(),
                isDuplicate,
                req.getClinicalNotes() != null && !req.getClinicalNotes().isBlank(),
                req.getAttachments() != null && !req.getAttachments().isEmpty()
        );
    }

    private AIReviewResponse parseGeminiResponse(String rawResponse, String authorizationId, boolean isDuplicate) {
        try {
            String cleaned = rawResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            JsonNode json = objectMapper.readTree(cleaned);

            List<String> missingFields = new ArrayList<>();
            List<String> warnings      = new ArrayList<>();
            List<String> suggestions   = new ArrayList<>();

            if (json.has("missingFields") && json.get("missingFields").isArray())
                json.get("missingFields").forEach(n -> missingFields.add(n.asText()));
            if (json.has("warnings") && json.get("warnings").isArray())
                json.get("warnings").forEach(n -> warnings.add(n.asText()));
            if (json.has("suggestions") && json.get("suggestions").isArray())
                json.get("suggestions").forEach(n -> suggestions.add(n.asText()));

            return AIReviewResponse.builder()
                    .authorizationId(authorizationId)
                    .geminiModel(geminiModel)
                    .score(json.has("score") ? json.get("score").asDouble() : 50.0)
                    .riskLevel(json.has("riskLevel") ? json.get("riskLevel").asText() : "MEDIUM")
                    .recommendation(json.has("recommendation") ? json.get("recommendation").asText() : "REVIEW")
                    .medicalNecessityMet(json.has("medicalNecessityMet") && json.get("medicalNecessityMet").asBoolean(true))
                    .fhirCompliant(json.has("fhirCompliant") ? json.get("fhirCompliant").asBoolean() : null)
                    .missingFields(missingFields)
                    .warnings(warnings)
                    .suggestions(suggestions)
                    .duplicateDetected(isDuplicate)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response for authorizationId={}: {}", authorizationId, e.getMessage());
            return AIReviewResponse.builder()
                    .authorizationId(authorizationId)
                    .geminiModel(geminiModel)
                    .score(50.0)
                    .riskLevel("MEDIUM")
                    .recommendation("REVIEW")
                    .warnings(List.of("AI response parsing failed — manual review required"))
                    .missingFields(List.of())
                    .suggestions(List.of())
                    .duplicateDetected(isDuplicate)
                    .medicalNecessityMet(true)
                    .build();
        }
    }
}
