import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Button,
  List,
  ListItem,
  ListItemText,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Alert,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  LinearProgress,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  ExpandMore as ExpandMoreIcon,
  Download as DownloadIcon,
  Refresh as RefreshIcon,
  Description as FileIcon,
  Assessment as MetricsIcon,
  Error as ErrorIcon,
  CheckCircle as SuccessIcon,
  Warning as WarningIcon,
} from '@mui/icons-material';
import { format } from 'date-fns';
import StatusBadge from '../StatusBadge/StatusBadge';
import { useDocument, useDocumentOperations } from '../../hooks/useDocuments';
import { ProcessingStatus } from '../../types/document.types';
import { fileUtils } from '../../services/api';

interface DocumentDetailsProps {
  documentId: string;
  onClose?: () => void;
  onDeleted?: () => void;
}

const DocumentDetails: React.FC<DocumentDetailsProps> = ({
  documentId,
  onClose,
  onDeleted,
}) => {
  const { document, loading, error, refetch } = useDocument(documentId);
  const { 
    deleteDocument, 
    downloadDocument, 
    updateDocumentStatus,
    loading: operationLoading 
  } = useDocumentOperations();

  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [expandedSections, setExpandedSections] = useState<string[]>(['overview']);

  const handleSectionToggle = (section: string) => {
    setExpandedSections(prev => 
      prev.includes(section) 
        ? prev.filter(s => s !== section)
        : [...prev, section]
    );
  };

  const handleDownload = async () => {
    if (!document) return;
    try {
      await downloadDocument(document.id, document.filename);
    } catch (error) {
      console.error('Download failed:', error);
    }
  };

  const handleDelete = async () => {
    if (!document) return;
    try {
      await deleteDocument(document.id);
      setDeleteDialogOpen(false);
      onDeleted?.();
    } catch (error) {
      console.error('Delete failed:', error);
    }
  };

  const handleRetry = async () => {
    if (!document) return;
    try {
      await updateDocumentStatus(document.id, ProcessingStatus.UPLOADED);
      refetch();
    } catch (error) {
      console.error('Retry failed:', error);
    }
  };

  const formatDate = (dateString: string) => {
    try {
      return format(new Date(dateString), 'PPP pp');
    } catch {
      return 'Invalid date';
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(amount);
  };

  if (loading) {
    return (
      <Box p={3}>
        <LinearProgress sx={{ mb: 2 }} />
        <Typography>Loading document details...</Typography>
      </Box>
    );
  }

  if (error || !document) {
    return (
      <Box p={3}>
        <Alert 
          severity="error" 
          action={
            <Button color="inherit" size="small" onClick={refetch}>
              Retry
            </Button>
          }
        >
          {error || 'Document not found'}
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h5" gutterBottom>
            Document Details
          </Typography>
          <Typography variant="body2" color="textSecondary">
            {document.filename}
          </Typography>
        </Box>
        
        <Box display="flex" gap={1}>
          <Tooltip title="Refresh">
            <IconButton onClick={refetch} disabled={loading}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
          
          <Tooltip title="Download">
            <IconButton onClick={handleDownload} disabled={operationLoading}>
              <DownloadIcon />
            </IconButton>
          </Tooltip>
          
          {document.status === ProcessingStatus.FAILED && (
            <Button
              variant="outlined"
              size="small"
              onClick={handleRetry}
              disabled={operationLoading}
            >
              Retry
            </Button>
          )}
          
          <Button
            variant="outlined"
            color="error"
            size="small"
            onClick={() => setDeleteDialogOpen(true)}
            disabled={operationLoading}
          >
            Delete
          </Button>
          
          {onClose && (
            <Button variant="outlined" size="small" onClick={onClose}>
              Close
            </Button>
          )}
        </Box>
      </Box>

      <Accordion 
        expanded={expandedSections.includes('overview')}
        onChange={() => handleSectionToggle('overview')}
      >
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <FileIcon sx={{ mr: 1 }} />
          <Typography variant="h6">Overview</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="subtitle2" gutterBottom>
                    Document Information
                  </Typography>
                  <List dense>
                    <ListItem>
                      <ListItemText 
                        primary="Filename" 
                        secondary={document.filename} 
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemText 
                        primary="Status" 
                        secondary={<StatusBadge status={document.status} size="small" />} 
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemText 
                        primary="File Size" 
                        secondary={fileUtils.formatFileSize(document.fileSizeBytes || 0)} 
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemText 
                        primary="Content Type" 
                        secondary={document.contentType || 'Unknown'} 
                      />
                    </ListItem>
                  </List>
                </CardContent>
              </Card>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="subtitle2" gutterBottom>
                    Processing Information
                  </Typography>
                  <List dense>
                    <ListItem>
                      <ListItemText 
                        primary="Uploaded At" 
                        secondary={formatDate(document.uploadedAt)} 
                      />
                    </ListItem>
                    {document.processedAt && (
                      <ListItem>
                        <ListItemText 
                          primary="Processed At" 
                          secondary={formatDate(document.processedAt)} 
                        />
                      </ListItem>
                    )}
                  </List>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </AccordionDetails>
      </Accordion>

      {document.ocrResult && (
        <Accordion 
          expanded={expandedSections.includes('ocr')}
          onChange={() => handleSectionToggle('ocr')}
        >
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <MetricsIcon sx={{ mr: 1 }} />
            <Typography variant="h6">OCR Results</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="subtitle2" gutterBottom>
                      OCR Metrics
                    </Typography>
                    <List dense>
                      <ListItem>
                        <ListItemText 
                          primary="Confidence" 
                          secondary={
                            <Box display="flex" alignItems="center" gap={1}>
                              <Typography variant="body2">
                                {(document.ocrResult.confidence * 100).toFixed(1)}%
                              </Typography>
                              {document.ocrResult.confidence >= 0.8 ? (
                                <SuccessIcon color="success" fontSize="small" />
                              ) : document.ocrResult.confidence >= 0.6 ? (
                                <WarningIcon color="warning" fontSize="small" />
                              ) : (
                                <ErrorIcon color="error" fontSize="small" />
                              )}
                            </Box>
                          } 
                        />
                      </ListItem>
                      <ListItem>
                        <ListItemText 
                          primary="Language" 
                          secondary={document.ocrResult.language} 
                        />
                      </ListItem>
                      {document.ocrResult.processingTimeMs && (
                        <ListItem>
                          <ListItemText 
                            primary="Processing Time" 
                            secondary={`${document.ocrResult.processingTimeMs}ms`} 
                          />
                        </ListItem>
                      )}
                      {document.ocrResult.processingEngine && (
                        <ListItem>
                          <ListItemText 
                            primary="Engine" 
                            secondary={document.ocrResult.processingEngine} 
                          />
                        </ListItem>
                      )}
                    </List>
                  </CardContent>
                </Card>
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="subtitle2" gutterBottom>
                      Extracted Text
                    </Typography>
                    <Typography 
                      variant="body2" 
                      sx={{ 
                        whiteSpace: 'pre-wrap',
                        maxHeight: 200,
                        overflow: 'auto',
                        bgcolor: 'grey.50',
                        p: 1,
                        borderRadius: 1,
                      }}
                    >
                      {document.ocrResult.text}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
          </AccordionDetails>
        </Accordion>
      )}

      {document.metadata && (
        <Accordion 
          expanded={expandedSections.includes('metadata')}
          onChange={() => handleSectionToggle('metadata')}
        >
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <FileIcon sx={{ mr: 1 }} />
            <Typography variant="h6">Document Metadata</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="subtitle2" gutterBottom>
                      Invoice Information
                    </Typography>
                    <List dense>
                      <ListItem>
                        <ListItemText 
                          primary="Invoice Number" 
                          secondary={document.metadata.invoiceNumber} 
                        />
                      </ListItem>
                      <ListItem>
                        <ListItemText 
                          primary="Invoice Date" 
                          secondary={document.metadata.invoiceDate} 
                        />
                      </ListItem>
                      <ListItem>
                        <ListItemText 
                          primary="Total Amount" 
                          secondary={formatCurrency(document.metadata.totalAmount)} 
                        />
                      </ListItem>
                    </List>
                  </CardContent>
                </Card>
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="subtitle2" gutterBottom>
                      Line Items ({document.metadata.items.length})
                    </Typography>
                    <TableContainer sx={{ maxHeight: 300 }}>
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Description</TableCell>
                            <TableCell align="right">Qty</TableCell>
                            <TableCell align="right">Unit Price</TableCell>
                            <TableCell align="right">Total</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {document.metadata.items.map((item, index) => (
                            <TableRow key={index}>
                              <TableCell>{item.description}</TableCell>
                              <TableCell align="right">{item.quantity}</TableCell>
                              <TableCell align="right">
                                {formatCurrency(item.unitPrice)}
                              </TableCell>
                              <TableCell align="right">
                                {formatCurrency(item.total)}
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
          </AccordionDetails>
        </Accordion>
      )}

      {document.errors && document.errors.length > 0 && (
        <Accordion 
          expanded={expandedSections.includes('errors')}
          onChange={() => handleSectionToggle('errors')}
        >
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <ErrorIcon sx={{ mr: 1 }} color="error" />
            <Typography variant="h6">Processing Errors</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Alert severity="error" sx={{ mb: 2 }}>
              This document encountered errors during processing
            </Alert>
            <List>
              {document.errors.map((error, index) => (
                <ListItem key={index}>
                  <ErrorIcon color="error" sx={{ mr: 1 }} />
                  <ListItemText primary={error} />
                </ListItem>
              ))}
            </List>
          </AccordionDetails>
        </Accordion>
      )}

      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>Delete Document</DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to delete "{document.filename}"? 
            This action cannot be undone.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>
            Cancel
          </Button>
          <Button 
            onClick={handleDelete}
            color="error"
            disabled={operationLoading}
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DocumentDetails;