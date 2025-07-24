package com.docprocessor.domain.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OCRResultTest {

    private OCRResult ocrResult;

    @BeforeEach
    void setUp() {
        ocrResult = new OCRResult("Sample OCR text content", 0.95, "pt-BR");
    }

    @Test
    void testOCRResultCreation() {
        assertEquals("Sample OCR text content", ocrResult.getText());
        assertEquals(0.95, ocrResult.getConfidence());
        assertEquals("pt-BR", ocrResult.getLanguage());
        assertNotNull(ocrResult.getProcessedAt());
        assertEquals("SimulatedOCR", ocrResult.getProcessingEngine());
        assertNotNull(ocrResult.getExtractedData());
    }

    @Test
    void testConfidenceLevels() {
        ocrResult.setConfidence(0.85);
        assertTrue(ocrResult.isHighConfidence());
        assertFalse(ocrResult.isMediumConfidence());
        assertFalse(ocrResult.isLowConfidence());
        assertEquals(OCRResult.ConfidenceLevel.HIGH, ocrResult.getConfidenceLevel());

        ocrResult.setConfidence(0.70);
        assertFalse(ocrResult.isHighConfidence());
        assertTrue(ocrResult.isMediumConfidence());
        assertFalse(ocrResult.isLowConfidence());
        assertEquals(OCRResult.ConfidenceLevel.MEDIUM, ocrResult.getConfidenceLevel());

        ocrResult.setConfidence(0.45);
        assertFalse(ocrResult.isHighConfidence());
        assertFalse(ocrResult.isMediumConfidence());
        assertTrue(ocrResult.isLowConfidence());
        assertEquals(OCRResult.ConfidenceLevel.LOW, ocrResult.getConfidenceLevel());
    }

    @Test
    void testConfidencePercentage() {
        ocrResult.setConfidence(0.95);
        assertEquals("95.0%", ocrResult.getConfidencePercentage());

        ocrResult.setConfidence(0.8765);
        assertEquals("87.6%", ocrResult.getConfidencePercentage());

        ocrResult.setConfidence(null);
        assertEquals("0%", ocrResult.getConfidencePercentage());
    }

    @Test
    void testAddExtractedData() {
        ocrResult.addExtractedData("invoiceNumber", "INV-2024-001");
        ocrResult.addExtractedData("customerName", "ACME Corp");
        
        assertTrue(ocrResult.hasExtractedData());
        assertEquals("INV-2024-001", ocrResult.getExtractedData().get("invoiceNumber"));
        assertEquals("ACME Corp", ocrResult.getExtractedData().get("customerName"));
    }

    @Test
    void testSetExtractedData() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalAmount", 1500.00);
        data.put("currency", "BRL");
        
        ocrResult.setExtractedData(data);
        
        assertTrue(ocrResult.hasExtractedData());
        assertEquals(1500.00, ocrResult.getExtractedData().get("totalAmount"));
        assertEquals("BRL", ocrResult.getExtractedData().get("currency"));
    }

    @Test
    void testTextLength() {
        assertEquals("Sample OCR text content".length(), ocrResult.getTextLength());
        
        ocrResult.setText("");
        assertEquals(0, ocrResult.getTextLength());
        
        ocrResult.setText(null);
        assertEquals(0, ocrResult.getTextLength());
    }

    @Test
    void testProcessingTimeMs() {
        Long processingTime = 1500L;
        ocrResult.setProcessingTimeMs(processingTime);
        assertEquals(processingTime, ocrResult.getProcessingTimeMs());
    }

    @Test
    void testProcessingEngine() {
        ocrResult.setProcessingEngine("AWS Textract");
        assertEquals("AWS Textract", ocrResult.getProcessingEngine());
    }

    @Test
    void testConstructorWithAllFields() {
        Map<String, Object> data = new HashMap<>();
        data.put("test", "value");
        
        OCRResult result = new OCRResult(
            "Test text", 
            0.88, 
            "en", 
            data, 
            2000L
        );
        
        assertEquals("Test text", result.getText());
        assertEquals(0.88, result.getConfidence());
        assertEquals("en", result.getLanguage());
        assertEquals(2000L, result.getProcessingTimeMs());
        assertTrue(result.hasExtractedData());
        assertEquals("value", result.getExtractedData().get("test"));
    }

    @Test
    void testConfidenceLevelEnum() {
        assertEquals("High confidence (≥80%)", OCRResult.ConfidenceLevel.HIGH.getDescription());
        assertEquals("Medium confidence (60-79%)", OCRResult.ConfidenceLevel.MEDIUM.getDescription());
        assertEquals("Low confidence (<60%)", OCRResult.ConfidenceLevel.LOW.getDescription());
        assertEquals("Unknown confidence", OCRResult.ConfidenceLevel.UNKNOWN.getDescription());
    }

    @Test
    void testToString() {
        ocrResult.setProcessingTimeMs(500L);
        String toString = ocrResult.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("0.95"));
        assertTrue(toString.contains("pt-BR"));
        assertTrue(toString.contains("500"));
    }
}