package com.docprocessor.exception;

import lombok.Getter;

/**
 * Error codes enumeration with i18n support.
 * 
 * @since 1.0.0
 */
@Getter
public enum ErrorCode {
    DOCUMENT_NOT_FOUND("DOC001", "Document not found: %s"),
    DOCUMENT_INVALID_STATUS("DOC004", "Invalid document status transition from %s to %s"),
    
    FILE_TOO_LARGE("FILE002", "File size exceeds maximum allowed size of %s MB"),
    FILE_EMPTY("FILE003", "File is empty or corrupted"),
    FILE_NOT_FOUND("FILE005", "File not found: %s"),
    
    PROCESSING_ERROR("PROC001", "Document processing failed: %s"),
    
    STORAGE_NOT_FOUND("STOR002", "File not found in storage: %s"),
    
    VALIDATION_ERROR("VAL001", "Validation error: %s");
    
    private final String code;
    private final String messageTemplate;
    
    ErrorCode(String code, String messageTemplate) {
        this.code = code;
        this.messageTemplate = messageTemplate;
    }
    
    public String getMessage(Object... args) {
        return String.format(messageTemplate, args);
    }
}