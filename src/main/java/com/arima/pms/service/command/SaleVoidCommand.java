package com.arima.pms.service.command;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SaleVoidCommand(
    @NotNull UUID performedByUserId,
    String reason
) {
}
