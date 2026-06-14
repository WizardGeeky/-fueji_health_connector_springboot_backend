package com.healthconnector.app.exception;

import java.util.List;

public class FHIRValidationException extends RuntimeException {
    private final List<String> validationErrors;

    public FHIRValidationException(String message) {
        super(message);
        this.validationErrors = List.of();
    }

    public FHIRValidationException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() { return validationErrors; }
}
