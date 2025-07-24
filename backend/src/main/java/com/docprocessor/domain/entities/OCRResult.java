package com.docprocessor.domain.entities;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OCRResult {

    @NotBlank(message = "Text is required")
    private String text;

    @NotNull(message = "Confidence is required")
    @DecimalMin(value = "0.0", message = "Confidence must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Confidence must be between 0.0 and 1.0")
    private Double confidence;

    @NotBlank(message = "Language is required")
    @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Language must be in ISO format (e.g., 'en', 'pt-BR')")
    private String language;

    private LocalDateTime processedAt;

    private Map<String, Object> extractedData;
    
    private DocumentMetadata extractedMetadata;

    private String processingEngine;

    private Long processingTimeMs;

    public OCRResult(String text, Double confidence, String language) {
        this.text = text;
        this.confidence = confidence;
        this.language = language;
        this.processedAt = LocalDateTime.now();
        this.extractedData = new HashMap<>();
        this.processingEngine = "SimulatedOCR";
    }

    public OCRResult(String text, Double confidence, String language, 
                     Map<String, Object> extractedData, Long processingTimeMs) {
        this.text = text;
        this.confidence = confidence;
        this.language = language;
        this.processedAt = LocalDateTime.now();
        this.extractedData = extractedData != null ? extractedData : new HashMap<>();
        this.processingTimeMs = processingTimeMs;
        this.processingEngine = "SimulatedOCR";
    }

    public void setExtractedData(Map<String, Object> extractedData) {
        this.extractedData = extractedData != null ? extractedData : new HashMap<>();
    }

    public void addExtractedData(String key, Object value) {
        if (this.extractedData == null) {
            this.extractedData = new HashMap<>();
        }
        this.extractedData.put(key, value);
    }
    public boolean isHighConfidence() {
        return confidence != null && confidence >= 0.8;
    }

    public boolean isMediumConfidence() {
        return confidence != null && confidence >= 0.6 && confidence < 0.8;
    }

    public boolean isLowConfidence() {
        return confidence != null && confidence < 0.6;
    }

    public ConfidenceLevel getConfidenceLevel() {
        if (confidence == null) {
            return ConfidenceLevel.UNKNOWN;
        }
        
        if (confidence >= 0.8) {
            return ConfidenceLevel.HIGH;
        } else if (confidence >= 0.6) {
            return ConfidenceLevel.MEDIUM;
        } else {
            return ConfidenceLevel.LOW;
        }
    }

    public String getConfidencePercentage() {
        if (confidence == null) {
            return "0%";
        }
        return String.format("%.1f%%", confidence * 100);
    }

    public boolean hasExtractedData() {
        return extractedData != null && !extractedData.isEmpty();
    }

    public int getTextLength() {
        return text != null ? text.length() : 0;
    }

    public enum ConfidenceLevel {
        HIGH("High confidence (≥80%)"),
        MEDIUM("Medium confidence (60-79%)"),
        LOW("Low confidence (<60%)"),
        UNKNOWN("Unknown confidence");

        private final String description;

        ConfidenceLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}