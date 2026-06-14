package com.healthconnector.app.model;

import com.healthconnector.app.constants.AppConstants;
import com.healthconnector.app.constants.AuthorizationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Prior Authorization Request entity.
 * Sensitive fields (patientName, patientAddress, diagnosisDescription, etc.)
 * are AES-256/GCM encrypted before persistence and decrypted on read.
 */
@Document(collection = AppConstants.COL_AUTHORIZATIONS)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "idx_provider_status", def = "{'provider_id': 1, 'status': 1}"),
        @CompoundIndex(name = "idx_payer_status",    def = "{'payer_id': 1, 'status': 1}"),
        @CompoundIndex(name = "idx_org_created",     def = "{'organization_id': 1, 'created_at': -1}")
})
public class AuthorizationRequest {

    @Id
    private String id;

    /** Reference number displayed to users. */
    @Indexed(unique = true)
    @Field("reference_number")
    private String referenceNumber;

    @Field("provider_id")
    private String providerId;

    @Field("provider_name")
    private String providerName;

    @Field("payer_id")
    private String payerId;

    @Field("payer_name")
    private String payerName;

    @Field("organization_id")
    private String organizationId;

    // ── Encrypted patient fields ──────────────────────────────────────
    /** AES-256/GCM encrypted. */
    @Field("patient_name")
    private String patientName;

    /** AES-256/GCM encrypted. */
    @Field("patient_dob")
    private String patientDateOfBirth;

    /** AES-256/GCM encrypted. */
    @Field("patient_address")
    private String patientAddress;

    /** AES-256/GCM encrypted. */
    @Field("patient_mobile")
    private String patientMobile;

    /** AES-256/GCM encrypted. */
    @Field("insurance_number")
    private String insuranceNumber;

    /** AES-256/GCM encrypted. */
    @Field("member_id")
    private String memberId;

    // ── Clinical data (encrypted) ────────────────────────────────────
    /** ICD-10 code. */
    @Field("primary_diagnosis_code")
    private String primaryDiagnosisCode;

    /** AES-256/GCM encrypted. */
    @Field("diagnosis_description")
    private String diagnosisDescription;

    /** CPT code. */
    @Field("procedure_code")
    private String procedureCode;

    /** AES-256/GCM encrypted. */
    @Field("procedure_description")
    private String procedureDescription;

    /** AES-256/GCM encrypted. */
    @Field("clinical_notes")
    private String clinicalNotes;

    /** Date when service is requested. */
    @Field("requested_service_date")
    private String requestedServiceDate;

    @Field("priority")
    @Builder.Default
    private String priority = "ROUTINE";   // ROUTINE | URGENT | STAT

    @Field("place_of_service")
    private String placeOfService;

    // ── Attachments (metadata encrypted) ─────────────────────────────
    @Field("attachments")
    @Builder.Default
    private List<AttachmentMetadata> attachments = new ArrayList<>();

    // ── Status & Review ───────────────────────────────────────────────
    @Field("status")
    @Builder.Default
    private AuthorizationStatus status = AuthorizationStatus.DRAFT;

    @Field("payer_notes")
    private String payerNotes;

    @Field("rejection_reason")
    private String rejectionReason;

    @Field("more_info_notes")
    private String moreInfoNotes;

    /** ID of the latest AI review record. */
    @Field("latest_ai_review_id")
    private String latestAiReviewId;

    @Field("ai_score")
    private Double aiScore;

    @Field("ai_risk_level")
    private String aiRiskLevel;

    // ── FHIR ─────────────────────────────────────────────────────────
    @Field("fhir_resource_id")
    private String fhirResourceId;

    @Field("fhir_compliant")
    private Boolean fhirCompliant;

    // ── Audit ─────────────────────────────────────────────────────────
    @Field("submitted_at")
    private Instant submittedAt;

    @Field("reviewed_at")
    private Instant reviewedAt;

    @Field("approved_at")
    private Instant approvedAt;

    @Field("rejected_at")
    private Instant rejectedAt;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    @CreatedBy
    @Field("created_by")
    private String createdBy;

    @LastModifiedBy
    @Field("updated_by")
    private String updatedBy;

    @Field("deleted")
    @Builder.Default
    private boolean deleted = false;

    @Field("deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    // ── Nested class ──────────────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentMetadata {
        /** AES-256/GCM encrypted filename. */
        private String fileName;
        /** AES-256/GCM encrypted file path / URL. */
        private String filePath;
        private String contentType;
        private Long sizeBytes;
        private Instant uploadedAt;
        private String uploadedBy;
    }
}
