package com.arima.pms.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_order_id", nullable = false)
  private PurchaseOrder purchaseOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "ordered_quantity", nullable = false)
  private int orderedQuantity;

  @Column(name = "received_quantity", nullable = false)
  private int receivedQuantity;

  @Column(name = "unit_cost", nullable = false, precision = 14, scale = 2)
  private BigDecimal unitCost;

  public int remainingQuantity() {
    return orderedQuantity - receivedQuantity;
  }

  public boolean isFullyReceived() {
    return receivedQuantity >= orderedQuantity;
  }

  public void receive(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Received quantity must be positive");
    }
    if (receivedQuantity + quantity > orderedQuantity) {
      throw new IllegalArgumentException("Received quantity exceeds remaining ordered quantity");
    }
    receivedQuantity += quantity;
  }
}
