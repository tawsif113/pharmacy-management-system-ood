package com.arima.pms.web.dto;

import com.arima.pms.domain.entity.Batch;
import com.arima.pms.domain.enums.BatchStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BatchResponse(
    UUID id,
    UUID productId,
    String productName,
    String skuBarcode,
    UUID supplierId,
    String supplierName,
    UUID purchaseOrderItemId,
    UUID goodsReceiptId,
    String batchNumber,
    LocalDate expiryDate,
    BigDecimal purchaseCost,
    BigDecimal sellingPrice,
    int receivedQuantity,
    int availableQuantity,
    BatchStatus status,
    LocalDateTime receivedAt
) {
  public static BatchResponse from(Batch batch) {
    return new BatchResponse(
        batch.getId(),
        batch.getProduct() != null ? batch.getProduct().getId() : null,
        batch.getProduct() != null ? batch.getProduct().getName() : null,
        batch.getProduct() != null ? batch.getProduct().getSkuBarcode() : null,
        batch.getSupplier() != null ? batch.getSupplier().getId() : null,
        batch.getSupplier() != null ? batch.getSupplier().getName() : null,
        batch.getPurchaseOrderItem() != null ? batch.getPurchaseOrderItem().getId() : null,
        batch.getGoodsReceipt() != null ? batch.getGoodsReceipt().getId() : null,
        batch.getBatchNumber(),
        batch.getExpiryDate(),
        batch.getPurchaseCost(),
        batch.getSellingPrice(),
        batch.getReceivedQuantity(),
        batch.getAvailableQuantity(),
        batch.lifecycleStatus(),
        batch.getReceivedAt()
    );
  }
}
