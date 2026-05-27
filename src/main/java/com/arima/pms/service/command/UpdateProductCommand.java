package com.arima.pms.service.command;

import jakarta.validation.constraints.Min;

public record UpdateProductCommand(
    String skuBarcode,
    String name,
    String brand,
    String genericName,
    String category,
    String dosageForm,
    String strength,
    String packSize,
    Boolean prescriptionRequired,
    @Min(0) Integer reorderLevel,
    Boolean active
) {
}
