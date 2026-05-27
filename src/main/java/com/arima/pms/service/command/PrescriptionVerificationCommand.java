package com.arima.pms.service.command;

public record PrescriptionVerificationCommand(
    String verifiedBy,
    String notes
) {
}
