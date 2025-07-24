import { ProcessingStatus } from './document.types';

describe('ProcessingStatus', () => {
  test('should have all expected status values', () => {
    expect(ProcessingStatus.UPLOADED).toBe('UPLOADED');
    expect(ProcessingStatus.PROCESSING).toBe('PROCESSING');
    expect(ProcessingStatus.VALIDATED).toBe('VALIDATED');
    expect(ProcessingStatus.FAILED).toBe('FAILED');
  });

  test('should have 4 status values total', () => {
    const statusValues = Object.values(ProcessingStatus);
    expect(statusValues).toHaveLength(4);
  });

  test('should contain all expected status strings', () => {
    const statusValues = Object.values(ProcessingStatus);
    expect(statusValues).toContain('UPLOADED');
    expect(statusValues).toContain('PROCESSING');
    expect(statusValues).toContain('VALIDATED');
    expect(statusValues).toContain('FAILED');
  });
});