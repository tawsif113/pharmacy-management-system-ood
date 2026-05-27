package com.arima.pms.web.dto;

import java.time.LocalDate;
import java.util.UUID;

public record LowStockAlertResponse(
    UUID productId,
    String productName,
    String skuBarcode,
    int reorderLevel,
    int totalAvailableQuantity,
    int shortageQuantity,
    int batchCount,
    LocalDate earliestExpiryDate
) {
}
