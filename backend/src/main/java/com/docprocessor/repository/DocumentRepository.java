package com.docprocessor.repository;

import com.docprocessor.domain.entities.ProcessingDocument;
import com.docprocessor.domain.enums.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends MongoRepository<ProcessingDocument, String> {

    Page<ProcessingDocument> findByStatus(ProcessingStatus status, Pageable pageable);
    List<ProcessingDocument> findByStatus(ProcessingStatus status);
    long countByStatus(ProcessingStatus status);

    Optional<ProcessingDocument> findByFilename(String filename);
    List<ProcessingDocument> findByFilenameContainingIgnoreCase(String filename);

    @Query("{ 'metadata.invoiceNumber': ?0 }")
    Optional<ProcessingDocument> findByInvoiceNumber(String invoiceNumber);

    @Query("{ 'uploadedAt': { $gte: ?0, $lte: ?1 } }")
    List<ProcessingDocument> findDocumentsBetweenUploadDates(LocalDateTime startDate, LocalDateTime endDate);

    @Query("{ 'processedAt': { $gte: ?0, $lte: ?1 } }")
    List<ProcessingDocument> findDocumentsBetweenProcessedDates(LocalDateTime startDate, LocalDateTime endDate);

    @Query("{ 'metadata.invoiceDate': { $gte: ?0, $lte: ?1 } }")
    List<ProcessingDocument> findDocumentsBetweenInvoiceDates(String startDate, String endDate);

    @Query("{ $and: [ " +
           "{ 'status': ?0 }, " +
           "{ 'uploadedAt': { $gte: ?1, $lte: ?2 } }, " +
           "{ $or: [ " +
           "  { 'filename': { $regex: ?3, $options: 'i' } }, " +
           "  { 'metadata.invoiceNumber': { $regex: ?3, $options: 'i' } } " +
           "] } " +
           "] }")
    Page<ProcessingDocument> findByStatusAndDateRangeAndSearchTerm(
        ProcessingStatus status, 
        LocalDateTime startDate, 
        LocalDateTime endDate, 
        String searchTerm, 
        Pageable pageable
    );

    @Query("{ $or: [ " +
           "  { 'filename': { $regex: ?0, $options: 'i' } }, " +
           "  { 'metadata.invoiceNumber': { $regex: ?0, $options: 'i' } }, " +
           "  { 'ocrResult.text': { $regex: ?0, $options: 'i' } } " +
           "] }")
    Page<ProcessingDocument> searchDocuments(String searchTerm, Pageable pageable);

    @Query("{ 'status': 'UPLOADED' }")
    List<ProcessingDocument> findDocumentsToProcess();

    @Query("{ 'status': 'FAILED' }")
    List<ProcessingDocument> findFailedDocuments();

    @Query("{ 'errors': { $exists: true, $not: { $size: 0 } } }")
    List<ProcessingDocument> findDocumentsWithErrors();

    @Query("{ 'uploadedAt': { $gte: ?0 } }")
    List<ProcessingDocument> findRecentDocuments(LocalDateTime since);

    @Query(value = "{ 'status': ?0 }", count = true)
    long countDocumentsByStatus(ProcessingStatus status);

    Optional<ProcessingDocument> findByFileId(String fileId);

    @Query("{ 'uploadedAt': { $lt: ?0 } }")
    List<ProcessingDocument> findDocumentsOlderThan(LocalDateTime date);

    @Query(value = "{ 'metadata.invoiceNumber': ?0 }", exists = true)
    boolean existsByInvoiceNumber(String invoiceNumber);

    @Query("{ $group: { " +
           "_id: '$status', " +
           "count: { $sum: 1 }, " +
           "avgProcessingTime: { $avg: { $subtract: ['$processedAt', '$uploadedAt'] } } " +
           "} }")
    List<Object> getProcessingStatistics();
}