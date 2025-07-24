package com.docprocessor.domain.enums;

public enum ProcessingStatus {
    UPLOADED("Document uploaded and waiting for processing"),
    PROCESSING("Document is being processed"),
    VALIDATED("Document processed and validated successfully"),
    FAILED("Document processing failed");

    private final String description;

    ProcessingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return this == VALIDATED || this == FAILED;
    }

    public boolean isInProgress() {
        return this == PROCESSING;
    }

    public boolean canBeProcessed() {
        return this == UPLOADED;
    }
}