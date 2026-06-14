package com.healthconnector.app.security;

/**
 * Security-related constants for JWT processing.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final String CLAIM_USER_ID       = "userId";
    public static final String CLAIM_EMAIL         = "email";
    public static final String CLAIM_ROLE          = "role";
    public static final String CLAIM_ORGANIZATION  = "organizationId";
    public static final String CLAIM_SESSION_ID    = "sessionId";

    /** Public endpoints that do not require authentication. */
    public static final String[] PUBLIC_URLS = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/api-docs/**",
            "/v3/api-docs/**",
            "/actuator/health",
            "/actuator/info"
    };
}
