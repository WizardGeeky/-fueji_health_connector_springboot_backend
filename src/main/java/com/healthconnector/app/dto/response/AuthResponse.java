package com.healthconnector.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthconnector.app.constants.UserRole;
import com.healthconnector.app.constants.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Authentication response containing JWT tokens and user info.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Authentication response with JWT tokens")
public class AuthResponse {

    @Schema(description = "JWT access token (24h validity)")
    private String accessToken;

    @Schema(description = "JWT refresh token (7d validity)")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Access token expiry in milliseconds")
    private Long expiresIn;

    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private String organizationId;
    private String organizationName;
    private UserStatus status;
    private boolean passwordChanged;
}
