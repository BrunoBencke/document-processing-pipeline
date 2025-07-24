import { useState, useEffect, useCallback } from 'react';
import { documentApi, PaginatedResponse } from '../services/api';
import {
  Document,
  DocumentFilters,
  UploadResponse,
  ProcessingStatus,
} from '../types/document.types';

export interface UseDocumentsResult {
  documents: Document[];
  loading: boolean;
  error: string | null;
  totalElements: number;
  totalPages: number;
  currentPage: number;
  hasNext: boolean;
  hasPrevious: boolean;
  filters: DocumentFilters;
  setFilters: (filters: DocumentFilters) => void;
  refreshDocuments: () => Promise<void>;
  loadMore: () => Promise<void>;
  updateDocument: (id: string, updates: Partial<Document>) => void;
  removeDocument: (id: string) => void;
}

const DEFAULT_FILTERS: DocumentFilters = {
  page: 0,
  size: 20,
  sortBy: 'uploadedAt',
  sortDirection: 'desc',
};

export const useDocuments = (initialFilters: DocumentFilters = {}): UseDocumentsResult => {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [paginationInfo, setPaginationInfo] = useState({
    totalElements: 0,
    totalPages: 0,
    currentPage: 0,
    hasNext: false,
    hasPrevious: false,
  });

  const [filters, setFiltersState] = useState<DocumentFilters>({
    ...DEFAULT_FILTERS,
    ...initialFilters,
  });

  const fetchDocuments = useCallback(async (newFilters?: DocumentFilters, append = false) => {
    const currentFilters = newFilters || filters;
    setLoading(true);
    setError(null);

    try {
      const response: PaginatedResponse<Document> = await documentApi.getDocuments(currentFilters);

      const newDocuments = response.content || [];

      if (append) {
        setDocuments(prev => [...prev, ...newDocuments]);
      } else {
        setDocuments(newDocuments);
      }

      setPaginationInfo({
        totalElements: response.totalElements,
        totalPages: response.totalPages,
        currentPage: response.pageable.pageNumber,
        hasNext: !response.last,
        hasPrevious: !response.first,
      });
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to fetch documents');
      console.error('Error fetching documents:', err);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  const setFilters = useCallback((newFilters: DocumentFilters) => {
    const updatedFilters = { ...filters, ...newFilters, page: 0 };
    setFiltersState(updatedFilters);
    fetchDocuments(updatedFilters);
  }, [filters, fetchDocuments]);

  const refreshDocuments = useCallback(async () => {
    await fetchDocuments();
  }, [fetchDocuments]);

  const loadMore = useCallback(async () => {
    if (paginationInfo.hasNext && !loading) {
      const nextPageFilters = { ...filters, page: paginationInfo.currentPage + 1 };
      await fetchDocuments(nextPageFilters, true);
    }
  }, [filters, paginationInfo.hasNext, paginationInfo.currentPage, loading, fetchDocuments]);

  const updateDocument = useCallback((id: string, updates: Partial<Document>) => {
    setDocuments(prev =>
      prev.map(doc =>
        doc.id === id ? { ...doc, ...updates } : doc
      )
    );
  }, []);

  const removeDocument = useCallback((id: string) => {
    setDocuments(prev => prev.filter(doc => doc.id !== id));
    setPaginationInfo(prev => ({
      ...prev,
      totalElements: prev.totalElements - 1,
    }));
  }, []);

  useEffect(() => {
    fetchDocuments();
  }, []);

  useEffect(() => {
    const processingDocuments = documents.filter(doc => doc.status === ProcessingStatus.PROCESSING);

    if (processingDocuments.length > 0) {
      const interval = setInterval(() => {
        refreshDocuments();
      }, 5000); // Refresh every 5 seconds

      return () => clearInterval(interval);
    }

    return undefined;
  }, [documents, refreshDocuments]);

  return {
    documents,
    loading,
    error,
    totalElements: paginationInfo.totalElements,
    totalPages: paginationInfo.totalPages,
    currentPage: paginationInfo.currentPage,
    hasNext: paginationInfo.hasNext,
    hasPrevious: paginationInfo.hasPrevious,
    filters,
    setFilters,
    refreshDocuments,
    loadMore,
    updateDocument,
    removeDocument,
  };
};

export const useDocumentUpload = () => {
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const uploadDocument = useCallback(async (file: File): Promise<UploadResponse> => {
    setUploading(true);
    setUploadProgress(0);
    setError(null);

    try {
      const response = await documentApi.uploadDocument(file);
      setUploadProgress(100);
      return response;
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to upload document');
      throw err;
    } finally {
      setUploading(false);
      setTimeout(() => setUploadProgress(0), 1000);
    }
  }, []);

  return {
    uploadDocument,
    uploading,
    uploadProgress,
    error,
  };
};

export const useDocumentOperations = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const deleteDocument = useCallback(async (id: string) => {
    setLoading(true);
    setError(null);

    try {
      await documentApi.deleteDocument(id);
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to delete document');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const updateDocumentStatus = useCallback(async (id: string, status: ProcessingStatus) => {
    setLoading(true);
    setError(null);

    try {
      const updatedDocument = await documentApi.updateDocumentStatus(id, status);
      return updatedDocument;
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to update document status');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const downloadDocument = useCallback(async (id: string, filename: string) => {
    setLoading(true);
    setError(null);

    try {
      const blob = await documentApi.downloadDocument(id);

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to download document');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    deleteDocument,
    updateDocumentStatus,
    downloadDocument,
    loading,
    error,
  };
};

export const useDocument = (id: string) => {
  const [document, setDocument] = useState<Document | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchDocument = useCallback(async () => {
    if (!id) return;

    setLoading(true);
    setError(null);

    try {
      const doc = await documentApi.getDocument(id);
      setDocument(doc);
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to fetch document');
      console.error('Error fetching document:', err);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchDocument();
  }, [fetchDocument]);

  return {
    document,
    loading,
    error,
    refetch: fetchDocument,
  };
};

export default useDocuments;