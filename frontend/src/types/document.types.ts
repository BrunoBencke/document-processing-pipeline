export enum ProcessingStatus {
  UPLOADED = 'UPLOADED',
  PROCESSING = 'PROCESSING', 
  VALIDATED = 'VALIDATED',
  FAILED = 'FAILED'
}

export interface InvoiceItem {
  description: string;
  quantity: number;
  unitPrice: number;
  total: number;
}

export interface DocumentMetadata {
  invoiceNumber: string;
  invoiceDate: string;
  totalAmount: number;
  items: InvoiceItem[];
  additionalFields?: Record<string, any>;
}

export interface OCRResult {
  text: string;
  confidence: number;
  language: string;
  processedAt: string;
  extractedData?: Record<string, any>;
  processingEngine?: string;
  processingTimeMs?: number;
}

export interface Document {
  id: string;
  filename: string;
  uploadedAt: string;
  processedAt?: string;
  status: ProcessingStatus;
  metadata?: DocumentMetadata;
  ocrResult?: OCRResult;
  errors?: string[];
  createdAt: string;
  updatedAt: string;
  downloadUrl?: string;
  fileSizeBytes?: number;
  contentType?: string;
  processingProgress?: number;
}

export interface UploadResponse {
  documentId: string;
  filename: string;
  status: ProcessingStatus;
  uploadedAt: string;
  message: string;
  downloadUrl?: string;
  fileSizeBytes?: number;
  contentType?: string;
  warnings?: string[];
}

export interface DocumentListResponse {
  documents: Document[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface DocumentFilters {
  status?: ProcessingStatus;
  searchTerm?: string;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'asc' | 'desc';
}

export interface ProcessingStatistics {
  total: number;
  uploaded: number;
  processing: number;
  validated: number;
  failed: number;
  averageProcessingTime?: number;
}

export interface WebSocketMessage {
  type: 'STATUS_UPDATE' | 'PROGRESS_UPDATE' | 'ERROR' | 'COMPLETED';
  documentId: string;
  status?: ProcessingStatus;
  progress?: number;
  message?: string;
  timestamp: string;
}

export interface ApiError {
  message: string;
  code?: string;
  details?: Record<string, any>;
  timestamp: string;
}

export interface ValidationError {
  field: string;
  message: string;
  rejectedValue?: any;
}