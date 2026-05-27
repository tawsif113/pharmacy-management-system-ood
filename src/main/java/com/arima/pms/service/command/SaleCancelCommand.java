package com.arima.pms.service.command;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SaleCancelCommand(
    @NotNull UUID performedByUserId,
    String reason
) {
}
