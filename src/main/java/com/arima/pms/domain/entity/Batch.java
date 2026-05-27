package com.arima.pms.domain.entity;

import com.arima.pms.domain.enums.BatchStatus;
import com.arima.pms.domain.enums.BatchStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "batches")
public class Batch extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id", nullable = false)
  private Supplier supplier;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_order_item_id")
  private PurchaseOrderItem purchaseOrderItem;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "goods_receipt_id")
  private GoodsReceipt goodsReceipt;

  @Column(name = "batch_number", nullable = false, length = 120)
  private String batchNumber;

  @Column(name = "expiry_date", nullable = false)
  private LocalDate expiryDate;

  @Column(name = "purchase_cost", nullable = false, precision = 14, scale = 2)
  private BigDecimal purchaseCost;

  @Column(name = "selling_price", nullable = false, precision = 14, scale = 2)
  private BigDecimal sellingPrice;

  @Column(name = "received_quantity", nullable = false)
  private int receivedQuantity;

  @Column(name = "available_quantity", nullable = false)
  private int availableQuantity;

  @Column(name = "received_at", nullable = false)
  private LocalDateTime receivedAt;

  @Convert(converter = BatchStatusConverter.class)
  @Column(nullable = false, length = 20)
  private BatchStatus status;

  public static Batch createForReceipt(
      PurchaseOrderItem purchaseOrderItem,
      GoodsReceipt goodsReceipt,
      String batchNumber,
      LocalDate expiryDate,
      BigDecimal purchaseCost,
      BigDecimal sellingPrice,
      int receivedQuantity,
      LocalDateTime receivedAt
  ) {
    if (purchaseOrderItem == null) {
      throw new IllegalArgumentException("Purchase order item is required");
    }
    if (goodsReceipt == null) {
      throw new IllegalArgumentException("Goods receipt is required");
    }
    if (batchNumber == null || batchNumber.trim().isEmpty()) {
      throw new IllegalArgumentException("Batch number is required");
    }
    if (expiryDate == null) {
      throw new IllegalArgumentException("Expiry date is required");
    }
    if (purchaseCost == null) {
      throw new IllegalArgumentException("Purchase cost is required");
    }
    if (sellingPrice == null) {
      throw new IllegalArgumentException("Selling price is required");
    }
    if (receivedQuantity <= 0) {
      throw new IllegalArgumentException("Received quantity must be positive");
    }
    if (receivedAt == null) {
      throw new IllegalArgumentException("Received at is required");
    }

    Batch batch = new Batch();
    batch.setProduct(purchaseOrderItem.getProduct());
    batch.setSupplier(purchaseOrderItem.getPurchaseOrder().getSupplier());
    batch.setPurchaseOrderItem(purchaseOrderItem);
    batch.setGoodsReceipt(goodsReceipt);
    batch.setBatchNumber(batchNumber.trim());
    batch.setExpiryDate(expiryDate);
    batch.setPurchaseCost(purchaseCost);
    batch.setSellingPrice(sellingPrice);
    batch.setReceivedQuantity(receivedQuantity);
    batch.setAvailableQuantity(receivedQuantity);
    batch.setReceivedAt(receivedAt);
    batch.setStatus(BatchStatus.AVAILABLE);
    return batch;
  }

  public int remainingQuantity() {
    return availableQuantity;
  }

  public BatchStatus lifecycleStatus() {
    if (status == BatchStatus.VOIDED) {
      return BatchStatus.VOIDED;
    }
    if (availableQuantity <= 0) {
      return BatchStatus.DEPLETED;
    }
    if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
      return BatchStatus.EXPIRED;
    }
    return BatchStatus.AVAILABLE;
  }

  public void adjustAvailableQuantity(int delta) {
    if (delta == 0) {
      throw new IllegalArgumentException("Quantity change must not be zero");
    }
    int updated = availableQuantity + delta;
    if (updated < 0) {
      throw new IllegalArgumentException("Quantity change exceeds available stock");
    }
    availableQuantity = updated;
    status = lifecycleStatus();
  }

  public boolean isLowStock(int reorderLevel) {
    return availableQuantity <= reorderLevel;
  }
}
