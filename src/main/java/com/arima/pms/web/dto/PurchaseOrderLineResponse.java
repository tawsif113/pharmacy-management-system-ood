package com.arima.pms.web.dto;

import com.arima.pms.domain.entity.PurchaseOrderItem;
import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseOrderLineResponse(
    UUID id,
    UUID productId,
    String productName,
    String skuBarcode,
    int orderedQuantity,
    int receivedQuantity,
    int remainingQuantity,
    BigDecimal unitCost,
    BigDecimal lineTotal
) {
  public static PurchaseOrderLineResponse from(PurchaseOrderItem item) {
    BigDecimal lineTotal = item.getUnitCost().multiply(BigDecimal.valueOf(item.getOrderedQuantity()));
    return new PurchaseOrderLineResponse(
        item.getId(),
        item.getProduct() != null ? item.getProduct().getId() : null,
        item.getProduct() != null ? item.getProduct().getName() : null,
        item.getProduct() != null ? item.getProduct().getSkuBarcode() : null,
        item.getOrderedQuantity(),
        item.getReceivedQuantity(),
        item.remainingQuantity(),
        item.getUnitCost(),
        lineTotal
    );
  }
}
