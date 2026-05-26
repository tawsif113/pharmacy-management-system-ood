package com.arima.pms.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

  @Column(name = "sku_barcode", nullable = false, unique = true, length = 120)
  private String skuBarcode;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 120)
  private String brand;

  @Column(name = "generic_name", length = 160)
  private String genericName;

  @Column(length = 120)
  private String category;

  @Column(name = "dosage_form", length = 80)
  private String dosageForm;

  @Column(length = 80)
  private String strength;

  @Column(name = "pack_size", length = 80)
  private String packSize;

  @Column(name = "is_prescription_required", nullable = false)
  private boolean prescriptionRequired;

  @Column(name = "reorder_level", nullable = false)
  private int reorderLevel;

  @Column(nullable = false)
  private boolean active = true;
}


