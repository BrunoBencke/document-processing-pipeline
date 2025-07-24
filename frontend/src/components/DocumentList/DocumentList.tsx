import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  IconButton,
  Tooltip,
  Menu,
  MenuItem,
  TextField,
  InputAdornment,
  FormControl,
  InputLabel,
  Select,
  Button,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  LinearProgress,
} from '@mui/material';
import {
  Download as DownloadIcon,
  Delete as DeleteIcon,
  Visibility as ViewIcon,
  MoreVert as MoreIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { format } from 'date-fns';
import StatusBadge from '../StatusBadge/StatusBadge';
import { useDocuments, useDocumentOperations } from '../../hooks/useDocuments';
import { Document, ProcessingStatus } from '../../types/document.types';
import { fileUtils } from '../../services/api';

interface DocumentListProps {
  onDocumentSelect?: (document: Document) => void;
  showFilters?: boolean;
  showActions?: boolean;
  compact?: boolean;
}

const DocumentList: React.FC<DocumentListProps> = ({
  onDocumentSelect,
  showFilters = true,
  showActions = true,
  compact = false,
}) => {
  const {
    documents,
    loading,
    error,
    totalElements,
    currentPage,
    filters,
    setFilters,
    refreshDocuments,
    removeDocument,
  } = useDocuments({});

  const { deleteDocument, downloadDocument, loading: operationLoading } = useDocumentOperations();

  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<ProcessingStatus | ''>('');
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedDocument, setSelectedDocument] = useState<Document | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

  const handleSearch = (value: string) => {
    setSearchTerm(value);
    setFilters({ ...filters, searchTerm: value, page: 0 });
  };

  const handleStatusFilter = (status: ProcessingStatus | '') => {
    setStatusFilter(status);
    const newFilters = { ...filters, page: 0 };
    if (status) {
      newFilters.status = status;
    } else {
      delete newFilters.status;
    }
    setFilters(newFilters);
  };

  const handlePageChange = (_event: unknown, newPage: number) => {
    setFilters({ ...filters, page: newPage });
  };

  const handleRowsPerPageChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setFilters({ 
      ...filters, 
      size: parseInt(event.target.value, 10), 
      page: 0 
    });
  };

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, document: Document) => {
    setAnchorEl(event.currentTarget);
    setSelectedDocument(document);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedDocument(null);
  };

  const handleView = (document: Document) => {
    onDocumentSelect?.(document);
    handleMenuClose();
  };

  const handleDownload = async (document: Document) => {
    try {
      await downloadDocument(document.id, document.filename);
    } catch (error) {
      console.error('Download failed:', error);
    }
    handleMenuClose();
  };

  const handleDeleteClick = (document: Document) => {
    setSelectedDocument(document);
    setDeleteDialogOpen(true);
    handleMenuClose();
  };

  const handleDeleteConfirm = async () => {
    if (!selectedDocument) return;

    try {
      await deleteDocument(selectedDocument.id);
      removeDocument(selectedDocument.id);
    } catch (error) {
      console.error('Delete failed:', error);
    } finally {
      setDeleteDialogOpen(false);
      setSelectedDocument(null);
    }
  };

  const formatFileSize = (bytes?: number) => {
    return bytes ? fileUtils.formatFileSize(bytes) : 'Unknown';
  };

  const formatDate = (dateString: string) => {
    try {
      return format(new Date(dateString), 'MMM dd, yyyy HH:mm');
    } catch {
      return 'Invalid date';
    }
  };

  if (error) {
    return (
      <Alert severity="error" action={
        <Button color="inherit" size="small" onClick={refreshDocuments}>
          Retry
        </Button>
      }>
        {error}
      </Alert>
    );
  }

  return (
    <Box>
      {showFilters && (
        <Card sx={{ mb: 2 }}>
          <CardContent>
            <Box display="flex" gap={2} alignItems="center" flexWrap="wrap">
              <TextField
                placeholder="Search documents..."
                value={searchTerm}
                onChange={(e) => handleSearch(e.target.value)}
                size="small"
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon />
                    </InputAdornment>
                  ),
                }}
                sx={{ minWidth: 200 }}
              />
              
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>Status</InputLabel>
                <Select
                  value={statusFilter}
                  label="Status"
                  onChange={(e) => handleStatusFilter(e.target.value as ProcessingStatus | '')}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value={ProcessingStatus.UPLOADED}>Uploaded</MenuItem>
                  <MenuItem value={ProcessingStatus.PROCESSING}>Processing</MenuItem>
                  <MenuItem value={ProcessingStatus.VALIDATED}>Validated</MenuItem>
                  <MenuItem value={ProcessingStatus.FAILED}>Failed</MenuItem>
                </Select>
              </FormControl>

              <Button
                startIcon={<RefreshIcon />}
                onClick={refreshDocuments}
                disabled={loading}
                size="small"
              >
                Refresh
              </Button>
            </Box>
          </CardContent>
        </Card>
      )}

      {loading && <LinearProgress sx={{ mb: 2 }} />}

      <Card>
        <CardContent sx={{ p: 0 }}>
          <TableContainer>
            <Table size={compact ? 'small' : 'medium'}>
              <TableHead>
                <TableRow>
                  <TableCell>Document</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Uploaded</TableCell>
                  {!compact && <TableCell>Size</TableCell>}
                  {showActions && <TableCell align="right">Actions</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {documents.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={compact ? 4 : 5} align="center" sx={{ py: 4 }}>
                      <Typography color="textSecondary">
                        {loading ? 'Loading documents...' : 'No documents found'}
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  documents.map((document) => (
                    <TableRow 
                      key={document.id}
                      hover
                      sx={{ 
                        cursor: onDocumentSelect ? 'pointer' : 'default',
                        '&:hover': {
                          backgroundColor: 'action.hover',
                        },
                      }}
                      onClick={() => onDocumentSelect?.(document)}
                    >
                      <TableCell>
                        <Box>
                          <Typography variant="body2" fontWeight="medium">
                            {document.filename}
                          </Typography>
                          <Typography variant="caption" color="textSecondary">
                            {document.id}
                          </Typography>
                        </Box>
                      </TableCell>
                      
                      <TableCell>
                        {document.status === ProcessingStatus.PROCESSING && document.processingProgress !== undefined ? (
                          <StatusBadge 
                            status={document.status}
                            size="small"
                            showProgress={true}
                            progress={document.processingProgress}
                          />
                        ) : (
                          <StatusBadge 
                            status={document.status}
                            size="small"
                            showProgress={document.status === ProcessingStatus.PROCESSING}
                          />
                        )}
                      </TableCell>
                      
                      <TableCell>
                        <Typography variant="body2">
                          {formatDate(document.uploadedAt)}
                        </Typography>
                      </TableCell>
                      
                      {!compact && (
                        <TableCell>
                          <Typography variant="body2">
                            {formatFileSize(document.fileSizeBytes)}
                          </Typography>
                        </TableCell>
                      )}
                      
                      {showActions && (
                        <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                          <Box display="flex" gap={1} justifyContent="flex-end">
                            <Tooltip title="View details">
                              <IconButton 
                                size="small" 
                                onClick={() => handleView(document)}
                              >
                                <ViewIcon />
                              </IconButton>
                            </Tooltip>
                            
                            <Tooltip title="Download">
                              <IconButton 
                                size="small" 
                                onClick={() => handleDownload(document)}
                                disabled={operationLoading}
                              >
                                <DownloadIcon />
                              </IconButton>
                            </Tooltip>
                            
                            <Tooltip title="More actions">
                              <IconButton 
                                size="small"
                                onClick={(e) => handleMenuOpen(e, document)}
                              >
                                <MoreIcon />
                              </IconButton>
                            </Tooltip>
                          </Box>
                        </TableCell>
                      )}
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>

          {totalElements > 0 && (
            <TablePagination
              component="div"
              count={totalElements}
              page={currentPage}
              onPageChange={handlePageChange}
              rowsPerPage={filters.size || 20}
              onRowsPerPageChange={handleRowsPerPageChange}
              rowsPerPageOptions={[10, 20, 50, 100]}
            />
          )}
        </CardContent>
      </Card>

      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={() => selectedDocument && handleView(selectedDocument)}>
          <ViewIcon sx={{ mr: 1 }} />
          View Details
        </MenuItem>
        <MenuItem onClick={() => selectedDocument && handleDownload(selectedDocument)}>
          <DownloadIcon sx={{ mr: 1 }} />
          Download
        </MenuItem>
        <MenuItem 
          onClick={() => selectedDocument && handleDeleteClick(selectedDocument)}
          sx={{ color: 'error.main' }}
        >
          <DeleteIcon sx={{ mr: 1 }} />
          Delete
        </MenuItem>
      </Menu>

      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>Delete Document</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete "{selectedDocument?.filename}"? 
            This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>
            Cancel
          </Button>
          <Button 
            onClick={handleDeleteConfirm}
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

export default DocumentList;