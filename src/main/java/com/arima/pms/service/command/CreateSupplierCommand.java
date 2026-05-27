package com.arima.pms.service.command;

import jakarta.validation.constraints.NotBlank;

public record CreateSupplierCommand(
    @NotBlank String name,
    String phone,
    String email,
    String address,
    Boolean active
) {
}
