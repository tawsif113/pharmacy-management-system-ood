package com.arima.pms.service.command;

public record UpdateSupplierCommand(
    String name,
    String phone,
    String email,
    String address,
    Boolean active
) {
}
