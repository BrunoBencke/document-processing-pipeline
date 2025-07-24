import React from 'react';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import App from './App';

const createTestWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  const theme = createTheme();

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <BrowserRouter>
          {children}
        </BrowserRouter>
      </ThemeProvider>
    </QueryClientProvider>
  );
};

jest.mock('./pages/Dashboard', () => {
  return function Dashboard() {
    return <div data-testid="dashboard">Dashboard</div>;
  };
});

jest.mock('./pages/DocumentView', () => {
  return function DocumentView() {
    return <div data-testid="document-view">Document View</div>;
  };
});

jest.mock('./components/Layout/Layout', () => {
  return function Layout({ children }: { children: React.ReactNode }) {
    return <div data-testid="layout">{children}</div>;
  };
});

describe('App', () => {
  test('renders without crashing', () => {
    const TestWrapper = createTestWrapper();
    
    render(
      <TestWrapper>
        <App />
      </TestWrapper>
    );

    expect(screen.getByTestId('layout')).toBeInTheDocument();
  });

  test('renders dashboard on root route', () => {
    const TestWrapper = createTestWrapper();
    Object.defineProperty(window, 'location', {
      value: {
        pathname: '/'
      },
      writable: true
    });

    render(
      <TestWrapper>
        <App />
      </TestWrapper>
    );

    expect(screen.getByTestId('dashboard')).toBeInTheDocument();
  });
});