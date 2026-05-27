package com.arima.pms.web.dto;

import com.arima.pms.domain.entity.Supplier;
import java.time.LocalDateTime;
import java.util.UUID;

public record SupplierResponse(
    UUID id,
    String name,
    String phone,
    String email,
    String address,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
  public static SupplierResponse from(Supplier supplier) {
    return new SupplierResponse(
        supplier.getId(),
        supplier.getName(),
        supplier.getPhone(),
        supplier.getEmail(),
        supplier.getAddress(),
        supplier.isActive(),
        supplier.getCreatedAt(),
        supplier.getUpdatedAt()
    );
  }
}
