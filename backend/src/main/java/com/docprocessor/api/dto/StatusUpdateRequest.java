package com.docprocessor.api.dto;

import com.docprocessor.domain.enums.ProcessingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for updating document status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {
    
    @NotNull(message = "Status is required")
    private ProcessingStatus status;
    
    private String reason;
}