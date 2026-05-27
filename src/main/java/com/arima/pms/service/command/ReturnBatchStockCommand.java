package com.arima.pms.service.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReturnBatchStockCommand(
    @NotNull UUID performedByUserId,
    @Min(1) int quantity,
    String reason
) {
}
