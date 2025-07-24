import { WebSocketMessage, ProcessingStatus } from '@/types/document.types';

export class WebSocketService {
  private ws: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;
  private listeners: Map<string, Set<(message: WebSocketMessage) => void>> = new Map();
  private globalListeners: Set<(message: WebSocketMessage) => void> = new Set();
  private isConnecting = false;

  constructor(private baseUrl: string = process.env.REACT_APP_WS_URL || 'ws://localhost:8080/ws') { }

  connect(): Promise<void> {
    if (this.ws?.readyState === WebSocket.OPEN || this.isConnecting) {
      return Promise.resolve();
    }

    this.isConnecting = true;

    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(`${this.baseUrl}/documents`);

        this.ws.onopen = () => {
          console.log('WebSocket connected');
          this.isConnecting = false;
          this.reconnectAttempts = 0;
          resolve();
        };

        this.ws.onmessage = (event) => {
          try {
            const message: WebSocketMessage = JSON.parse(event.data);
            this.handleMessage(message);
          } catch (error) {
            console.error('Error parsing WebSocket message:', error);
          }
        };

        this.ws.onclose = () => {
          console.log('WebSocket disconnected');
          this.isConnecting = false;
          this.handleReconnect();
        };

        this.ws.onerror = (error) => {
          console.error('WebSocket error:', error);
          this.isConnecting = false;
          if (this.reconnectAttempts === 0) {
            reject(error);
          }
        };
      } catch (error) {
        this.isConnecting = false;
        reject(error);
      }
    });
  }

  disconnect(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.listeners.clear();
    this.globalListeners.clear();
  }

  subscribeToDocument(documentId: string, callback: (message: WebSocketMessage) => void): () => void {
    if (!this.listeners.has(documentId)) {
      this.listeners.set(documentId, new Set());
    }

    this.listeners.get(documentId)!.add(callback);

    if (this.ws?.readyState !== WebSocket.OPEN) {
      this.connect().catch(console.error);
    }

    return () => {
      const documentListeners = this.listeners.get(documentId);
      if (documentListeners) {
        documentListeners.delete(callback);
        if (documentListeners.size === 0) {
          this.listeners.delete(documentId);
        }
      }
    };
  }

  subscribeToAll(callback: (message: WebSocketMessage) => void): () => void {
    this.globalListeners.add(callback);

    if (this.ws?.readyState !== WebSocket.OPEN) {
      this.connect().catch(console.error);
    }

    return () => {
      this.globalListeners.delete(callback);
    };
  }

  send(message: any): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.warn('WebSocket is not connected. Message not sent:', message);
    }
  }

  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }

  private handleMessage(message: WebSocketMessage): void {
    const documentListeners = this.listeners.get(message.documentId);
    if (documentListeners) {
      documentListeners.forEach(callback => {
        try {
          callback(message);
        } catch (error) {
          console.error('Error in document listener callback:', error);
        }
      });
    }

    this.globalListeners.forEach(callback => {
      try {
        callback(message);
      } catch (error) {
        console.error('Error in global listener callback:', error);
      }
    });
  }

  private handleReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);

      console.log(`Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts})`);

      setTimeout(() => {
        this.connect().catch(error => {
          console.error('Reconnection failed:', error);
        });
      }, delay);
    } else {
      console.error('Max reconnection attempts reached. Please refresh the page.');
    }
  }
}

export const webSocketService = new WebSocketService();

export const useWebSocket = () => {
  return {
    connect: () => webSocketService.connect(),
    disconnect: () => webSocketService.disconnect(),
    subscribeToDocument: (documentId: string, callback: (message: WebSocketMessage) => void) =>
      webSocketService.subscribeToDocument(documentId, callback),
    subscribeToAll: (callback: (message: WebSocketMessage) => void) =>
      webSocketService.subscribeToAll(callback),
    isConnected: () => webSocketService.isConnected(),
    send: (message: any) => webSocketService.send(message),
  };
};

export const webSocketUtils = {
  createStatusUpdate: (documentId: string, status: ProcessingStatus): WebSocketMessage => ({
    type: 'STATUS_UPDATE',
    documentId,
    status,
    timestamp: new Date().toISOString(),
  }),

  createProgressUpdate: (documentId: string, progress: number): WebSocketMessage => ({
    type: 'PROGRESS_UPDATE',
    documentId,
    progress,
    timestamp: new Date().toISOString(),
  }),

  createError: (documentId: string, message: string): WebSocketMessage => ({
    type: 'ERROR',
    documentId,
    message,
    timestamp: new Date().toISOString(),
  }),

  createCompleted: (documentId: string, status: ProcessingStatus): WebSocketMessage => ({
    type: 'COMPLETED',
    documentId,
    status,
    timestamp: new Date().toISOString(),
  }),
};

export default webSocketService;