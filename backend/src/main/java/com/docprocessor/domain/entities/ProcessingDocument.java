package com.docprocessor.domain.entities;

import com.docprocessor.domain.enums.ProcessingStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document(collection = "documents")
public class ProcessingDocument {

    @Id
    private String id;

    @NotBlank(message = "Filename is required")
    private String filename;

    private String fileId;

    @NotNull(message = "Upload date is required")
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedAt;

    @NotNull(message = "Status is required")
    @Indexed
    private ProcessingStatus status;

    private DocumentMetadata metadata;

    private OCRResult ocrResult;

    @Builder.Default
    private List<String> errors = new ArrayList<>();


    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public ProcessingDocument(String filename) {
        this.filename = filename;
        this.status = ProcessingStatus.UPLOADED;
        this.uploadedAt = LocalDateTime.now();
        this.errors = new ArrayList<>();
    }

    public void setStatus(ProcessingStatus status) {
        this.status = status;
        if (status.isCompleted()) {
            this.processedAt = LocalDateTime.now();
        }
    }

    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }
    public boolean isProcessed() {
        return this.status.isCompleted();
    }

    public boolean hasErrors() {
        return this.errors != null && !this.errors.isEmpty();
    }

    public void markAsProcessing() {
        this.status = ProcessingStatus.PROCESSING;
    }

    public void markAsValidated() {
        this.status = ProcessingStatus.VALIDATED;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsFailed(String error) {
        this.status = ProcessingStatus.FAILED;
        this.processedAt = LocalDateTime.now();
        if (error != null) {
            this.addError(error);
        }
    }

}