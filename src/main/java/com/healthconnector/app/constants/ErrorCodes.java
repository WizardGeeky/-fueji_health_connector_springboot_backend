package com.healthconnector.app.constants;

/**
 * Standardized error codes returned in API error responses.
 */
public final class ErrorCodes {

    private ErrorCodes() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String VALIDATION_ERROR        = "VALIDATION_ERROR";
    public static final String RESOURCE_NOT_FOUND      = "RESOURCE_NOT_FOUND";
    public static final String DUPLICATE_RESOURCE      = "DUPLICATE_RESOURCE";
    public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
    public static final String UNAUTHORIZED             = "UNAUTHORIZED";
    public static final String FORBIDDEN                = "FORBIDDEN";
    public static final String AES_ENCRYPTION_ERROR    = "AES_ENCRYPTION_ERROR";
    public static final String GEMINI_INTEGRATION_ERROR = "GEMINI_INTEGRATION_ERROR";
    public static final String FHIR_VALIDATION_ERROR   = "FHIR_VALIDATION_ERROR";
    public static final String INTERNAL_SERVER_ERROR   = "INTERNAL_SERVER_ERROR";
    public static final String DATABASE_ERROR          = "DATABASE_ERROR";
    public static final String ACCOUNT_LOCKED          = "ACCOUNT_LOCKED";
    public static final String ACCOUNT_BLOCKED         = "ACCOUNT_BLOCKED";
    public static final String INVALID_CREDENTIALS     = "INVALID_CREDENTIALS";
    public static final String TOKEN_EXPIRED           = "TOKEN_EXPIRED";
    public static final String TOKEN_INVALID           = "TOKEN_INVALID";
    public static final String PASSWORD_POLICY_VIOLATED = "PASSWORD_POLICY_VIOLATED";
    public static final String RATE_LIMIT_EXCEEDED     = "RATE_LIMIT_EXCEEDED";
    public static final String METHOD_NOT_ALLOWED      = "METHOD_NOT_ALLOWED";
    public static final String MEDIA_TYPE_NOT_SUPPORTED = "MEDIA_TYPE_NOT_SUPPORTED";
}
