package com.healthconnector.app.constants;

/**
 * User roles used for RBAC across the platform.
 * Spring Security role prefix ROLE_ is applied automatically.
 */
public enum UserRole {
    SUPER_ADMIN,
    PROVIDER,
    PAYER
}
