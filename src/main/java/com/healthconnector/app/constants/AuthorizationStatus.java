package com.healthconnector.app.constants;

/**
 * Prior Authorization Request status lifecycle.
 * State transitions:
 * DRAFT → AI_REVIEW → SUBMITTED → UNDER_REVIEW → APPROVED | REJECTED | MORE_INFO_REQUIRED → COMPLETED
 */
public enum AuthorizationStatus {
    DRAFT,
    AI_REVIEW,
    SUBMITTED,
    UNDER_REVIEW,
    MORE_INFO_REQUIRED,
    APPROVED,
    REJECTED,
    COMPLETED
}
