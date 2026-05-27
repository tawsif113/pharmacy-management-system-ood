package com.arima.pms.domain.entity;

import com.arima.pms.domain.enums.PurchaseOrderStatus;
import com.arima.pms.domain.enums.PurchaseOrderStatusConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

  @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PurchaseOrderItem> items = new ArrayList<>();

  public PurchaseOrderItem addItem(Product product, int orderedQuantity, BigDecimal unitCost) {
    if (product == null) {
      throw new IllegalArgumentException("Product is required");
    }
    if (orderedQuantity <= 0) {
      throw new IllegalArgumentException("Ordered quantity must be positive");
    }
    if (unitCost == null) {
      throw new IllegalArgumentException("Unit cost is required");
    }
    if (unitCost.signum() < 0) {
      throw new IllegalArgumentException("Unit cost cannot be negative");
    }

    PurchaseOrderItem item = new PurchaseOrderItem();
    item.setPurchaseOrder(this);
    item.setProduct(product);
    item.setOrderedQuantity(orderedQuantity);
    item.setReceivedQuantity(0);
    item.setUnitCost(unitCost);
    items.add(item);
    totalEstimatedCost = totalEstimatedCost.add(unitCost.multiply(BigDecimal.valueOf(orderedQuantity)));
    return item;
  }

  public boolean canApprove() {
    return status == PurchaseOrderStatus.DRAFT;
  }

  public boolean canReceive() {
    return status == PurchaseOrderStatus.APPROVED || status == PurchaseOrderStatus.PARTIALLY_RECEIVED;
  }

  public boolean canCancel() {
    return status != PurchaseOrderStatus.RECEIVED && status != PurchaseOrderStatus.CANCELLED;
  }

  public void approve() {
    if (!canApprove()) {
      throw new IllegalStateException("Only draft purchase orders can be approved");
    }
    status = PurchaseOrderStatus.APPROVED;
  }

  public void cancel() {
    if (!canCancel()) {
      throw new IllegalStateException("Purchase order cannot be cancelled in its current state");
    }
    status = PurchaseOrderStatus.CANCELLED;
  }

  public PurchaseOrderItem findItemById(UUID itemId) {
    if (itemId == null) {
      throw new IllegalArgumentException("Purchase order item id is required");
    }
    for (PurchaseOrderItem item : items) {
      if (itemId.equals(item.getId())) {
        return item;
      }
    }
    throw new IllegalArgumentException("Purchase order item not found: " + itemId);
  }

  public void receiveItem(UUID itemId, int quantity) {
    PurchaseOrderItem item = findItemById(itemId);
    item.receive(quantity);
    syncReceiptStatus();
  }

  public boolean isFullyReceived() {
    if (items.isEmpty()) {
      return false;
    }
    for (PurchaseOrderItem item : items) {
      if (!item.isFullyReceived()) {
        return false;
      }
    }
    return true;
  }

  public void syncReceiptStatus() {
    if (items.isEmpty()) {
      return;
    }

    boolean anyQuantityReceived = false;
    boolean allFullyReceived = true;

    for (PurchaseOrderItem item : items) {
      if (item.getReceivedQuantity() > 0) {
        anyQuantityReceived = true;
      }
      if (!item.isFullyReceived()) {
        allFullyReceived = false;
      }
    }

    if (allFullyReceived) {
      markReceived();
      return;
    }

    if (anyQuantityReceived) {
      markPartiallyReceived();
    }
  }

  public void markReceived() {
    status = PurchaseOrderStatus.RECEIVED;
  }

  public void markPartiallyReceived() {
    status = PurchaseOrderStatus.PARTIALLY_RECEIVED;
  }
}
