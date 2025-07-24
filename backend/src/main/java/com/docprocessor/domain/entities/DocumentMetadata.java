package com.docprocessor.domain.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@ToString
public class DocumentMetadata {

    @NotBlank(message = "Invoice number is required")
    @Pattern(regexp = "^[A-Za-z0-9\\-_]+$", message = "Invoice number can only contain letters, numbers, hyphens and underscores")
    private String invoiceNumber;

    @NotNull(message = "Invoice date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate invoiceDate;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Total amount must have at most 10 integer digits and 2 decimal places")
    private BigDecimal totalAmount;

    @NotNull(message = "Items list cannot be null")
    @Size(min = 1, message = "At least one item is required")
    private List<InvoiceItem> items = new ArrayList<>();

    private Map<String, Object> additionalFields = new HashMap<>();

    public DocumentMetadata(String invoiceNumber, LocalDate invoiceDate, BigDecimal totalAmount) {
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.totalAmount = totalAmount;
        this.items = new ArrayList<>();
        this.additionalFields = new HashMap<>();
    }

    public void setItems(List<InvoiceItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void addItem(InvoiceItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    public void setAdditionalFields(Map<String, Object> additionalFields) {
        this.additionalFields = additionalFields != null ? additionalFields : new HashMap<>();
    }

    public void addAdditionalField(String key, Object value) {
        if (this.additionalFields == null) {
            this.additionalFields = new HashMap<>();
        }
        this.additionalFields.put(key, value);
    }
    public BigDecimal calculateItemsTotal() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return items.stream()
                .map(InvoiceItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isTotalAmountConsistent() {
        BigDecimal itemsTotal = calculateItemsTotal();
        return totalAmount != null && totalAmount.compareTo(itemsTotal) == 0;
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    @Data
    @NoArgsConstructor
    @ToString
    public static class InvoiceItem {
        
        @NotBlank(message = "Item description is required")
        @Size(max = 200, message = "Item description cannot exceed 200 characters")
        private String description;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
        private BigDecimal quantity;

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
        private BigDecimal unitPrice;

        @NotNull(message = "Total is required")
        @DecimalMin(value = "0.01", message = "Total must be greater than 0")
        private BigDecimal total;

        public InvoiceItem(String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal total) {
            this.description = description;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.total = total;
        }

        public InvoiceItem(String description, BigDecimal quantity, BigDecimal unitPrice) {
            this.description = description;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.total = quantity.multiply(unitPrice);
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
            recalculateTotal();
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            recalculateTotal();
        }
        private void recalculateTotal() {
            if (quantity != null && unitPrice != null) {
                this.total = quantity.multiply(unitPrice);
            }
        }

        public boolean isTotalConsistent() {
            if (quantity == null || unitPrice == null || total == null) {
                return false;
            }
            BigDecimal calculatedTotal = quantity.multiply(unitPrice);
            return calculatedTotal.compareTo(total) == 0;
        }

    }
}