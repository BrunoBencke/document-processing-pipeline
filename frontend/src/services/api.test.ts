import { fileUtils } from './api';

describe('fileUtils', () => {
  describe('formatFileSize', () => {
    test('should format bytes correctly', () => {
      expect(fileUtils.formatFileSize(0)).toBe('0 Bytes');
      expect(fileUtils.formatFileSize(1024)).toBe('1 KB');
      expect(fileUtils.formatFileSize(1536)).toBe('1.5 KB');
      expect(fileUtils.formatFileSize(1048576)).toBe('1 MB');
      expect(fileUtils.formatFileSize(1073741824)).toBe('1 GB');
    });

    test('should handle small byte values', () => {
      expect(fileUtils.formatFileSize(500)).toBe('500 Bytes');
      expect(fileUtils.formatFileSize(1000)).toBe('1000 Bytes');
    });
  });

  describe('isValidFileType', () => {
    test('should accept valid file types', () => {
      const pdfFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      const jpegFile = new File(['content'], 'test.jpg', { type: 'image/jpeg' });
      const pngFile = new File(['content'], 'test.png', { type: 'image/png' });

      expect(fileUtils.isValidFileType(pdfFile)).toBe(true);
      expect(fileUtils.isValidFileType(jpegFile)).toBe(true);
      expect(fileUtils.isValidFileType(pngFile)).toBe(true);
    });

    test('should reject invalid file types', () => {
      const txtFile = new File(['content'], 'test.txt', { type: 'text/plain' });
      const docFile = new File(['content'], 'test.doc', { type: 'application/msword' });

      expect(fileUtils.isValidFileType(txtFile)).toBe(false);
      expect(fileUtils.isValidFileType(docFile)).toBe(false);
    });
  });

  describe('isValidFileSize', () => {
    test('should accept files within size limit', () => {
      const smallFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      Object.defineProperty(smallFile, 'size', {
        value: 1024 * 1024, // 1MB
        writable: false
      });

      expect(fileUtils.isValidFileSize(smallFile, 50)).toBe(true);
    });

    test('should reject files exceeding size limit', () => {
      const largeFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      Object.defineProperty(largeFile, 'size', {
        value: 100 * 1024 * 1024, // 100MB
        writable: false
      });

      expect(fileUtils.isValidFileSize(largeFile, 50)).toBe(false);
    });

    test('should use default 50MB limit', () => {
      const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
      Object.defineProperty(file, 'size', {
        value: 40 * 1024 * 1024, // 40MB
        writable: false
      });

      expect(fileUtils.isValidFileSize(file)).toBe(true);
    });
  });

  describe('getFileIcon', () => {
    test('should return correct icons for file types', () => {
      expect(fileUtils.getFileIcon('application/pdf')).toBe('📄');
      expect(fileUtils.getFileIcon('image/jpeg')).toBe('🖼️');
      expect(fileUtils.getFileIcon('image/png')).toBe('🖼️');
    });

    test('should return default icon for unknown types', () => {
      expect(fileUtils.getFileIcon('text/plain')).toBe('📄');
      expect(fileUtils.getFileIcon(undefined)).toBe('📄');
      expect(fileUtils.getFileIcon('')).toBe('📄');
    });
  });

  describe('downloadBlob', () => {
    test('should create download link and trigger download', () => {
      const mockUrl = 'mock-blob-url';
      global.URL.createObjectURL = jest.fn(() => mockUrl);
      global.URL.revokeObjectURL = jest.fn();

      const mockLink = {
        href: '',
        download: '',
        click: jest.fn(),
      };
      const mockAppendChild = jest.fn();
      const mockRemoveChild = jest.fn();

      jest.spyOn(document, 'createElement').mockReturnValue(mockLink as any);
      jest.spyOn(document.body, 'appendChild').mockImplementation(mockAppendChild);
      jest.spyOn(document.body, 'removeChild').mockImplementation(mockRemoveChild);

      const blob = new Blob(['test content'], { type: 'text/plain' });
      const filename = 'test-file.txt';

      fileUtils.downloadBlob(blob, filename);

      expect(global.URL.createObjectURL).toHaveBeenCalledWith(blob);
      expect(mockLink.href).toBe(mockUrl);
      expect(mockLink.download).toBe(filename);
      expect(mockAppendChild).toHaveBeenCalledWith(mockLink);
      expect(mockLink.click).toHaveBeenCalled();
      expect(mockRemoveChild).toHaveBeenCalledWith(mockLink);
      expect(global.URL.revokeObjectURL).toHaveBeenCalledWith(mockUrl);

      jest.restoreAllMocks();
    });
  });
});