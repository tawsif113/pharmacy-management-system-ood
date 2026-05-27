package com.arima.pms.service.command;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateSaleCommand(
    UUID customerId,
    UUID prescriptionId,
    BigDecimal discount,
    BigDecimal tax,
    List<@Valid CreateSaleLineCommand> items
) {
}
