package com.assessly.exceptions;

public class UnsupportedEvidenceException extends AssesslyException {
    public UnsupportedEvidenceException(String message) {
        super(message);
    }

    public UnsupportedEvidenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
