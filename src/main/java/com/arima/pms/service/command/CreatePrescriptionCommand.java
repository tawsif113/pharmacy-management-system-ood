package com.arima.pms.service.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePrescriptionCommand(
    @NotNull UUID customerId,
    @NotBlank String doctorName,
    String doctorRegistrationNo,
    @NotNull LocalDate issueDate,
    @NotNull LocalDate expiryDate,
    String fileUrl,
    com.arima.pms.domain.enums.VerificationStatus verificationStatus
) {
}
