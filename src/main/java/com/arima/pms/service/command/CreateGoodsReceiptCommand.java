package com.arima.pms.service.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateGoodsReceiptCommand(
    @NotNull UUID purchaseOrderId,
    @NotNull UUID receivedByUserId,
    LocalDateTime receivedAt,
    String notes,
    @NotEmpty List<@Valid GoodsReceiptLineCommand> lines
) {
}
