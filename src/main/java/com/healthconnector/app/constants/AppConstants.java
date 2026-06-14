package com.healthconnector.app.constants;

/**
 * Application-wide constants used across all layers.
 * All string literals that appear more than once must reference these constants.
 */
public final class AppConstants {

    private AppConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // API Path Prefixes
    public static final String API_BASE = "/api";
    public static final String API_AUTH = "/api/auth";
    public static final String API_PROVIDERS = "/api/providers";
    public static final String API_PAYERS = "/api/payers";
    public static final String API_USERS = "/api/users";
    public static final String API_AUTHORIZATIONS = "/api/authorizations";
    public static final String API_CHAT = "/api/chat";
    public static final String API_NOTIFICATIONS = "/api/notifications";
    public static final String API_AUDIT_LOGS = "/api/audit-logs";
    public static final String API_ANALYTICS = "/api/analytics";
    public static final String API_SYSTEM_CONFIG = "/api/system/config";

    // JWT Header
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    // Security
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long PASSWORD_EXPIRY_DAYS = 90L;
    public static final int TEMP_PASSWORD_LENGTH = 12;

    // Pagination defaults
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Audit
    public static final String SYSTEM_USER = "SYSTEM";
    public static final String UNKNOWN = "UNKNOWN";

    // Date / Time formats
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    // Collections
    public static final String COL_USERS = "users";
    public static final String COL_AUTHORIZATIONS = "authorizations";
    public static final String COL_CHAT_MESSAGES = "chat_messages";
    public static final String COL_AUDIT_LOGS = "audit_logs";
    public static final String COL_NOTIFICATIONS = "notifications";
    public static final String COL_AI_REVIEWS = "ai_review_history";
    public static final String COL_SYSTEM_CONFIGS = "system_configs";

    // Cache names
    public static final String CACHE_USERS = "users";
    public static final String CACHE_ANALYTICS = "analytics";
    public static final String CACHE_SYSTEM_CONFIG = "systemConfig";

    // Validation messages
    public static final String MSG_EMAIL_EXISTS = "Email address is already registered";
    public static final String MSG_MOBILE_EXISTS = "Mobile number is already registered";
    public static final String MSG_NPI_INVALID = "NPI must be exactly 10 digits and pass Luhn check";
    public static final String MSG_ICD_INVALID = "Invalid ICD-10 code format";
    public static final String MSG_CPT_INVALID = "Invalid CPT code format (must be 5 digits or alphanumeric)";
    public static final String MSG_MEMBER_ID_INVALID = "Insurance Member ID must be 6-20 alphanumeric characters";
    public static final String MSG_PASSWORD_POLICY =
            "Password must be at least 8 characters with uppercase, lowercase, digit, and special character";

    // Email subjects
    public static final String EMAIL_SUBJECT_WELCOME = "Welcome to HealthConnector Platform";
    public static final String EMAIL_SUBJECT_PASSWORD_RESET = "Password Reset — HealthConnector Platform";
    public static final String EMAIL_SUBJECT_LOGIN_ALERT = "New Login Detected — HealthConnector Platform";
    public static final String EMAIL_SUBJECT_AUTH_SUBMITTED = "Authorization Request Submitted";
    public static final String EMAIL_SUBJECT_AUTH_APPROVED = "Authorization Request Approved";
    public static final String EMAIL_SUBJECT_AUTH_REJECTED = "Authorization Request Rejected";
    public static final String EMAIL_SUBJECT_MORE_INFO = "Additional Information Required";
    public static final String EMAIL_SUBJECT_AI_REVIEW = "AI Review Completed";
}
