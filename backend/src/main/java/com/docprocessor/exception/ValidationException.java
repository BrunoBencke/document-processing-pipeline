package com.docprocessor.exception;

import lombok.Getter;
import java.util.Map;

/**
 * Exception thrown when validation fails.
 * 
 * @since 1.0.0
 */
@Getter
public class ValidationException extends DocumentProcessingException {
    
    private final Map<String, String> fieldErrors;
    
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
        this.fieldErrors = Map.of();
    }
    
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(ErrorCode.VALIDATION_ERROR, message);
        this.fieldErrors = fieldErrors;
    }
    
    public ValidationException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
        this.fieldErrors = Map.of();
    }
}