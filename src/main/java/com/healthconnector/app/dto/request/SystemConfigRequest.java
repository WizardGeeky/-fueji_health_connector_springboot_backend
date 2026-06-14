package com.healthconnector.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "System configuration update (SUPER_ADMIN only)")
public class SystemConfigRequest {

    @Schema(description = "Gemini model name", example = "gemini-2.0-flash")
    private String geminiModel;

    @DecimalMin(value = "0.0") @DecimalMax(value = "1.0")
    @Schema(description = "Gemini temperature (0.0 to 1.0)", example = "0.3")
    private Double geminiTemperature;

    @Positive
    @Schema(description = "Gemini max output tokens", example = "4096")
    private Integer geminiMaxTokens;

    @Schema(description = "Enable/disable Gemini AI review")
    private Boolean geminiEnabled;

    @Schema(description = "Require AI review before submission")
    private Boolean aiReviewRequiredBeforeSubmit;

    @Schema(description = "FHIR base URL", example = "https://hapi.fhir.org/baseR4")
    private String fhirBaseUrl;

    @Schema(description = "Enable/disable FHIR validation")
    private Boolean fhirEnabled;

    @Min(1) @Max(10)
    @Schema(description = "Max failed login attempts before lock", example = "5")
    private Integer maxFailedAttempts;

    @Min(30) @Max(365)
    @Schema(description = "Password expiry in days", example = "90")
    private Integer passwordExpiryDays;
}
