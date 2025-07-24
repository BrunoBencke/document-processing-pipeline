package com.docprocessor.service.impl;

import com.docprocessor.domain.dto.DocumentDTO;
import com.docprocessor.domain.dto.UploadResponse;
import com.docprocessor.domain.entities.ProcessingDocument;
import com.docprocessor.domain.enums.ProcessingStatus;
import com.docprocessor.exception.*;
import com.docprocessor.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of DocumentService with enterprise features.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl {
    
    private final DocumentRepository repository;
    private final StorageServiceImpl storageService;
    private final OCRService ocrService;
    private final ValidationServiceImpl validationService;
    private final MongoTemplate mongoTemplate;
    
    @Value("${app.document.max-file-size:52428800}")
    private long maxFileSize;
    
    @Value("${app.document.allowed-types:application/pdf,image/jpeg,image/png}")
    private String[] allowedTypes;
    
    
    @Transactional
    public UploadResponse uploadDocument(MultipartFile file) {
        try {
            log.info("Starting document upload: filename={}, size={}", 
                    file.getOriginalFilename(), file.getSize());
            
            if (file.isEmpty()) {
                throw new ValidationException(ErrorCode.FILE_EMPTY);
            }
            if (file.getSize() > maxFileSize) {
                throw new ValidationException(ErrorCode.FILE_TOO_LARGE, maxFileSize / (1024 * 1024));
            }
            
            String fileId = storageService.storeFile(file);
            
            ProcessingDocument document = ProcessingDocument.builder()
                    .filename(sanitizeFilename(file.getOriginalFilename()))
                    .fileId(fileId)
                    .status(ProcessingStatus.UPLOADED)
                    .build();
            
            document = repository.save(document);
            
            processDocumentAsync(document.getId());
            
            log.info("Document uploaded successfully: id={}", document.getId());
            
            return UploadResponse.builder()
                    .documentId(document.getId())
                    .filename(document.getFilename())
                    .status(document.getStatus())
                    .uploadedAt(LocalDateTime.now())
                    .message("Document uploaded successfully and queued for processing")
                    .build();
                    
        } catch (ValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to store file", e);
        } catch (Exception e) {
            log.error("Failed to upload document", e);
            throw new DocumentProcessingException("Upload failed", e);
        }
    }
    
    @Transactional(readOnly = true)
    public Optional<DocumentDTO> getDocumentById(String documentId) {
        try {
            DocumentDTO doc = getDocument(documentId);
            return Optional.of(doc);
        } catch (DocumentProcessingException e) {
            return Optional.empty();
        }
    }
    
    @Transactional(readOnly = true)
    private DocumentDTO getDocument(String documentId) {
        log.debug("Retrieving document: {}", documentId);
        
        ProcessingDocument document = repository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException(ErrorCode.DOCUMENT_NOT_FOUND, documentId));
                
        return convertToDTO(document);
    }
    
    public InputStream downloadDocument(String documentId) {
        log.info("Downloading document: {}", documentId);
        
        ProcessingDocument document = repository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException(ErrorCode.DOCUMENT_NOT_FOUND, documentId));
                
        try {
            
            return storageService.getFileStream(document.getFileId());
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to download file", e);
        }
    }
    
    @Transactional(readOnly = true)
    public Page<DocumentDTO> getDocuments(ProcessingStatus status, Pageable pageable) {
        log.debug("Getting documents: status={}", status);
        
        Query query = buildQuery(status, null);
        
        long total = mongoTemplate.count(query, ProcessingDocument.class);
        
        query.with(pageable);
        List<ProcessingDocument> documents = mongoTemplate.find(query, ProcessingDocument.class);
        
        List<DocumentDTO> dtos = documents.stream()
                .map(this::convertToDTO)
                .toList();
                
        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, total);
    }
    
    @Transactional
    public DocumentDTO updateDocumentStatus(String documentId, ProcessingStatus status) {
        log.info("Updating document status: id={}, status={}", documentId, status);
        
        ProcessingDocument document = repository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException(ErrorCode.DOCUMENT_NOT_FOUND, documentId));
                
        ProcessingStatus oldStatus = document.getStatus();
        
        if (!isValidStatusTransition(oldStatus, status)) {
            throw new ValidationException(ErrorCode.DOCUMENT_INVALID_STATUS, oldStatus, status);
        }
        
        document.setStatus(status);
        document.setUpdatedAt(LocalDateTime.now());
        
        if (status == ProcessingStatus.PROCESSING) {
            document.markAsProcessing();
        } else if (status == ProcessingStatus.VALIDATED) {
            document.markAsValidated();
        } else if (status == ProcessingStatus.FAILED) {
            document.markAsFailed("Manual status update");
        }
        
        document = repository.save(document);
        
        return convertToDTO(document);
    }
    
    @Transactional
    public void deleteDocument(String documentId) {
        log.info("Deleting document: {}", documentId);
        
        ProcessingDocument document = repository.findById(documentId)
                .orElseThrow(() -> new DocumentProcessingException(ErrorCode.DOCUMENT_NOT_FOUND, documentId));
                
        try {
            storageService.deleteFile(document.getFileId());
            
            repository.deleteById(documentId);
                    
            log.info("Document deleted successfully: {}", documentId);
            
        } catch (IOException e) {
            log.error("Failed to delete file from storage: {}", document.getFileId(), e);
            repository.deleteById(documentId);
        }
    }
    
    public CompletableFuture<Void> processDocumentAsync(String documentId) {
        log.info("Starting async processing for document: {}", documentId);
        
        try {
            ProcessingDocument document = repository.findById(documentId)
                    .orElseThrow(() -> new DocumentProcessingException(ErrorCode.DOCUMENT_NOT_FOUND, documentId));
                    
            document.markAsProcessing();
            repository.save(document);
            
            byte[] fileContent = storageService.getFileContent(document.getFileId());
            
            var ocrResult = ocrService.performOCR(fileContent, document.getFilename());
            document.setOcrResult(ocrResult);
            
            if (ocrResult != null && StringUtils.hasText(ocrResult.getText())) {
                var metadata = ocrResult.getExtractedMetadata();
                document.setMetadata(metadata);
                
                var validationResult = validationService.validateDocument(document);
                
                if (validationResult.isValid()) {
                    document.markAsValidated();
                } else {
                    document.markAsFailed(String.join(", ", validationResult.getErrors()));
                }
            } else {
                document.markAsFailed("OCR failed to extract text");
            }
            
            document = repository.save(document);
            
            log.info("Document processing completed: id={}, status={}", 
                    documentId, document.getStatus());
                    
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Document processing failed: {}", documentId, e);
            
            repository.findById(documentId).ifPresent(doc -> {
                doc.markAsFailed(e.getMessage());
                repository.save(doc);
            });
            
            return CompletableFuture.failedFuture(e);
        }
    }
    
    
    private DocumentDTO convertToDTO(ProcessingDocument document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .uploadedAt(document.getUploadedAt())
                .processedAt(document.getProcessedAt())
                .status(document.getStatus())
                .metadata(document.getMetadata())
                .ocrResult(document.getOcrResult())
                .errors(document.getErrors())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .downloadUrl("/api/documents/" + document.getId() + "/download")
                .fileSizeBytes(getFileSize(document.getFileId()))
                .build();
    }
    
    private Query buildQuery(ProcessingStatus status, String searchTerm) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();
        
        if (status != null) {
            criteria.add(Criteria.where("status").is(status));
        }
        
        if (StringUtils.hasText(searchTerm)) {
            criteria.add(new Criteria().orOperator(
                    Criteria.where("filename").regex(searchTerm, "i"),
                    Criteria.where("metadata.invoiceNumber").regex(searchTerm, "i")
            ));
        }
        
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }
        
        return query;
    }
    
    private String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_")
                      .replaceAll("\\.\\.", "");
    }
    
    private boolean isValidStatusTransition(ProcessingStatus from, ProcessingStatus to) {
        return switch (from) {
            case UPLOADED -> to == ProcessingStatus.PROCESSING || to == ProcessingStatus.FAILED;
            case PROCESSING -> to == ProcessingStatus.VALIDATED || to == ProcessingStatus.FAILED;
            case VALIDATED, FAILED -> to == ProcessingStatus.UPLOADED;
        };
    }
    
    private Long getFileSize(String fileId) {
        try {
            return storageService.getFileSize(fileId);
        } catch (Exception e) {
            log.warn("Failed to get file size for: {}", fileId);
            return null;
        }
    }
    
    @Transactional(readOnly = true)
    public Optional<ProcessingDocument> getDocumentEntityById(String documentId) {
        log.debug("Getting document entity: {}", documentId);
        return repository.findById(documentId);
    }
}