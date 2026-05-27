package com.arima.pms.web.dto;

import com.arima.pms.domain.entity.Customer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponse(
    UUID id,
    String name,
    String phone,
    String address,
    LocalDate dateOfBirth,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
  public static CustomerResponse from(Customer customer) {
    return new CustomerResponse(
        customer.getId(),
        customer.getName(),
        customer.getPhone(),
        customer.getAddress(),
        customer.getDateOfBirth(),
        customer.getNotes(),
        customer.getCreatedAt(),
        customer.getUpdatedAt()
    );
  }
}
