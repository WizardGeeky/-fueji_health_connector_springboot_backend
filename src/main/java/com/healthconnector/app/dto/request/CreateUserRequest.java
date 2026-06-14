package com.healthconnector.app.dto.request;

import com.healthconnector.app.constants.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Super Admin to onboard a new Provider or Payer.
 */
@Data
@Schema(description = "Request to create a new Provider or Payer by Super Admin")
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Schema(description = "Contact person first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Schema(description = "Contact person last name", example = "Smith", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Schema(description = "User email", example = "provider@hospital.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Mobile number must be 10-15 digits")
    @Schema(description = "Mobile number", example = "+14155550100", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;

    @Schema(description = "User role: PROVIDER or PAYER — set by the controller, not required from client")
    private UserRole role;

    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 100, message = "Organization name must be between 2 and 100 characters")
    @Schema(description = "Organization / Hospital name", example = "City General Hospital", requiredMode = Schema.RequiredMode.REQUIRED)
    private String organizationName;

    @Schema(description = "NPI number (10-digit, required for PROVIDER)", example = "1234567890")
    private String npi;

    @Schema(description = "Organization address", example = "123 Healthcare Ave, New York, NY 10001")
    private String address;
}
