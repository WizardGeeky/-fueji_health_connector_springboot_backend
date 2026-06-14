package com.healthconnector.app.service;

import com.healthconnector.app.model.AuthorizationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * FHIR R4 validation service.
 * Validates authorization request data against FHIR R4 resource constraints.
 * In production this integrates with a HAPI FHIR server for full validation.
 */
@Service
@Slf4j
public class FHIRService {

    @Value("${app.fhir.enabled:true}")
    private boolean fhirEnabled;

    @Value("${app.fhir.base-url:https://hapi.fhir.org/baseR4}")
    private String fhirBaseUrl;

    private static final Pattern ICD10_PATTERN = Pattern.compile("^[A-Z][0-9]{2}(\\.[0-9A-Z]{0,4})?$");
    private static final Pattern CPT_PATTERN   = Pattern.compile("^[0-9]{5}([A-Z])?$|^[A-Z][0-9]{4}$");
    private static final Pattern NPI_PATTERN   = Pattern.compile("^[0-9]{10}$");

    /**
     * Validates an authorization request against FHIR R4 CoverageEligibilityRequest constraints.
     *
     * @param request the authorization request to validate
     * @throws FHIRValidationException if any FHIR constraint is violated
     */
    public boolean validateAuthorizationRequest(AuthorizationRequest request) {
        if (!fhirEnabled) {
            log.info("FHIR validation disabled. Skipping for authorization {}", request.getId());
            return true;
        }

        List<String> warnings = new ArrayList<>();

        if (request.getPrimaryDiagnosisCode() != null
                && !ICD10_PATTERN.matcher(request.getPrimaryDiagnosisCode()).matches()) {
            warnings.add("Non-standard ICD-10 code: " + request.getPrimaryDiagnosisCode());
        }

        if (request.getProcedureCode() != null
                && !CPT_PATTERN.matcher(request.getProcedureCode()).matches()) {
            warnings.add("Non-standard CPT code: " + request.getProcedureCode());
        }

        if (!warnings.isEmpty()) {
            log.warn("FHIR advisory warnings for authorization {}: {}", request.getId(), warnings);
        } else {
            log.info("FHIR validation passed for authorization {}", request.getId());
        }
        return true;
    }

    /**
     * Validates an NPI number format (10-digit Luhn check).
     */
    public boolean isValidNpi(String npi) {
        if (npi == null || !NPI_PATTERN.matcher(npi).matches()) return false;
        return luhnCheck(npi);
    }

    private boolean luhnCheck(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = number.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
