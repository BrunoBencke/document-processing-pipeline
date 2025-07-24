import React from 'react';
import { 
  AppBar, 
  Toolbar, 
  Typography, 
  Container, 
  Box, 
} from '@mui/material';
import { 
  Description as DocumentIcon,
} from '@mui/icons-material';
import { styled } from '@mui/material/styles';

const StyledContainer = styled(Container)(({ theme }) => ({
  marginTop: theme.spacing(3),
  marginBottom: theme.spacing(3),
  minHeight: 'calc(100vh - 100px)',
}));

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  return (
    <Box sx={{ flexGrow: 1, minHeight: '100vh', bgcolor: 'grey.50' }}>
      <AppBar position="static" elevation={1}>
        <Toolbar>
          <DocumentIcon sx={{ mr: 2 }} />
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Document Processing Pipeline
          </Typography>
        </Toolbar>
      </AppBar>
      
      <StyledContainer maxWidth="xl">
        {children}
      </StyledContainer>
      
      <Box 
        component="footer" 
        sx={{ 
          bgcolor: 'background.paper', 
          borderTop: 1, 
          borderColor: 'divider',
          py: 2,
          mt: 'auto',
        }}
      >
        <Container maxWidth="xl">
          <Typography variant="body2" color="textSecondary" align="center">
            Document Processing Pipeline - Built with React, TypeScript, Spring Boot & Material-UI
          </Typography>
        </Container>
      </Box>
    </Box>
  );
};

export default Layout;