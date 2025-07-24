package com.docprocessor.api.controllers;

import com.docprocessor.api.dto.StatusUpdateRequest;
import com.docprocessor.domain.dto.DocumentDTO;
import com.docprocessor.domain.dto.UploadResponse;
import com.docprocessor.domain.entities.ProcessingDocument;
import com.docprocessor.domain.enums.ProcessingStatus;
import com.docprocessor.exception.DocumentProcessingException;
import com.docprocessor.exception.ErrorCode;
import com.docprocessor.exception.ValidationException;
import com.docprocessor.repository.DocumentRepository;
import com.docprocessor.service.impl.DocumentServiceImpl;
import com.docprocessor.service.impl.StorageServiceImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * REST controller for document management operations.
 * Provides endpoints for upload, retrieval, status updates, and deletion.
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DocumentController {

    private final DocumentServiceImpl documentService;
    private final StorageServiceImpl storageService;
    private final DocumentRepository repository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        
        log.info("Received upload request: file={}, size={}", 
                file.getOriginalFilename(), file.getSize());
        
        UploadResponse response = documentService.uploadDocument(file);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<DocumentDTO>> getDocuments(
            @RequestParam(value = "status", required = false) ProcessingStatus status,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(value = "sort", defaultValue = "uploadedAt") String sortBy,
            @RequestParam(value = "direction", defaultValue = "desc") String sortDirection) {
        
        log.debug("Getting documents: status={}, page={}, size={}", 
                status, page, size);
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<DocumentDTO> documents = documentService.getDocuments(status, pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocument(
            @PathVariable String id) {
        
        log.debug("Getting document: {}", id);
        
        DocumentDTO document = documentService.getDocumentById(id)
                .orElseThrow(() -> new DocumentProcessingException(ErrorCode.DOCUMENT_NOT_FOUND, id));
        
        return ResponseEntity.ok(document);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable String id) {
        
        log.info("Download request for document: {}", id);
        
        ProcessingDocument document = documentService.getDocumentEntityById(id)
                .orElseThrow(() -> new DocumentProcessingException(ErrorCode.DOCUMENT_NOT_FOUND, id));
        
        if (document.getFileId() == null) {
            throw new ValidationException(ErrorCode.FILE_NOT_FOUND, "Document file is not available");
        }
        
        InputStream fileStream;
        try {
            fileStream = storageService.getFileStream(document.getFileId());
        } catch (IOException e) {
            log.error("Failed to get file stream", e);
            throw new DocumentProcessingException("Failed to read file", e);
        }
        long fileSize = storageService.getFileSize(document.getFileId());
        
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + sanitizeFilename(document.getFilename()) + "\"");
        headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize));
        headers.add("X-Document-Id", document.getId());
        
        MediaType mediaType = determineMediaType(document.getFilename());
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(new InputStreamResource(fileStream));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<DocumentDTO> updateDocumentStatus(
            @PathVariable String id,
            @Valid @RequestBody StatusUpdateRequest statusUpdate) {
        
        log.info("Updating status for document: {} to {}", id, statusUpdate.getStatus());
        
        DocumentDTO updatedDocument = documentService.updateDocumentStatus(id, statusUpdate.getStatus());
        return ResponseEntity.ok(updatedDocument);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable String id) {
        
        log.info("Deleting document: {}", id);
        
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<DocumentDTO>> getDocumentsByStatus(
            @PathVariable ProcessingStatus status,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        
        log.debug("Getting documents by status: {}", status);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        Page<DocumentDTO> documents = documentService.getDocuments(status, pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDocumentStats() {
        log.debug("Getting document statistics");
        
        Map<String, Object> stats = Map.of(
            "totalDocuments", repository.count(),
            "message", "Statistics endpoint"
        );
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Sanitize filename to prevent security issues
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "document";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_")
                      .replaceAll("\\.\\.", "")
                      .replaceAll("^\\.", "_");
    }
    
    /**
     * Determine media type from filename
     */
    private MediaType determineMediaType(String filename) {
        if (filename == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        
        String lowercaseFilename = filename.toLowerCase();
        if (lowercaseFilename.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        } else if (lowercaseFilename.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (lowercaseFilename.endsWith(".jpg") || lowercaseFilename.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        } else if (lowercaseFilename.endsWith(".txt")) {
            return MediaType.TEXT_PLAIN;
        }
        
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}