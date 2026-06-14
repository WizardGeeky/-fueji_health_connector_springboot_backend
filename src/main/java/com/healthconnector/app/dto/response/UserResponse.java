package com.healthconnector.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthconnector.app.constants.UserRole;
import com.healthconnector.app.constants.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Full user profile response")
public class UserResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String mobile;
    private UserRole role;
    private String organizationId;
    private String organizationName;
    private String npi;
    private String address;
    private UserStatus status;
    private boolean accountLocked;
    private boolean passwordChanged;
    private Instant lastLogin;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String lastAdminPassword;
}
