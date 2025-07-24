import React, { useCallback, useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  LinearProgress,
  Alert,
  Chip,
} from '@mui/material';
import {
  CloudUpload as UploadIcon,
  CheckCircle as SuccessIcon,
  Error as ErrorIcon,
} from '@mui/icons-material';
import { useDropzone } from 'react-dropzone';
import { useDocumentUpload } from '../../hooks/useDocuments';
import { fileUtils } from '../../services/api';

interface DocumentUploadProps {
  onUploadSuccess?: (response: any) => void;
  onUploadError?: (error: string) => void;
  disabled?: boolean;
  maxFiles?: number;
}

const DocumentUpload: React.FC<DocumentUploadProps> = ({
  onUploadSuccess,
  onUploadError,
  disabled = false,
  maxFiles = 1,
}) => {
  const { uploadDocument, uploading, uploadProgress, error } = useDocumentUpload();
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const onDrop = useCallback((acceptedFiles: File[]) => {
    const validFiles = acceptedFiles.filter(file => {
      if (!fileUtils.isValidFileType(file)) {
        onUploadError?.(`File type not supported: ${file.name}`);
        return false;
      }
      if (!fileUtils.isValidFileSize(file)) {
        onUploadError?.(`File too large: ${file.name} (max 50MB)`);
        return false;
      }
      return true;
    });

    if (validFiles.length === 0) return;

    handleUpload(validFiles);
  }, [onUploadError]);

  const { getRootProps, getInputProps, isDragActive, isDragReject } = useDropzone({
    onDrop,
    accept: {
      'application/pdf': ['.pdf'],
      'image/jpeg': ['.jpg', '.jpeg'],
      'image/png': ['.png'],
    },
    maxFiles,
    disabled: disabled || uploading,
    multiple: maxFiles > 1,
  });

  const handleUpload = async (files: File[]) => {
    try {
      for (const file of files) {
        const response = await uploadDocument(file);
        setSuccessMessage(`${file.name} uploaded successfully!`);
        onUploadSuccess?.(response);

        setTimeout(() => setSuccessMessage(null), 3000);
      }
    } catch (err: any) {
      onUploadError?.(err.message || 'Upload failed');
    }
  };

  const getDropzoneContent = () => {
    if (uploading) {
      return (
        <Box textAlign="center" py={4}>
          <UploadIcon sx={{ fontSize: 48, color: 'primary.main', mb: 2 }} />
          <Typography variant="h6" gutterBottom>
            Uploading...
          </Typography>
          <Typography variant="body2" color="textSecondary" gutterBottom>
            Please wait while your file is being uploaded
          </Typography>
          <LinearProgress 
            variant="determinate" 
            value={uploadProgress} 
            sx={{ mt: 2, mb: 1 }}
          />
          <Typography variant="caption" color="textSecondary">
            {uploadProgress}%
          </Typography>
        </Box>
      );
    }

    if (successMessage) {
      return (
        <Box textAlign="center" py={4}>
          <SuccessIcon sx={{ fontSize: 48, color: 'success.main', mb: 2 }} />
          <Typography variant="h6" gutterBottom color="success.main">
            Upload Successful!
          </Typography>
          <Typography variant="body2" color="textSecondary">
            {successMessage}
          </Typography>
        </Box>
      );
    }

    if (isDragReject) {
      return (
        <Box textAlign="center" py={4}>
          <ErrorIcon sx={{ fontSize: 48, color: 'error.main', mb: 2 }} />
          <Typography variant="h6" gutterBottom color="error.main">
            Invalid File Type
          </Typography>
          <Typography variant="body2" color="textSecondary">
            Please upload PDF, JPG, or PNG files only
          </Typography>
        </Box>
      );
    }

    return (
      <Box textAlign="center" py={4}>
        <UploadIcon 
          sx={{ 
            fontSize: 48, 
            color: isDragActive ? 'primary.main' : 'text.secondary', 
            mb: 2 
          }} 
        />
        <Typography 
          variant="h6" 
          gutterBottom 
          color={isDragActive ? 'primary.main' : 'textPrimary'}
        >
          {isDragActive ? 'Drop files here' : 'Upload Documents'}
        </Typography>
        <Typography variant="body2" color="textSecondary" gutterBottom>
          Drag and drop files here, or click to select
        </Typography>
        <Box mt={2}>
          <Chip label="PDF" size="small" sx={{ mr: 1 }} />
          <Chip label="JPG" size="small" sx={{ mr: 1 }} />
          <Chip label="PNG" size="small" />
        </Box>
        <Typography variant="caption" color="textSecondary" display="block" mt={1}>
          Maximum file size: 50MB
        </Typography>
      </Box>
    );
  };

  return (
    <Box>
      <Card 
        sx={{ 
          border: isDragActive ? 2 : 1,
          borderColor: isDragActive 
            ? 'primary.main' 
            : isDragReject 
              ? 'error.main' 
              : 'divider',
          borderStyle: isDragActive ? 'solid' : 'dashed',
          cursor: disabled || uploading ? 'not-allowed' : 'pointer',
          opacity: disabled ? 0.6 : 1,
          transition: 'all 0.2s ease-in-out',
        }}
      >
        <CardContent>
          <Box {...getRootProps()}>
            <input {...getInputProps()} />
            {getDropzoneContent()}
          </Box>
        </CardContent>
      </Card>

      {error && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
        </Alert>
      )}
    </Box>
  );
};

export default DocumentUpload;