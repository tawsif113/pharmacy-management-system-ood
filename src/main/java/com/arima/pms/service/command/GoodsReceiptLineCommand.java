package com.arima.pms.service.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoodsReceiptLineCommand(
    @NotNull UUID purchaseOrderItemId,
    @Min(1) int receivedQuantity,
    @NotNull String batchNumber,
    @NotNull LocalDate expiryDate,
    @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal purchaseCost,
    @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal sellingPrice
) {
}
