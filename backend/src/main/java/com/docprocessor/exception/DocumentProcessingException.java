package com.docprocessor.exception;

import lombok.Getter;

/**
 * Base exception for document processing errors.
 * 
 * @since 1.0.0
 */
@Getter
public class DocumentProcessingException extends RuntimeException {
    private final ErrorCode errorCode;
    private final transient Object[] args;
    
    public DocumentProcessingException(String message) {
        this(message, ErrorCode.PROCESSING_ERROR);
    }
    
    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.PROCESSING_ERROR;
        this.args = new Object[0];
    }
    
    public DocumentProcessingException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }
    
    public DocumentProcessingException(ErrorCode errorCode, Object... args) {
        super(errorCode.getMessage(args));
        this.errorCode = errorCode;
        this.args = args;
    }
    
    public DocumentProcessingException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode.getMessage(args), cause);
        this.errorCode = errorCode;
        this.args = args;
    }
}