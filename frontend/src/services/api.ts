import axios, { AxiosResponse } from 'axios';
import {
  Document,
  UploadResponse,
  DocumentFilters,
  ProcessingStatistics,
  ProcessingStatus
} from '../types/document.types';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export interface PaginatedResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      direction: string;
    };
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

export const documentApi = {
  uploadDocument: async (file: File): Promise<UploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);

    const response: AxiosResponse<UploadResponse> = await api.post('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        const progress = progressEvent.total
          ? Math.round((progressEvent.loaded * 100) / progressEvent.total)
          : 0;
        console.log(`Upload progress: ${progress}%`);
      },
    });

    return response.data;
  },

  getDocuments: async (filters: DocumentFilters = {}): Promise<PaginatedResponse<Document>> => {
    const params = new URLSearchParams();

    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params.append(key, value.toString());
      }
    });

    const response: AxiosResponse<PaginatedResponse<Document>> = await api.get(`/documents?${params}`);
    return response.data;
  },

  getDocument: async (documentId: string): Promise<Document> => {
    const response: AxiosResponse<Document> = await api.get(`/documents/${documentId}`);
    return response.data;
  },

  getDocumentStatus: async (documentId: string): Promise<{ status: string; progress?: number }> => {
    const response = await api.get(`/documents/${documentId}/status`);
    return response.data;
  },

  downloadDocument: async (documentId: string): Promise<Blob> => {
    const response: AxiosResponse<Blob> = await api.get(`/documents/${documentId}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },

  deleteDocument: async (documentId: string): Promise<void> => {
    await api.delete(`/documents/${documentId}`);
  },

  updateDocumentStatus: async (id: string, status: ProcessingStatus): Promise<Document> => {
    const response: AxiosResponse<Document> = await api.put(
      `/documents/${id}/status`,
      { status }
    );
    return response.data;
  },

  getDocumentsByStatus: async (
    status: ProcessingStatus,
    page: number = 0,
    size: number = 20
  ): Promise<PaginatedResponse<Document>> => {
    const response: AxiosResponse<PaginatedResponse<Document>> = await api.get(
      `/documents/status/${status}?page=${page}&size=${size}`
    );
    return response.data;
  },

  getDocumentStats: async (): Promise<ProcessingStatistics> => {
    const response: AxiosResponse<ProcessingStatistics> = await api.get('/documents/stats');
    return response.data;
  },
};

export const fileUtils = {
  formatFileSize: (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  },

  isValidFileType: (file: File): boolean => {
    const allowedTypes = [
      'application/pdf',
      'image/jpeg',
      'image/jpg',
      'image/png'
    ];
    return allowedTypes.includes(file.type);
  },
  isValidFileSize: (file: File, maxSizeInMB: number = 50): boolean => {
    const maxSizeInBytes = maxSizeInMB * 1024 * 1024;
    return file.size <= maxSizeInBytes;
  },

  getFileIcon: (contentType?: string): string => {
    if (!contentType) return '';

    if (contentType.includes('pdf')) return '';
    if (contentType.includes('image')) return '';

    return '';
  },

  downloadBlob: (blob: Blob, filename: string): void => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },
};

export default api;