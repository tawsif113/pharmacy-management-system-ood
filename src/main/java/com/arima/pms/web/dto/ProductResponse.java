package com.arima.pms.web.dto;

import com.arima.pms.domain.entity.Product;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String skuBarcode,
    String name,
    String brand,
    String genericName,
    String category,
    String dosageForm,
    String strength,
    String packSize,
    boolean prescriptionRequired,
    int reorderLevel,
    boolean active
) {
  public static ProductResponse from(Product product) {
    return new ProductResponse(
        product.getId(),
        product.getSkuBarcode(),
        product.getName(),
        product.getBrand(),
        product.getGenericName(),
        product.getCategory(),
        product.getDosageForm(),
        product.getStrength(),
        product.getPackSize(),
        product.isPrescriptionRequired(),
        product.getReorderLevel(),
        product.isActive()
    );
  }
}
