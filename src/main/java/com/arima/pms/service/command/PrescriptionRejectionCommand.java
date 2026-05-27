package com.arima.pms.service.command;

import jakarta.validation.constraints.NotBlank;

public record PrescriptionRejectionCommand(
    @NotBlank String reason
) {
}
