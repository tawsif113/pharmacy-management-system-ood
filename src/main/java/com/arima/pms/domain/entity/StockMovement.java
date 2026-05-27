package com.arima.pms.domain.entity;

import com.arima.pms.domain.enums.StockMovementType;
import com.arima.pms.domain.enums.StockMovementTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_movements")
public class StockMovement extends CreatedAtEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "batch_id", nullable = false)
  private Batch batch;

  @Convert(converter = StockMovementTypeConverter.class)
  @Column(nullable = false, length = 20)
  private StockMovementType type;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "delta_quantity", nullable = false)
  private int deltaQuantity;

  @Column(name = "reference_type", nullable = false, length = 50)
  private String referenceType;

  @Column(name = "reference_id", nullable = false)
  private UUID referenceId;

  @Column(columnDefinition = "text")
  private String reason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  public static StockMovement receipt(Batch batch, User createdBy, UUID referenceId, String reason) {
    if (batch == null) {
      throw new IllegalArgumentException("Batch is required");
    }
    if (createdBy == null) {
      throw new IllegalArgumentException("Created by user is required");
    }
    if (referenceId == null) {
      throw new IllegalArgumentException("Reference id is required");
    }

    StockMovement movement = new StockMovement();
    movement.setProduct(batch.getProduct());
    movement.setBatch(batch);
    movement.setType(StockMovementType.RECEIPT);
    movement.setQuantity(batch.getReceivedQuantity());
    movement.setDeltaQuantity(batch.getReceivedQuantity());
    movement.setReferenceType("goods_receipt");
    movement.setReferenceId(referenceId);
    movement.setReason(reason);
    movement.setCreatedBy(createdBy);
    return movement;
  }

  public static StockMovement adjustment(Batch batch, User createdBy, UUID referenceId, String reason, int deltaQuantity) {
    validateStockMovement(batch, createdBy, referenceId, deltaQuantity);
    StockMovement movement = new StockMovement();
    movement.setProduct(batch.getProduct());
    movement.setBatch(batch);
    movement.setType(StockMovementType.ADJUSTMENT);
    movement.setQuantity(Math.abs(deltaQuantity));
    movement.setDeltaQuantity(deltaQuantity);
    movement.setReferenceType("inventory_adjustment");
    movement.setReferenceId(referenceId);
    movement.setReason(reason);
    movement.setCreatedBy(createdBy);
    return movement;
  }

  public static StockMovement returned(Batch batch, User createdBy, UUID referenceId, String reason, int deltaQuantity) {
    validateStockMovement(batch, createdBy, referenceId, deltaQuantity);
    StockMovement movement = new StockMovement();
    movement.setProduct(batch.getProduct());
    movement.setBatch(batch);
    movement.setType(StockMovementType.RETURNED);
    movement.setQuantity(Math.abs(deltaQuantity));
    movement.setDeltaQuantity(deltaQuantity);
    movement.setReferenceType("inventory_return");
    movement.setReferenceId(referenceId);
    movement.setReason(reason);
    movement.setCreatedBy(createdBy);
    return movement;
  }

  public static StockMovement writeOff(Batch batch, User createdBy, UUID referenceId, String reason, int deltaQuantity) {
    validateStockMovement(batch, createdBy, referenceId, deltaQuantity);
    StockMovement movement = new StockMovement();
    movement.setProduct(batch.getProduct());
    movement.setBatch(batch);
    movement.setType(StockMovementType.WRITE_OFF);
    movement.setQuantity(Math.abs(deltaQuantity));
    movement.setDeltaQuantity(deltaQuantity);
    movement.setReferenceType("inventory_write_off");
    movement.setReferenceId(referenceId);
    movement.setReason(reason);
    movement.setCreatedBy(createdBy);
    return movement;
  }

  public static StockMovement sale(Batch batch, User createdBy, UUID referenceId, String reason, int deltaQuantity) {
    validateStockMovement(batch, createdBy, referenceId, deltaQuantity);
    StockMovement movement = new StockMovement();
    movement.setProduct(batch.getProduct());
    movement.setBatch(batch);
    movement.setType(StockMovementType.SALE);
    movement.setQuantity(Math.abs(deltaQuantity));
    movement.setDeltaQuantity(deltaQuantity);
    movement.setReferenceType("sale");
    movement.setReferenceId(referenceId);
    movement.setReason(reason);
    movement.setCreatedBy(createdBy);
    return movement;
  }

  public static StockMovement cancelled(Batch batch, User createdBy, UUID referenceId, String reason, int deltaQuantity) {
    validateStockMovement(batch, createdBy, referenceId, deltaQuantity);
    StockMovement movement = new StockMovement();
    movement.setProduct(batch.getProduct());
    movement.setBatch(batch);
    movement.setType(StockMovementType.CANCELLED);
    movement.setQuantity(Math.abs(deltaQuantity));
    movement.setDeltaQuantity(deltaQuantity);
    movement.setReferenceType("sale_void");
    movement.setReferenceId(referenceId);
    movement.setReason(reason);
    movement.setCreatedBy(createdBy);
    return movement;
  }

  private static void validateStockMovement(Batch batch, User createdBy, UUID referenceId, int deltaQuantity) {
    if (batch == null) {
      throw new IllegalArgumentException("Batch is required");
    }
    if (createdBy == null) {
      throw new IllegalArgumentException("Created by user is required");
    }
    if (referenceId == null) {
      throw new IllegalArgumentException("Reference id is required");
    }
    if (deltaQuantity == 0) {
      throw new IllegalArgumentException("Quantity change must not be zero");
    }
  }
}
