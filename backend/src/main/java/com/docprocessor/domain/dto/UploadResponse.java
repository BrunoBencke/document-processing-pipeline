package com.docprocessor.domain.dto;

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
public class UploadResponse {

    @NotBlank(message = "Document ID is required")
    private String documentId;

    @NotBlank(message = "Filename is required")
    private String filename;

    @NotNull(message = "Status is required")
    private ProcessingStatus status;

    @NotNull(message = "Upload timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedAt;

    private String message;

    private String downloadUrl;

    private Long fileSizeBytes;

    private String contentType;

    private List<String> warnings;

}