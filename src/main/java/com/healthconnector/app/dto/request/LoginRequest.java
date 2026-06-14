package com.healthconnector.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Login request DTO — shared across all roles (SUPER_ADMIN, PROVIDER, PAYER).
 */
@Data
@Schema(description = "Login credentials for all user roles")
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Schema(description = "User email address", example = "admin@healthconnector.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "Admin@1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
