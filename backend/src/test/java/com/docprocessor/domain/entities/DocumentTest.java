package com.docprocessor.domain.entities;

import com.docprocessor.domain.enums.ProcessingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTest {

    private ProcessingDocument document;

    @BeforeEach
    void setUp() {
        document = new ProcessingDocument("test-invoice.pdf");
    }

    @Test
    void testDocumentCreation() {
        assertEquals("test-invoice.pdf", document.getFilename());
        assertEquals(ProcessingStatus.UPLOADED, document.getStatus());
        assertNotNull(document.getUploadedAt());
        assertNotNull(document.getErrors());
        assertTrue(document.getErrors().isEmpty());
    }

    @Test
    void testSetStatus() {
        document.setStatus(ProcessingStatus.PROCESSING);
        assertEquals(ProcessingStatus.PROCESSING, document.getStatus());
        assertNull(document.getProcessedAt());

        document.setStatus(ProcessingStatus.VALIDATED);
        assertEquals(ProcessingStatus.VALIDATED, document.getStatus());
        assertNotNull(document.getProcessedAt());
    }

    @Test
    void testMarkAsProcessing() {
        document.markAsProcessing();
        assertEquals(ProcessingStatus.PROCESSING, document.getStatus());
        assertTrue(document.getStatus().isInProgress());
    }

    @Test
    void testMarkAsValidated() {
        document.markAsValidated();
        assertEquals(ProcessingStatus.VALIDATED, document.getStatus());
        assertNotNull(document.getProcessedAt());
        assertTrue(document.isProcessed());
    }

    @Test
    void testMarkAsFailed() {
        String errorMessage = "OCR processing failed";
        document.markAsFailed(errorMessage);
        
        assertEquals(ProcessingStatus.FAILED, document.getStatus());
        assertNotNull(document.getProcessedAt());
        assertTrue(document.isProcessed());
        assertTrue(document.hasErrors());
        assertTrue(document.getErrors().contains(errorMessage));
    }

    @Test
    void testAddError() {
        String error1 = "First error";
        String error2 = "Second error";
        
        document.addError(error1);
        document.addError(error2);
        
        assertEquals(2, document.getErrors().size());
        assertTrue(document.getErrors().contains(error1));
        assertTrue(document.getErrors().contains(error2));
        assertTrue(document.hasErrors());
    }

    @Test
    void testProcessingStatusEnumMethods() {
        assertEquals(ProcessingStatus.UPLOADED, document.getStatus());
        assertTrue(document.getStatus().canBeProcessed());
        assertFalse(document.getStatus().isCompleted());
        assertFalse(document.getStatus().isInProgress());

        document.setStatus(ProcessingStatus.PROCESSING);
        assertTrue(document.getStatus().isInProgress());
        assertFalse(document.getStatus().isCompleted());
        assertFalse(document.getStatus().canBeProcessed());

        document.setStatus(ProcessingStatus.VALIDATED);
        assertTrue(document.getStatus().isCompleted());
        assertFalse(document.getStatus().isInProgress());
        assertFalse(document.getStatus().canBeProcessed());
    }

    @Test
    void testToString() {
        String toString = document.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("test-invoice.pdf"));
        assertTrue(toString.contains("UPLOADED"));
    }
}