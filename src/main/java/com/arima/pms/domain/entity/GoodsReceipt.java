package com.arima.pms.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "goods_receipts")
public class GoodsReceipt extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_order_id", nullable = false)
  private PurchaseOrder purchaseOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "received_by", nullable = false)
  private User receivedBy;

  @Column(name = "received_at", nullable = false)
  private LocalDateTime receivedAt;

  @Column(columnDefinition = "text")
  private String notes;

  @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Batch> batches = new ArrayList<>();

  public static GoodsReceipt create(PurchaseOrder purchaseOrder, User receivedBy, LocalDateTime receivedAt, String notes) {
    GoodsReceipt goodsReceipt = new GoodsReceipt();
    goodsReceipt.setPurchaseOrder(purchaseOrder);
    goodsReceipt.setReceivedBy(receivedBy);
    goodsReceipt.setReceivedAt(receivedAt);
    goodsReceipt.setNotes(notes);
    return goodsReceipt;
  }

  public void addBatch(Batch batch) {
    if (batch == null) {
      throw new IllegalArgumentException("Batch is required");
    }
    batch.setGoodsReceipt(this);
    batches.add(batch);
  }
}
