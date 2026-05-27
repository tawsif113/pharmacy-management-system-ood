package com.arima.pms.service.command;

import com.arima.pms.domain.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SaleConfirmCommand(
    @NotNull UUID performedByUserId,
    PaymentStatus paymentStatus
) {
}
