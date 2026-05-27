package com.arima.pms.web.dto;

import com.arima.pms.domain.entity.Batch;
import com.arima.pms.domain.enums.BatchStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record GoodsReceiptBatchResponse(
    UUID id,
    UUID productId,
    String productName,
    UUID purchaseOrderItemId,
    String batchNumber,
    LocalDate expiryDate,
    BigDecimal purchaseCost,
    BigDecimal sellingPrice,
    int receivedQuantity,
    int availableQuantity,
    BatchStatus status,
    LocalDateTime receivedAt
) {
  public static GoodsReceiptBatchResponse from(Batch batch) {
    return new GoodsReceiptBatchResponse(
        batch.getId(),
        batch.getProduct() != null ? batch.getProduct().getId() : null,
        batch.getProduct() != null ? batch.getProduct().getName() : null,
        batch.getPurchaseOrderItem() != null ? batch.getPurchaseOrderItem().getId() : null,
        batch.getBatchNumber(),
        batch.getExpiryDate(),
        batch.getPurchaseCost(),
        batch.getSellingPrice(),
        batch.getReceivedQuantity(),
        batch.getAvailableQuantity(),
        batch.getStatus(),
        batch.getReceivedAt()
    );
  }
}
