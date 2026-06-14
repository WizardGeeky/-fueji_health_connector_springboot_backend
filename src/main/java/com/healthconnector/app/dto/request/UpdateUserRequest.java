package com.healthconnector.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Request to update an existing Provider or Payer profile")
public class UpdateUserRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    private String lastName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Mobile must be 10-15 digits")
    private String mobile;

    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 100)
    private String organizationName;

    @Schema(description = "NPI number (providers only)")
    private String npi;

    @Schema(description = "Office address")
    private String address;
}
