package com.arima.pms.web.dto;

import com.arima.pms.domain.entity.SaleItem;
import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemResponse(
    UUID id,
    UUID productId,
    String productName,
    String skuBarcode,
    UUID batchId,
    String batchNumber,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal discount,
    BigDecimal lineTotal
) {
  public static SaleItemResponse from(SaleItem item) {
    return new SaleItemResponse(
        item.getId(),
        item.getProduct() != null ? item.getProduct().getId() : null,
        item.getProduct() != null ? item.getProduct().getName() : null,
        item.getProduct() != null ? item.getProduct().getSkuBarcode() : null,
        item.getBatch() != null ? item.getBatch().getId() : null,
        item.getBatch() != null ? item.getBatch().getBatchNumber() : null,
        item.getQuantity(),
        item.getUnitPrice(),
        item.getDiscount(),
        item.getLineTotal()
    );
  }
}
