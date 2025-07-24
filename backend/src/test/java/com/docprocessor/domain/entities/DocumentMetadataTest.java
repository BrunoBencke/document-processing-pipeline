package com.docprocessor.domain.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DocumentMetadataTest {

    private DocumentMetadata metadata;

    @BeforeEach
    void setUp() {
        metadata = new DocumentMetadata(
            "ACME Corp",
            "INV-2024-001",
            LocalDate.of(2024, 1, 15),
            new BigDecimal("1500.00")
        );
    }

    @Test
    void testMetadataCreation() {
        assertEquals("ACME Corp", metadata.getCustomerName());
        assertEquals("INV-2024-001", metadata.getInvoiceNumber());
        assertEquals(LocalDate.of(2024, 1, 15), metadata.getInvoiceDate());
        assertEquals(new BigDecimal("1500.00"), metadata.getTotalAmount());
        assertNotNull(metadata.getItems());
        assertNotNull(metadata.getAdditionalFields());
    }

    @Test
    void testAddItem() {
        DocumentMetadata.InvoiceItem item = new DocumentMetadata.InvoiceItem(
            "Product A", 
            new BigDecimal("2"), 
            new BigDecimal("750.00"),
            new BigDecimal("1500.00")
        );
        
        metadata.addItem(item);
        
        assertEquals(1, metadata.getItemCount());
        assertEquals(item, metadata.getItems().get(0));
    }

    @Test
    void testCalculateItemsTotal() {
        DocumentMetadata.InvoiceItem item1 = new DocumentMetadata.InvoiceItem(
            "Product A", 
            new BigDecimal("1"), 
            new BigDecimal("500.00"),
            new BigDecimal("500.00")
        );
        
        DocumentMetadata.InvoiceItem item2 = new DocumentMetadata.InvoiceItem(
            "Product B", 
            new BigDecimal("2"), 
            new BigDecimal("500.00"),
            new BigDecimal("1000.00")
        );
        
        metadata.addItem(item1);
        metadata.addItem(item2);
        
        BigDecimal total = metadata.calculateItemsTotal();
        assertEquals(new BigDecimal("1500.00"), total);
    }

    @Test
    void testIsTotalAmountConsistent() {
        DocumentMetadata.InvoiceItem item = new DocumentMetadata.InvoiceItem(
            "Product A", 
            new BigDecimal("2"), 
            new BigDecimal("750.00"),
            new BigDecimal("1500.00")
        );
        
        metadata.addItem(item);
        assertTrue(metadata.isTotalAmountConsistent());
        
        metadata.setTotalAmount(new BigDecimal("2000.00"));
        assertFalse(metadata.isTotalAmountConsistent());
    }

    @Test
    void testAddAdditionalField() {
        metadata.addAdditionalField("taxRate", "18%");
        metadata.addAdditionalField("paymentTerms", "30 days");
        
        assertEquals("18%", metadata.getAdditionalFields().get("taxRate"));
        assertEquals("30 days", metadata.getAdditionalFields().get("paymentTerms"));
    }

    @Test
    void testInvoiceItemCreation() {
        DocumentMetadata.InvoiceItem item = new DocumentMetadata.InvoiceItem(
            "Product A", 
            new BigDecimal("2"), 
            new BigDecimal("750.00")
        );
        
        assertEquals("Product A", item.getDescription());
        assertEquals(new BigDecimal("2"), item.getQuantity());
        assertEquals(new BigDecimal("750.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("1500.00"), item.getTotal());
        assertTrue(item.isTotalConsistent());
    }

    @Test
    void testInvoiceItemTotalRecalculation() {
        DocumentMetadata.InvoiceItem item = new DocumentMetadata.InvoiceItem();
        item.setDescription("Product A");
        item.setQuantity(new BigDecimal("3"));
        item.setUnitPrice(new BigDecimal("100.00"));
        
        assertEquals(new BigDecimal("300.00"), item.getTotal());
        assertTrue(item.isTotalConsistent());
    }

    @Test
    void testToString() {
        String toString = metadata.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("ACME Corp"));
        assertTrue(toString.contains("INV-2024-001"));
    }
}