package com.arima.pms.service.command;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AdjustBatchStockCommand(
    @NotNull UUID performedByUserId,
    int quantityChange,
    String reason
) {
}
