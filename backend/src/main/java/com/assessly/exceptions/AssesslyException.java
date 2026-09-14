package com.assessly.exceptions;

public class AssesslyException extends RuntimeException {
    public AssesslyException(String message) {
        super(message);
    }

    public AssesslyException(String message, Throwable cause) {
        super(message, cause);
    }
}
