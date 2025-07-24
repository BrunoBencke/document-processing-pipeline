package com.docprocessor.domain.dto;

import com.docprocessor.domain.entities.DocumentMetadata;
import com.docprocessor.domain.entities.OCRResult;
import com.docprocessor.domain.enums.ProcessingStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {

    private String id;

    @NotBlank(message = "Filename is required")
    private String filename;

    @NotNull(message = "Upload date is required")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedAt;

    @NotNull(message = "Status is required")
    private ProcessingStatus status;

    private DocumentMetadata metadata;

    private OCRResult ocrResult;

    private List<String> errors;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private String downloadUrl;
    private Long fileSizeBytes;
    private String contentType;
    private Double processingProgress;

    public DocumentDTO(String id, String filename, ProcessingStatus status, LocalDateTime uploadedAt) {
        this.id = id;
        this.filename = filename;
        this.status = status;
        this.uploadedAt = uploadedAt;
    }

    public boolean isProcessed() {
        return status != null && status.isCompleted();
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public String getStatusDescription() {
        return status != null ? status.getDescription() : "Unknown";
    }

    public String getFileSizeFormatted() {
        if (fileSizeBytes == null) {
            return "Unknown";
        }
        
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " B";
        } else if (fileSizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", fileSizeBytes / 1024.0);
        } else {
            return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0));
        }
    }

}