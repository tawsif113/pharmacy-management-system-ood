package com.arima.pms.service.command;

import java.time.LocalDate;

public record UpdateCustomerCommand(
    String name,
    String phone,
    String address,
    LocalDate dateOfBirth,
    String notes
) {
}
