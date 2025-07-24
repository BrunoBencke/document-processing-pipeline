package com.docprocessor.service.impl;

import com.docprocessor.exception.ErrorCode;
import com.docprocessor.exception.DocumentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Implementation of StorageService using local file system.
 */
@Slf4j
@Service
public class StorageServiceImpl {
    
    @Value("${app.storage.upload-dir:uploads}")
    private String uploadDir;
    
    @Value("${app.storage.create-dirs:true}")
    private boolean createDirs;
    
    @PostConstruct
    public void init() {
        if (createDirs) {
            try {
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                    log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
                }
            } catch (IOException e) {
                log.error("Failed to create upload directory", e);
                throw new DocumentProcessingException("Failed to initialize storage", e);
            }
        }
    }
    
    public String storeFile(MultipartFile file) throws IOException {
        try {
            if (file.isEmpty()) {
                throw new DocumentProcessingException(ErrorCode.FILE_EMPTY);
            }
            
            String filename = sanitizeFilename(file.getOriginalFilename());
            String extension = getFileExtension(filename);
            String uniqueFilename = generateUniqueFilename(extension);
            
            Path uploadPath = Paths.get(uploadDir);
            Path filePath = uploadPath.resolve(uniqueFilename);
            
            Files.createDirectories(uploadPath);
            
            Path tempFile = Files.createTempFile(uploadPath, "upload_", extension);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                Files.move(tempFile, filePath, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.deleteIfExists(tempFile);
                throw e;
            }
            
            log.info("File stored successfully: {} -> {}", filename, uniqueFilename);
            return uniqueFilename;
            
        } catch (IOException e) {
            log.error("Failed to store file: {}", file.getOriginalFilename(), e);
            throw new DocumentProcessingException("Failed to store file", e);
        }
    }
    
    public byte[] getFileContent(String fileId) throws IOException {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileId);
            
            if (!Files.exists(filePath)) {
                throw new DocumentProcessingException(ErrorCode.STORAGE_NOT_FOUND, fileId);
            }
            
            return Files.readAllBytes(filePath);
            
        } catch (IOException e) {
            log.error("Failed to read file: {}", fileId, e);
            throw new DocumentProcessingException("Failed to read file", e);
        }
    }
    
    public InputStream getFileStream(String fileId) throws IOException {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileId);
            
            if (!Files.exists(filePath)) {
                throw new DocumentProcessingException(ErrorCode.STORAGE_NOT_FOUND, fileId);
            }
            
            return new BufferedInputStream(Files.newInputStream(filePath));
            
        } catch (IOException e) {
            log.error("Failed to open file stream: {}", fileId, e);
            throw new DocumentProcessingException("Failed to open file stream", e);
        }
    }
    
    public void deleteFile(String fileId) throws IOException {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileId);
            boolean deleted = Files.deleteIfExists(filePath);
            
            if (deleted) {
                log.info("File deleted: {}", fileId);
            } else {
                log.warn("File not found for deletion: {}", fileId);
            }
            
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileId, e);
            throw new DocumentProcessingException("Failed to delete file", e);
        }
    }
    
    public long getFileSize(String fileId) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileId);
            
            if (!Files.exists(filePath)) {
                return 0;
            }
            
            return Files.size(filePath);
            
        } catch (IOException e) {
            log.error("Failed to get file size: {}", fileId, e);
            return 0;
        }
    }
    
    public boolean fileExists(String fileId) {
        Path filePath = Paths.get(uploadDir).resolve(fileId);
        return Files.exists(filePath);
    }
    
    private String generateUniqueFilename(String extension) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s%s", timestamp, uuid, extension);
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }
    
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_")
                      .replaceAll("\\.\\.", "");
    }
}