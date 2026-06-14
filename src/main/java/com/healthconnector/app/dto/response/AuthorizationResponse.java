package com.healthconnector.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthconnector.app.constants.AuthorizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Full prior authorization request response")
public class AuthorizationResponse {
    private String id;
    private String referenceNumber;
    private String providerId;
    private String providerName;
    private String payerId;
    private String payerName;
    private String organizationId;

    // Decrypted patient fields
    private String patientName;
    private String patientDateOfBirth;
    private String patientAddress;
    private String patientMobile;
    private String insuranceNumber;
    private String memberId;

    // Clinical data
    private String primaryDiagnosisCode;
    private String diagnosisDescription;
    private String procedureCode;
    private String procedureDescription;
    private String clinicalNotes;
    private String requestedServiceDate;
    private String priority;
    private String placeOfService;

    private AuthorizationStatus status;
    private String payerNotes;
    private String rejectionReason;
    private String moreInfoNotes;

    // AI review summary
    private Double aiScore;
    private String aiRiskLevel;
    private String latestAiReviewId;

    // FHIR
    private Boolean fhirCompliant;

    private List<AttachmentInfo> attachments;

    private Instant submittedAt;
    private Instant reviewedAt;
    private Instant approvedAt;
    private Instant rejectedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;

    @Data
    @Builder
    public static class AttachmentInfo {
        private String fileName;
        private String contentType;
        private Long sizeBytes;
        private Instant uploadedAt;
    }
}
