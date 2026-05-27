package com.arima.pms.service.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSaleCommand(
    @NotNull UUID createdByUserId,
    UUID customerId,
    UUID prescriptionId,
    BigDecimal discount,
    BigDecimal tax,
    @NotEmpty List<@Valid CreateSaleLineCommand> items
) {
}
