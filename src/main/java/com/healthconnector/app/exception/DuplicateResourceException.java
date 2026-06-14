package com.healthconnector.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {
    private final String field;
    private final Object value;

    public DuplicateResourceException(String field, Object value) {
        super(String.format("Resource already exists with %s: '%s'", field, value));
        this.field = field;
        this.value = value;
    }

    public DuplicateResourceException(String message) {
        super(message);
        this.field = null;
        this.value = null;
    }

    public String getField() { return field; }
    public Object getValue() { return value; }
}
