package com.arima.pms.service.command;

import java.time.LocalDate;
import java.util.UUID;
import com.arima.pms.domain.enums.VerificationStatus;

public record UpdatePrescriptionCommand(
    UUID customerId,
    String doctorName,
    String doctorRegistrationNo,
    LocalDate issueDate,
    LocalDate expiryDate,
    String fileUrl,
    VerificationStatus verificationStatus
) {
}
