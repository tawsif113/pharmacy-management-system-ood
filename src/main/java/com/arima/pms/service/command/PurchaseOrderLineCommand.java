package com.arima.pms.service.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseOrderLineCommand(
    @NotNull UUID productId,
    @Positive int orderedQuantity,
    @NotNull BigDecimal unitCost
) {
}
