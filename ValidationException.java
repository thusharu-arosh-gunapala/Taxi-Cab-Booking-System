package com.taxibooking.service; //ValidationException

import java.util.List;

/** Thrown by services when user input is invalid. Carries every error message so the page can show them all. */
public class ValidationException extends RuntimeException {
    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = errors;
    }

    public ValidationException(String error) {
        this(List.of(error));
    }

    public List<String> getErrors() { return errors; }
}
