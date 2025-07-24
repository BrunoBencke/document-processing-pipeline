import React, { useState } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  Paper,
  Tabs,
  Tab,
  Alert,
  Snackbar,
  Dialog,
  DialogTitle,
  DialogContent,
  IconButton,
  Backdrop,
  CircularProgress,
} from '@mui/material';
import {
  CloudUpload as UploadIcon,
  List as ListIcon,
  Assessment as StatsIcon,
  Close as CloseIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { useDocuments, useDocumentUpload } from '../hooks/useDocuments';
import { documentApi } from '../services/api';
import DocumentUpload from '../components/DocumentUpload/DocumentUpload';
import DocumentList from '../components/DocumentList/DocumentList';
import DocumentDetails from '../components/DocumentDetails/DocumentDetails';
import StatusBadge from '../components/StatusBadge/StatusBadge';
import { Document, ProcessingStatus } from '../types/document.types';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
    </div>
  );
}

const Dashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [selectedDocument, setSelectedDocument] = useState<Document | null>(null);
  const [detailsDialogOpen, setDetailsDialogOpen] = useState(false);
  const [snackbarOpen, setSnackbarOpen] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');
  const [snackbarSeverity, setSnackbarSeverity] = useState<'success' | 'error'>('success');
  const [statsLoading, setStatsLoading] = useState(false);

  const { 
    documents, 
    loading, 
    error, 
    refreshDocuments,
    totalElements,
  } = useDocuments();

  const { uploading } = useDocumentUpload();

  const loadStats = async () => {
    setStatsLoading(true);
    try {
      await documentApi.getDocumentStats();
    } catch (error) {
      console.error('Failed to load stats:', error);
    } finally {
      setStatsLoading(false);
    }
  };

  React.useEffect(() => {
    loadStats();
  }, []);

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  const handleDocumentSelect = (document: Document) => {
    setSelectedDocument(document);
    setDetailsDialogOpen(true);
  };

  const handleCloseDetails = () => {
    setDetailsDialogOpen(false);
    setSelectedDocument(null);
  };

  const handleUploadSuccess = (response: any) => {
    setSnackbarMessage(`Document "${response.filename}" uploaded successfully!`);
    setSnackbarSeverity('success');
    setSnackbarOpen(true);
    refreshDocuments();
    loadStats();
  };

  const handleUploadError = (error: string) => {
    setSnackbarMessage(error);
    setSnackbarSeverity('error');
    setSnackbarOpen(true);
  };

  const handleDocumentDeleted = () => {
    setDetailsDialogOpen(false);
    setSelectedDocument(null);
    setSnackbarMessage('Document deleted successfully');
    setSnackbarSeverity('success');
    setSnackbarOpen(true);
    refreshDocuments();
    loadStats();
  };

  const getStatusStats = () => {
    const statusCounts = documents.reduce((acc, doc) => {
      acc[doc.status] = (acc[doc.status] || 0) + 1;
      return acc;
    }, {} as Record<ProcessingStatus, number>);

    return [
      { 
        status: ProcessingStatus.UPLOADED, 
        count: statusCounts[ProcessingStatus.UPLOADED] || 0,
        label: 'Uploaded'
      },
      { 
        status: ProcessingStatus.PROCESSING, 
        count: statusCounts[ProcessingStatus.PROCESSING] || 0,
        label: 'Processing'
      },
      { 
        status: ProcessingStatus.VALIDATED, 
        count: statusCounts[ProcessingStatus.VALIDATED] || 0,
        label: 'Validated'
      },
      { 
        status: ProcessingStatus.FAILED, 
        count: statusCounts[ProcessingStatus.FAILED] || 0,
        label: 'Failed'
      },
    ];
  };

  const statusStats = getStatusStats();

  return (
    <Box>
      <Box mb={4}>
        <Typography variant="h4" gutterBottom>
          Document Processing Dashboard
        </Typography>
        <Typography variant="body1" color="textSecondary">
          Upload, process, and manage your documents with OCR and validation
        </Typography>
      </Box>

      <Grid container spacing={3} mb={4}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" justifyContent="space-between">
                <Box>
                  <Typography color="textSecondary" gutterBottom variant="h6">
                    Total Documents
                  </Typography>
                  <Typography variant="h4">
                    {totalElements}
                  </Typography>
                </Box>
                <ListIcon color="primary" sx={{ fontSize: 40 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {statusStats.map((stat) => (
          <Grid item xs={12} sm={6} md={3} key={stat.status}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center" justifyContent="space-between">
                  <Box>
                    <Typography color="textSecondary" gutterBottom variant="h6">
                      {stat.label}
                    </Typography>
                    <Typography variant="h4">
                      {stat.count}
                    </Typography>
                  </Box>
                  <StatusBadge status={stat.status} />
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Paper sx={{ width: '100%' }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={activeTab} onChange={handleTabChange} aria-label="dashboard tabs">
            <Tab icon={<UploadIcon />} label="Upload Documents" />
            <Tab icon={<ListIcon />} label="Document List" />
            <Tab icon={<StatsIcon />} label="Statistics" />
          </Tabs>
        </Box>

        <TabPanel value={activeTab} index={0}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={8}>
              <DocumentUpload
                onUploadSuccess={handleUploadSuccess}
                onUploadError={handleUploadError}
                disabled={uploading}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Upload Guidelines
                  </Typography>
                  <Typography variant="body2" paragraph>
                    • Supported formats: PDF, JPG, PNG
                  </Typography>
                  <Typography variant="body2" paragraph>
                    • Maximum file size: 50MB
                  </Typography>
                  <Typography variant="body2" paragraph>
                    • Documents will be automatically processed with OCR
                  </Typography>
                  <Typography variant="body2" paragraph>
                    • Processing typically takes 1-3 seconds
                  </Typography>
                  <Typography variant="body2">
                    • You can track progress in the Document List tab
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </TabPanel>

        <TabPanel value={activeTab} index={1}>
          {error ? (
            <Alert 
              severity="error" 
              action={
                <Button color="inherit" size="small" onClick={refreshDocuments}>
                  <RefreshIcon sx={{ mr: 1 }} />
                  Retry
                </Button>
              }
            >
              {error}
            </Alert>
          ) : (
            <DocumentList
              onDocumentSelect={handleDocumentSelect}
              showFilters={true}
              showActions={true}
            />
          )}
        </TabPanel>

        <TabPanel value={activeTab} index={2}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    Document Status Distribution
                  </Typography>
                  {statusStats.map((stat) => (
                    <Box
                      key={stat.status}
                      display="flex"
                      justifyContent="space-between"
                      alignItems="center"
                      py={1}
                    >
                      <Box display="flex" alignItems="center" gap={2}>
                        <StatusBadge status={stat.status} size="small" />
                        <Typography>{stat.label}</Typography>
                      </Box>
                      <Typography variant="h6">{stat.count}</Typography>
                    </Box>
                  ))}
                </CardContent>
              </Card>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Card>
                <CardContent>
                  <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                    <Typography variant="h6">
                      System Statistics
                    </Typography>
                    <Button
                      size="small"
                      onClick={loadStats}
                      disabled={statsLoading}
                      startIcon={<RefreshIcon />}
                    >
                      Refresh
                    </Button>
                  </Box>
                  
                  {statsLoading ? (
                    <Box display="flex" justifyContent="center" py={4}>
                      <CircularProgress />
                    </Box>
                  ) : (
                    <Box>
                      <Typography variant="body2" color="textSecondary" paragraph>
                        • OCR Engine: SimulatedOCR v2.1
                      </Typography>
                      <Typography variant="body2" color="textSecondary" paragraph>
                        • Average Processing Time: ~1.5s
                      </Typography>
                      <Typography variant="body2" color="textSecondary" paragraph>
                        • Supported Languages: Portuguese, English
                      </Typography>
                      <Typography variant="body2" color="textSecondary">
                        • Document Types: Invoices, Receipts, Contracts
                      </Typography>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </TabPanel>
      </Paper>

      <Dialog
        open={detailsDialogOpen}
        onClose={handleCloseDetails}
        maxWidth="lg"
        fullWidth
        PaperProps={{
          sx: { height: '90vh' },
        }}
      >
        <DialogTitle>
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Typography variant="h6">Document Details</Typography>
            <IconButton onClick={handleCloseDetails}>
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          {selectedDocument && (
            <DocumentDetails
              documentId={selectedDocument.id}
              onClose={handleCloseDetails}
              onDeleted={handleDocumentDeleted}
            />
          )}
        </DialogContent>
      </Dialog>

      <Backdrop open={loading || uploading} sx={{ zIndex: 1300 }}>
        <CircularProgress color="inherit" />
      </Backdrop>

      <Snackbar
        open={snackbarOpen}
        autoHideDuration={6000}
        onClose={() => setSnackbarOpen(false)}
      >
        <Alert
          onClose={() => setSnackbarOpen(false)}
          severity={snackbarSeverity}
          sx={{ width: '100%' }}
        >
          {snackbarMessage}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default Dashboard;