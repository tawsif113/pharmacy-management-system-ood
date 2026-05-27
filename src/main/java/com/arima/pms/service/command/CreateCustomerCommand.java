package com.arima.pms.service.command;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateCustomerCommand(
    @NotBlank String name,
    String phone,
    String address,
    LocalDate dateOfBirth,
    String notes
) {
}
