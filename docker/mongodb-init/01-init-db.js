db = db.getSiblingDB('docprocessor');

db.createUser({
  user: 'docapp',
  pwd: 'docapp123',
  roles: [
    {
      role: 'readWrite',
      db: 'docprocessor'
    }
  ]
});

db.createCollection('documents', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['filename', 'uploadedAt', 'status'],
      properties: {
        filename: {
          bsonType: 'string',
          description: 'must be a string and is required'
        },
        fileId: {
          bsonType: 'string',
          description: 'GridFS file reference'
        },
        uploadedAt: {
          bsonType: 'date',
          description: 'must be a date and is required'
        },
        processedAt: {
          bsonType: ['date', 'null'],
          description: 'processing completion date'
        },
        status: {
          enum: ['UPLOADED', 'PROCESSING', 'VALIDATED', 'FAILED'],
          description: 'must be one of the enum values and is required'
        },
        metadata: {
          bsonType: ['object', 'null'],
          description: 'extracted document metadata'
        },
        ocrResult: {
          bsonType: ['object', 'null'],
          description: 'OCR processing result'
        },
        errors: {
          bsonType: 'array',
          description: 'processing errors'
        },
        customerId: {
          bsonType: ['string', 'null'],
          description: 'customer identifier'
        }
      }
    }
  }
});

// Create indexes for better performance
db.documents.createIndex({ 'status': 1 });
db.documents.createIndex({ 'uploadedAt': -1 });
db.documents.createIndex({ 'customerId': 1 });
db.documents.createIndex({ 'metadata.invoiceNumber': 1 }, { sparse: true });
db.documents.createIndex({ 'metadata.customerName': 1 }, { sparse: true });
db.documents.createIndex({ 'processedAt': -1 });

// Create compound indexes
db.documents.createIndex({ 'status': 1, 'uploadedAt': -1 });
db.documents.createIndex({ 'customerId': 1, 'status': 1 });

print('Database and collections initialized successfully');