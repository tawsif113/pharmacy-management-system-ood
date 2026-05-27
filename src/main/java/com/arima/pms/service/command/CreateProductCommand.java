package com.arima.pms.service.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateProductCommand(
    @NotBlank String skuBarcode,
    @NotBlank String name,
    String brand,
    String genericName,
    String category,
    String dosageForm,
    String strength,
    String packSize,
    boolean prescriptionRequired,
    @Min(0) int reorderLevel,
    boolean active
) {
}
