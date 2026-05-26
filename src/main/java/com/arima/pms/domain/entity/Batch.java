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
}


