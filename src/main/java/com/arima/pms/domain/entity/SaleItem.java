package com.arima.pms.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sale_items")
public class SaleItem extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sale_id", nullable = false)
  private Sale sale;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "batch_id", nullable = false)
  private Batch batch;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
  private BigDecimal unitPrice;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal discount = BigDecimal.ZERO;

  @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
  private BigDecimal lineTotal;

  public void recalculateLineTotal() {
    BigDecimal subtotal = unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
    BigDecimal discountValue = discount == null ? BigDecimal.ZERO : discount;
    lineTotal = subtotal.subtract(discountValue).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
  }
}


