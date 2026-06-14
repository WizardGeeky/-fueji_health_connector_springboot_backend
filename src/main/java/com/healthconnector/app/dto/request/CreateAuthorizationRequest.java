package com.healthconnector.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request to create a Prior Authorization request (PROVIDER role).
 */
@Data
@Schema(description = "Create a new Prior Authorization Request")
public class CreateAuthorizationRequest {

    @NotBlank(message = "Patient name is required")
    @Schema(description = "Full patient name (will be AES encrypted)", example = "Jane Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String patientName;

    @NotBlank(message = "Patient date of birth is required")
    @Schema(description = "Patient DOB (YYYY-MM-DD)", example = "1985-04-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private String patientDateOfBirth;

    @Schema(description = "Patient address (will be AES encrypted)", example = "456 Oak Street, Chicago, IL 60601")
    private String patientAddress;

    @Schema(description = "Patient mobile (will be AES encrypted)", example = "+13125550150")
    private String patientMobile;

    @NotBlank(message = "Insurance number is required")
    @Schema(description = "Insurance policy number (AES encrypted)", example = "INS123456789", requiredMode = Schema.RequiredMode.REQUIRED)
    private String insuranceNumber;

    @NotBlank(message = "Member ID is required")
    @Schema(description = "Insurance member ID (AES encrypted)", example = "MEM987654", requiredMode = Schema.RequiredMode.REQUIRED)
    private String memberId;

    @NotBlank(message = "Primary diagnosis code is required")
    @Schema(description = "ICD-10 diagnosis code", example = "M54.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private String primaryDiagnosisCode;

    @NotBlank(message = "Diagnosis description is required")
    @Schema(description = "Diagnosis description (AES encrypted)", example = "Low back pain", requiredMode = Schema.RequiredMode.REQUIRED)
    private String diagnosisDescription;

    @NotBlank(message = "Procedure code is required")
    @Schema(description = "CPT procedure code", example = "99213", requiredMode = Schema.RequiredMode.REQUIRED)
    private String procedureCode;

    @NotBlank(message = "Procedure description is required")
    @Schema(description = "Procedure description (AES encrypted)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String procedureDescription;

    @Schema(description = "Clinical notes (AES encrypted)")
    private String clinicalNotes;

    @NotBlank(message = "Payer ID is required")
    @Schema(description = "Target payer user ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String payerId;

    @Schema(description = "Requested service date (YYYY-MM-DD)", example = "2026-07-01")
    private String requestedServiceDate;

    @Schema(description = "Request priority: ROUTINE, URGENT, STAT", example = "ROUTINE", allowableValues = {"ROUTINE","URGENT","STAT"})
    private String priority = "ROUTINE";

    @Schema(description = "Place of service code", example = "11")
    private String placeOfService;
}
