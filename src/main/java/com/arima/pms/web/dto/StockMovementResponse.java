package com.arima.pms.web.dto;

import com.arima.pms.domain.entity.StockMovement;
import com.arima.pms.domain.enums.StockMovementType;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementResponse(
    UUID id,
    UUID productId,
    String productName,
    String skuBarcode,
    UUID batchId,
    String batchNumber,
    StockMovementType type,
    int quantity,
    int deltaQuantity,
    String referenceType,
    UUID referenceId,
    String reason,
    UUID createdById,
    String createdByName,
    LocalDateTime createdAt
) {
  public static StockMovementResponse from(StockMovement movement) {
    return new StockMovementResponse(
        movement.getId(),
        movement.getProduct() != null ? movement.getProduct().getId() : null,
        movement.getProduct() != null ? movement.getProduct().getName() : null,
        movement.getProduct() != null ? movement.getProduct().getSkuBarcode() : null,
        movement.getBatch() != null ? movement.getBatch().getId() : null,
        movement.getBatch() != null ? movement.getBatch().getBatchNumber() : null,
        movement.getType(),
        movement.getQuantity(),
        movement.getDeltaQuantity(),
        movement.getReferenceType(),
        movement.getReferenceId(),
        movement.getReason(),
        movement.getCreatedBy() != null ? movement.getCreatedBy().getId() : null,
        movement.getCreatedBy() != null ? movement.getCreatedBy().getFullName() : null,
        movement.getCreatedAt()
    );
  }
}
