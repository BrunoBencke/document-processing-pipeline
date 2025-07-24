# Document Processing Pipeline

A modern document processing system that automates extraction, validation, and persistence of information from invoices, receipts, and contracts using microservices architecture with asynchronous processing and real-time notifications.

## Tech Stack

### Frontend
- **React 18** with **TypeScript**
- **Material-UI** components
- **React Query** for server state management
- **WebSocket** for real-time updates
- **React Dropzone** for file uploads

### Backend
- **Spring Boot 3.5.3** with **Java 21**
- **Spring Data MongoDB**
- **Spring AMQP** (RabbitMQ integration)
- **Spring WebSocket**

### Infrastructure
- **MongoDB** - Document storage with GridFS for files
- **RabbitMQ** - Message broker for async processing

## Architecture

### Processing Flow
1. **Upload**: User uploads document → Backend stores in MongoDB/GridFS → Message sent to RabbitMQ
2. **Processing**: RabbitMQ consumer → OCR simulation → Metadata extraction → Validation
3. **Real-time Updates**: Status changes broadcasted via WebSocket to connected clients

### Data Model
```javascript
{
  "_id": "507f1f77bcf86cd799439011",
  "filename": "invoice_001.pdf",
  "fileId": "507f1f77bcf86cd799439012", // GridFS reference
  "status": "VALIDATED", // UPLOADED, PROCESSING, VALIDATED, FAILED
  "metadata": {
    "invoiceNumber": "INV-2024-001",
    "invoiceDate": "2024-01-15",
    "totalAmount": 1500.00,
    "items": [{
      "description": "Product A",
      "quantity": 2,
      "unitPrice": 750.00
    }]
  },
  "ocrResult": {
    "text": "Invoice content...",
    "confidence": 0.98
  }
}
```

## API Endpoints

```
POST   /api/documents/upload          # Upload documents
GET    /api/documents                 # List documents (paginated)
GET    /api/documents/{id}            # Get document by ID
GET    /api/documents/{id}/download   # Download original file
PUT    /api/documents/{id}/status     # Update document status
DELETE /api/documents/{id}            # Delete document
GET    /api/documents/status/{status} # Filter by status
```

## Quick Start

### Prerequisites
- Java 21
- Node.js 18+
- Docker & Docker Compose

### Local Development
```bash
# Clone repository
git clone <repository-url>
cd document-processing-pipeline

# Start infrastructure
cd docker
docker-compose -f docker-compose.dev.yml up -d

# Run backend
cd ../backend
./mvnw spring-boot:run

# Run frontend
cd ../frontend
npm install
npm start
```

### Access URLs
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- MongoDB: mongodb://localhost:27017
- RabbitMQ Management: http://localhost:15672 (admin/admin123)

## Testing

### Backend
```bash
./mvnw test              # Run all tests
./mvnw jacoco:report     # Generate coverage report
```

### Frontend
```bash
npm test                 # Run tests
npm run test:coverage    # Generate coverage report
```

## Docker Deployment
```bash
docker-compose up --build -d    # Build and run all services
docker-compose logs -f          # View logs
```
