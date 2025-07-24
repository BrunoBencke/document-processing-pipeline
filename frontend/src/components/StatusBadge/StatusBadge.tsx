import React from 'react';
import { Chip, CircularProgress, Box } from '@mui/material';
import {
  CloudUpload as UploadedIcon,
  Settings as ProcessingIcon,
  CheckCircle as ValidatedIcon,
  Error as FailedIcon,
} from '@mui/icons-material';
import { ProcessingStatus } from '../../types/document.types';

interface StatusBadgeProps {
  status: ProcessingStatus;
  size?: 'small' | 'medium';
  showProgress?: boolean;
  progress?: number | undefined;
}

const StatusBadge: React.FC<StatusBadgeProps> = ({ 
  status, 
  size = 'medium',
  showProgress = false,
  progress 
}) => {
  const getStatusConfig = (status: ProcessingStatus) => {
    switch (status) {
      case ProcessingStatus.UPLOADED:
        return {
          label: 'Uploaded',
          color: 'info' as const,
          icon: <UploadedIcon />,
        };
      case ProcessingStatus.PROCESSING:
        return {
          label: 'Processing',
          color: 'warning' as const,
          icon: showProgress ? (
            <Box position="relative" display="inline-flex">
              {progress !== undefined ? (
                <CircularProgress 
                  size={16} 
                  color="inherit"
                  variant="determinate"
                  value={progress}
                />
              ) : (
                <CircularProgress 
                  size={16} 
                  color="inherit"
                  variant="indeterminate"
                />
              )}
            </Box>
          ) : (
            <ProcessingIcon />
          ),
        };
      case ProcessingStatus.VALIDATED:
        return {
          label: 'Validated',
          color: 'success' as const,
          icon: <ValidatedIcon />,
        };
      case ProcessingStatus.FAILED:
        return {
          label: 'Failed',
          color: 'error' as const,
          icon: <FailedIcon />,
        };
      default:
        return {
          label: 'Unknown',
          color: 'default' as const,
          icon: null,
        };
    }
  };

  const config = getStatusConfig(status);

  const chipProps = {
    label: config.label,
    color: config.color,
    size,
    variant: "filled" as const,
    sx: {
      fontWeight: 'medium',
      ...(status === ProcessingStatus.PROCESSING && {
        animation: 'pulse 2s infinite',
        '@keyframes pulse': {
          '0%': {
            opacity: 1,
          },
          '50%': {
            opacity: 0.7,
          },
          '100%': {
            opacity: 1,
          },
        },
      }),
    },
  };

  return config.icon ? (
    <Chip {...chipProps} icon={config.icon} />
  ) : (
    <Chip {...chipProps} />
  );
};

export default StatusBadge;