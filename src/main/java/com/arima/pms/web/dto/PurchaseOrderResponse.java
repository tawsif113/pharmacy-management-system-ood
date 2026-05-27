package com.arima.pms.web.dto;

import com.arima.pms.domain.entity.PurchaseOrder;
import com.arima.pms.domain.entity.PurchaseOrderItem;
import com.arima.pms.domain.enums.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderResponse(
    UUID id,
    String poNumber,
    UUID supplierId,
    String supplierName,
    PurchaseOrderStatus status,
    LocalDate expectedDeliveryDate,
    BigDecimal totalEstimatedCost,
    UUID createdById,
    String createdByName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    int totalOrderedQuantity,
    int totalReceivedQuantity,
    List<PurchaseOrderLineResponse> items
) {
  public static PurchaseOrderResponse from(PurchaseOrder purchaseOrder) {
    List<PurchaseOrderLineResponse> lines = purchaseOrder.getItems().stream()
        .map(PurchaseOrderLineResponse::from)
        .toList();
    int totalOrderedQuantity = purchaseOrder.getItems().stream().mapToInt(PurchaseOrderItem::getOrderedQuantity).sum();
    int totalReceivedQuantity = purchaseOrder.getItems().stream().mapToInt(PurchaseOrderItem::getReceivedQuantity).sum();
    return new PurchaseOrderResponse(
        purchaseOrder.getId(),
        purchaseOrder.getPoNumber(),
        purchaseOrder.getSupplier() != null ? purchaseOrder.getSupplier().getId() : null,
        purchaseOrder.getSupplier() != null ? purchaseOrder.getSupplier().getName() : null,
        purchaseOrder.getStatus(),
        purchaseOrder.getExpectedDeliveryDate(),
        purchaseOrder.getTotalEstimatedCost(),
        purchaseOrder.getCreatedBy() != null ? purchaseOrder.getCreatedBy().getId() : null,
        purchaseOrder.getCreatedBy() != null ? purchaseOrder.getCreatedBy().getFullName() : null,
        purchaseOrder.getCreatedAt(),
        purchaseOrder.getUpdatedAt(),
        totalOrderedQuantity,
        totalReceivedQuantity,
        lines
    );
  }
}
