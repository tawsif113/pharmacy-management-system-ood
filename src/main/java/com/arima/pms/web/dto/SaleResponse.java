package com.arima.pms.web.dto;

import com.arima.pms.domain.entity.Sale;
import com.arima.pms.domain.entity.SaleItem;
import com.arima.pms.domain.enums.PaymentStatus;
import com.arima.pms.domain.enums.SaleStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SaleResponse(
    UUID id,
    String invoiceNumber,
    UUID customerId,
    String customerName,
    UUID prescriptionId,
    SaleStatus status,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal tax,
    BigDecimal total,
    PaymentStatus paymentStatus,
    UUID createdById,
    String createdByName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    int totalItemQuantity,
    List<SaleItemResponse> items
) {
  public static SaleResponse from(Sale sale) {
    List<SaleItemResponse> items = sale.getItems().stream().map(SaleItemResponse::from).toList();
    int totalQuantity = sale.getItems().stream().mapToInt(SaleItem::getQuantity).sum();
    return new SaleResponse(
        sale.getId(),
        sale.getInvoiceNumber(),
        sale.getCustomer() != null ? sale.getCustomer().getId() : null,
        sale.getCustomer() != null ? sale.getCustomer().getName() : null,
        sale.getPrescription() != null ? sale.getPrescription().getId() : null,
        sale.getStatus(),
        sale.getSubtotal(),
        sale.getDiscount(),
        sale.getTax(),
        sale.getTotal(),
        sale.getPaymentStatus(),
        sale.getCreatedBy() != null ? sale.getCreatedBy().getId() : null,
        sale.getCreatedBy() != null ? sale.getCreatedBy().getFullName() : null,
        sale.getCreatedAt(),
        sale.getUpdatedAt(),
        totalQuantity,
        items
    );
  }
}
