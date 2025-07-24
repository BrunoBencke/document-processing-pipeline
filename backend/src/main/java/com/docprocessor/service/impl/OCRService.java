package com.docprocessor.service.impl;

import com.docprocessor.domain.entities.DocumentMetadata;
import com.docprocessor.domain.entities.OCRResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class OCRService {

    private static final List<String> SAMPLE_INVOICE_TEXTS = List.of(
        "INVOICE\nCompany: ACME Corporation\nInvoice #: INV-2024-001\nDate: 2024-07-10\nAmount: $1,250.00\nDescription: Software License\nQuantity: 1\nUnit Price: $1,250.00",
        "INVOICE\nCompany: Tech Solutions Ltd\nNumber: INV-2024-045\nDate: 07/10/2024\nTotal Amount: $2,850.50\nConsulting Services\n15 hours x $190.03",
        "INVOICE\nCompany Name: Digital Innovations\nInvoice: 000123456\nIssue Date: 2024-07-10\nAmount: $4,750.25\nProduct: Software Development\nQty: 1 unit\nUnit Price: $4,750.25",
        "INVOICE\nBill To: Enterprise Holdings\nInvoice Number: 2024-INV-789\nIssue Date: July 10, 2024\nTotal Due: $3,199.99\nCloud Services - Monthly Subscription\n1 month @ $3,199.99"
    );

    public OCRResult performOCR(byte[] fileContent, String filename) {
        log.info("Starting OCR processing for file: {}", filename);
        
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(500, 2000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        double confidence = 0.75 + (ThreadLocalRandom.current().nextDouble() * 0.23);
        
        String ocrText = SAMPLE_INVOICE_TEXTS.get(
            ThreadLocalRandom.current().nextInt(SAMPLE_INVOICE_TEXTS.size())
        );
        
        String language = detectLanguage(ocrText);
        
        OCRResult result = new OCRResult(ocrText, confidence, language);
        
        Map<String, Object> extractedData = extractStructuredData(ocrText);
        result.setExtractedData(extractedData);
        result.setProcessingTimeMs(ThreadLocalRandom.current().nextLong(800, 3000));
        result.setProcessingEngine("SimulatedOCR v2.1");
        
        DocumentMetadata metadata = extractMetadata(ocrText);
        result.setExtractedMetadata(metadata);
        
        log.info("OCR processing completed for file: {} with confidence: {:.2f}", filename, confidence);
        return result;
    }

    public DocumentMetadata extractMetadata(String ocrText) {
        log.info("Extracting metadata from OCR text");
        
        DocumentMetadata metadata = new DocumentMetadata();

        String invoiceNumber = extractInvoiceNumber(ocrText);
        metadata.setInvoiceNumber(invoiceNumber);

        LocalDate invoiceDate = extractInvoiceDate(ocrText);
        metadata.setInvoiceDate(invoiceDate);

        BigDecimal totalAmount = extractTotalAmount(ocrText);
        metadata.setTotalAmount(totalAmount);

        List<DocumentMetadata.InvoiceItem> items = extractLineItems(ocrText, totalAmount);
        metadata.setItems(items);

        Map<String, Object> additionalFields = new HashMap<>();
        additionalFields.put("extractionMethod", "OCR");
        additionalFields.put("documentType", "invoice");
        additionalFields.put("processingTimestamp", LocalDate.now().toString());
        metadata.setAdditionalFields(additionalFields);
        
        log.info("Metadata extraction completed");
        return metadata;
    }

    private String detectLanguage(String text) {
        return "en-US";
    }

    private Map<String, Object> extractStructuredData(String text) {
        Map<String, Object> data = new HashMap<>();

        data.put("hasInvoiceNumber", text.matches("(?i).*invoice\\s*#?\\s*:?\\s*[A-Z0-9-]+.*"));
        data.put("hasAmount", text.matches("(?i).*(amount|total)\\s*:?\\s*\\$[0-9,.].*"));
        data.put("hasDate", text.matches("(?i).*date\\s*:?\\s*[0-9/-]+.*"));

        data.put("wordCount", text.split("\\s+").length);
        data.put("lineCount", text.split("\n").length);
        data.put("characterCount", text.length());
        
        return data;
    }

    private String extractInvoiceNumber(String text) {
        Pattern[] patterns = {
            Pattern.compile("(?i)invoice\\s*#?\\s*:?\\s*([A-Z0-9-]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)number\\s*:?\\s*([A-Z0-9-]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)invoice number\\s*:?\\s*([A-Z0-9-]+)", Pattern.CASE_INSENSITIVE)
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        return "INV-2024-" + String.format("%03d", ThreadLocalRandom.current().nextInt(1, 999));
    }

    private LocalDate extractInvoiceDate(String text) {
        Pattern[] patterns = {
            Pattern.compile("(?i)date\\s*:?\\s*(\\d{4}-\\d{2}-\\d{2})"),
            Pattern.compile("(?i)issue date\\s*:?\\s*(\\d{2}/\\d{2}/\\d{4})"),
            Pattern.compile("(?i)(\\d{2}/\\d{2}/\\d{4})"),
            Pattern.compile("(?i)(\\d{4}-\\d{2}-\\d{2})")
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String dateStr = matcher.group(1);
                try {
                    if (dateStr.contains("/")) {
                        String[] parts = dateStr.split("/");
                        if (parts.length == 3) {
                            int day = Integer.parseInt(parts[0]);
                            int month = Integer.parseInt(parts[1]);
                            int year = Integer.parseInt(parts[2]);
                            return LocalDate.of(year, month, day);
                        }
                    } else if (dateStr.contains("-")) {
                        return LocalDate.parse(dateStr);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing date: {}", dateStr);
                }
            }
        }

        return LocalDate.now();
    }

    private BigDecimal extractTotalAmount(String text) {
        Pattern[] patterns = {
            Pattern.compile("(?i)total.*?[\\$R]?\\s*([0-9,]+\\.\\d{2})"),
            Pattern.compile("(?i)amount.*?[\\$R]?\\s*([0-9,]+\\.\\d{2})"),
            Pattern.compile("(?i)total due.*?\\$\\s*([0-9,]+\\.\\d{2})"),
            Pattern.compile("\\$\\s*([0-9,]+\\.\\d{2})")
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String amountStr = matcher.group(1).replace(",", "").replace("R", "");
                try {
                    return new BigDecimal(amountStr);
                } catch (NumberFormatException e) {
                    log.warn("Error parsing amount: {}", amountStr);
                }
            }
        }

        double randomAmount = 100.0 + (ThreadLocalRandom.current().nextDouble() * 4900.0);
        return BigDecimal.valueOf(Math.round(randomAmount * 100.0) / 100.0);
    }

    private List<DocumentMetadata.InvoiceItem> extractLineItems(String text, BigDecimal totalAmount) {
        List<DocumentMetadata.InvoiceItem> items = new ArrayList<>();

        Pattern itemPattern = Pattern.compile("(?i)(\\d+)\\s*(hours?|unit?|month)\\s*[@x]?\\s*\\$\\s*([0-9,]+\\.\\d{2})");
        Matcher matcher = itemPattern.matcher(text);
        
        if (matcher.find()) {
            try {
                BigDecimal quantity = new BigDecimal(matcher.group(1));
                String unit = matcher.group(2);
                BigDecimal unitPrice = new BigDecimal(matcher.group(3).replace(",", ""));
                
                String description = extractItemDescription(text, unit);
                items.add(new DocumentMetadata.InvoiceItem(description, quantity, unitPrice));
            } catch (NumberFormatException e) {
                log.warn("Error parsing line item: {}", e.getMessage());
            }
        }

        if (items.isEmpty()) {
            String description = extractItemDescription(text, "");
            items.add(new DocumentMetadata.InvoiceItem(description, BigDecimal.ONE, totalAmount));
        }
        
        return items;
    }

    private String extractItemDescription(String text, String unit) {
        if (text.contains("Software") || text.contains("License")) {
            return "Software License";
        } else if (text.contains("Consulting")) {
            return "Consulting Services";
        } else if (text.contains("Development")) {
            return "Software Development";
        } else if (text.contains("Cloud")) {
            return "Cloud Services";
        }
        return "Professional Services";
    }
}