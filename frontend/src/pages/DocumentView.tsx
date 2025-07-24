import React from 'react';
import { useParams } from 'react-router-dom';
import { Typography, Paper, Box } from '@mui/material';

const DocumentView: React.FC = () => {
  const { documentId } = useParams<{ documentId: string }>();

  return (
    <Box>
      <Typography variant="h4" component="h1" gutterBottom>
        Document Details
      </Typography>
      <Paper elevation={3} sx={{ p: 3, mt: 2 }}>
        <Typography variant="h6" gutterBottom>
          Document ID: {documentId}
        </Typography>
        <Typography variant="body1">
          Document details will be displayed here.
        </Typography>
      </Paper>
    </Box>
  );
};

export default DocumentView;