package com.arima.pms.domain.entity;

import com.arima.pms.domain.enums.PurchaseOrderStatus;
import com.arima.pms.domain.enums.PurchaseOrderStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder extends BaseEntity {

  @Column(name = "po_number", nullable = false, unique = true, length = 50)
  private String poNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id", nullable = false)
  private Supplier supplier;

  @Convert(converter = PurchaseOrderStatusConverter.class)
  @Column(nullable = false, length = 30)
  private PurchaseOrderStatus status;

  @Column(name = "expected_delivery_date")
  private LocalDate expectedDeliveryDate;

  @Column(name = "total_estimated_cost", nullable = false, precision = 14, scale = 2)
  private BigDecimal totalEstimatedCost = BigDecimal.ZERO;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;
}


