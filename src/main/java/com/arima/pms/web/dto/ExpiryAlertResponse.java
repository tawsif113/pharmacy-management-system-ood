package com.arima.pms.web.dto;

import com.arima.pms.domain.enums.BatchStatus;
import java.time.LocalDate;
import java.util.UUID;

public record ExpiryAlertResponse(
    UUID batchId,
    UUID productId,
    String productName,
    String skuBarcode,
    String batchNumber,
    LocalDate expiryDate,
    long daysUntilExpiry,
    int availableQuantity,
    BatchStatus status
) {
}
