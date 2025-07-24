package com.docprocessor.service.impl;

import com.docprocessor.domain.entities.DocumentMetadata;
import com.docprocessor.domain.entities.OCRResult;
import com.docprocessor.domain.entities.ProcessingDocument;
import com.docprocessor.domain.enums.ProcessingStatus;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Document validation service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationServiceImpl {
    
    @Data
    @Builder
    public static class ValidationResult {
        
        @Builder.Default
        private final List<String> errors = new ArrayList<>();
        
        @Builder.Default
        private final List<String> warnings = new ArrayList<>();
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public static ValidationResult valid() {
            return ValidationResult.builder().build();
        }
        
        public static ValidationResult invalid(String error) {
            return ValidationResult.builder()
                    .errors(List.of(error))
                    .build();
        }
    }
    
    @Value("${app.validation.ocr.min-confidence:0.70}")
    private double minOcrConfidence;
    
    @Value("${app.validation.amount.max:100000.00}")
    private BigDecimal maxAmount;
    
    @Value("${app.validation.amount.min:0.01}")
    private BigDecimal minAmount;
    
    @Value("${app.validation.invoice.date-range-years:1}")
    private int invoiceDateRangeYears;
    
    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile("^[A-Za-z0-9\\-_]+$");
    
    public ValidationResult validateDocument(ProcessingDocument document) {
        log.info("Starting validation for document: {}", document.getId());
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        validateBasicProperties(document, errors);
        
        if (document.getOcrResult() != null) {
            validateOCRResult(document.getOcrResult(), errors, warnings);
        } else {
            errors.add("OCR result is missing");
        }
        
        if (document.getMetadata() != null) {
            validateMetadata(document.getMetadata(), errors, warnings);
        } else {
            errors.add("Document metadata is missing");
        }
        
        if (document.getOcrResult() != null && document.getMetadata() != null) {
            validateConsistency(document.getOcrResult(), document.getMetadata(), errors, warnings);
        }
        
        ValidationResult result = ValidationResult.builder()
                .errors(errors)
                .warnings(warnings)
                .build();
        
        log.info("Validation completed for document: {} - Valid: {}, Errors: {}, Warnings: {}", 
                document.getId(), result.isValid(), errors.size(), warnings.size());
        
        return result;
    }
    
    private void validateBasicProperties(ProcessingDocument document, List<String> errors) {
        if (document.getFilename() == null || document.getFilename().trim().isEmpty()) {
            errors.add("Filename is required");
        }
        
        if (document.getFileId() == null || document.getFileId().trim().isEmpty()) {
            errors.add("File reference is missing");
        }
        
        if (document.getUploadedAt() == null) {
            errors.add("Upload timestamp is missing");
        }
        
        if (document.getStatus() == null) {
            errors.add("Document status is missing");
        }
    }

    private void validateOCRResult(OCRResult ocrResult, List<String> errors, List<String> warnings) {
        if (ocrResult.getConfidence() == null) {
            errors.add("OCR confidence is missing");
        } else if (ocrResult.getConfidence() < minOcrConfidence) {
            if (ocrResult.getConfidence() < 0.50) {
                errors.add(String.format("OCR confidence too low: %.2f%% (minimum: %.0f%%)", 
                    ocrResult.getConfidence() * 100, minOcrConfidence * 100));
            } else {
                warnings.add(String.format("OCR confidence is below recommended threshold: %.2f%% (recommended: %.0f%%)", 
                    ocrResult.getConfidence() * 100, minOcrConfidence * 100));
            }
        }
        
        if (ocrResult.getText() == null || ocrResult.getText().trim().isEmpty()) {
            errors.add("OCR extracted text is empty");
        } else if (ocrResult.getText().length() < 10) {
            warnings.add("OCR extracted text is very short, may indicate poor quality scan");
        }
        
        if (ocrResult.getLanguage() == null || ocrResult.getLanguage().trim().isEmpty()) {
            warnings.add("OCR language detection failed");
        }
    }

    private void validateMetadata(DocumentMetadata metadata, List<String> errors, List<String> warnings) {
        if (metadata.getInvoiceNumber() == null || metadata.getInvoiceNumber().trim().isEmpty()) {
            errors.add("Invoice number is required");
        } else if (!INVOICE_NUMBER_PATTERN.matcher(metadata.getInvoiceNumber()).matches()) {
            errors.add("Invoice number contains invalid characters");
        }
        
        if (metadata.getInvoiceDate() == null) {
            errors.add("Invoice date is required");
        } else {
            LocalDate today = LocalDate.now();
            LocalDate minDate = today.minusYears(invoiceDateRangeYears);
            LocalDate maxDate = today.plusYears(invoiceDateRangeYears);
            
            if (metadata.getInvoiceDate().isBefore(minDate)) {
                warnings.add(String.format("Invoice date is more than %d year(s) old", invoiceDateRangeYears));
            } else if (metadata.getInvoiceDate().isAfter(maxDate)) {
                errors.add(String.format("Invoice date cannot be more than %d year(s) in the future", invoiceDateRangeYears));
            }
        }
        
        if (metadata.getTotalAmount() == null) {
            errors.add("Total amount is required");
        } else {
            if (metadata.getTotalAmount().compareTo(minAmount) < 0) {
                errors.add("Total amount must be greater than " + minAmount);
            } else if (metadata.getTotalAmount().compareTo(maxAmount) > 0) {
                errors.add("Total amount exceeds maximum allowed: " + maxAmount);
            }
        }
        
        if (metadata.getItems() == null || metadata.getItems().isEmpty()) {
            warnings.add("No line items found");
        } else {
            validateLineItems(metadata.getItems(), errors, warnings);
        }
    }

    private void validateLineItems(List<DocumentMetadata.InvoiceItem> items, List<String> errors, List<String> warnings) {
        for (int i = 0; i < items.size(); i++) {
            DocumentMetadata.InvoiceItem item = items.get(i);
            String itemPrefix = "Item " + (i + 1) + ": ";
            
            if (item.getDescription() == null || item.getDescription().trim().isEmpty()) {
                errors.add(itemPrefix + "Description is required");
            }
            
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(itemPrefix + "Quantity must be greater than zero");
            }
            
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(itemPrefix + "Unit price must be greater than zero");
            }
        }
    }

    private void validateConsistency(OCRResult ocrResult, DocumentMetadata metadata, List<String> errors, List<String> warnings) {
        String ocrText = ocrResult.getText().toLowerCase();
        
        if (metadata.getInvoiceNumber() != null && !ocrText.contains(metadata.getInvoiceNumber().toLowerCase())) {
            warnings.add("Invoice number not found in OCR text");
        }
        
        if (metadata.getTotalAmount() != null) {
            String amountStr = metadata.getTotalAmount().toString();
            if (!ocrText.contains(amountStr)) {
                warnings.add("Total amount not clearly visible in OCR text");
            }
        }
    }
}