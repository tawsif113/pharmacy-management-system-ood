package com.arima.pms.web.dto;

import com.arima.pms.domain.entity.Prescription;
import com.arima.pms.domain.enums.VerificationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PrescriptionResponse(
    UUID id,
    UUID customerId,
    String customerName,
    String doctorName,
    String doctorRegistrationNo,
    LocalDate issueDate,
    LocalDate expiryDate,
    String fileUrl,
    VerificationStatus verificationStatus,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
  public static PrescriptionResponse from(Prescription prescription) {
    return new PrescriptionResponse(
        prescription.getId(),
        prescription.getCustomer() != null ? prescription.getCustomer().getId() : null,
        prescription.getCustomer() != null ? prescription.getCustomer().getName() : null,
        prescription.getDoctorName(),
        prescription.getDoctorRegistrationNo(),
        prescription.getIssueDate(),
        prescription.getExpiryDate(),
        prescription.getFileUrl(),
        prescription.getVerificationStatus(),
        prescription.getCreatedAt(),
        prescription.getUpdatedAt()
    );
  }
}
