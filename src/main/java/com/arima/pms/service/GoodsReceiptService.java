package com.arima.pms.service;

import com.arima.pms.domain.entity.Batch;
import com.arima.pms.domain.entity.GoodsReceipt;
import com.arima.pms.domain.entity.PurchaseOrder;
import com.arima.pms.domain.entity.StockMovement;
import com.arima.pms.domain.entity.User;
import com.arima.pms.repository.GoodsReceiptRepository;
import com.arima.pms.repository.PurchaseOrderRepository;
import com.arima.pms.repository.StockMovementRepository;
import com.arima.pms.repository.UserRepository;
import com.arima.pms.service.command.CreateGoodsReceiptCommand;
import com.arima.pms.service.command.GoodsReceiptLineCommand;
import com.arima.pms.service.exception.InvalidGoodsReceiptException;
import com.arima.pms.service.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class GoodsReceiptService {

  private final GoodsReceiptRepository goodsReceiptRepository;
  private final PurchaseOrderRepository purchaseOrderRepository;
  private final UserRepository userRepository;
  private final StockMovementRepository stockMovementRepository;

  public GoodsReceipt createGoodsReceipt(CreateGoodsReceiptCommand command) {
    if (command == null) {
      throw new InvalidGoodsReceiptException("Goods receipt command is required");
    }
    if (CollectionUtils.isEmpty(command.lines())) {
      throw new InvalidGoodsReceiptException("Goods receipt must contain at least one line");
    }

    PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(command.purchaseOrderId())
        .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + command.purchaseOrderId()));
    if (!purchaseOrder.canReceive()) {
      throw new InvalidGoodsReceiptException("Purchase order must be approved before receipt: " + purchaseOrder.getId());
    }

    User receivedBy = userRepository.findById(command.receivedByUserId())
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + command.receivedByUserId()));
    if (!receivedBy.isActive()) {
      throw new InvalidGoodsReceiptException("User is inactive: " + receivedBy.getId());
    }

    LocalDateTime receivedAt = command.receivedAt() != null ? command.receivedAt() : LocalDateTime.now();
    GoodsReceipt receipt = GoodsReceipt.create(purchaseOrder, receivedBy, receivedAt, command.notes());

    List<StockMovement> movements = new ArrayList<>();
    for (GoodsReceiptLineCommand line : command.lines()) {
      validateLine(line);

      try {
        purchaseOrder.receiveItem(line.purchaseOrderItemId(), line.receivedQuantity());
      } catch (IllegalArgumentException ex) {
        throw new InvalidGoodsReceiptException(ex.getMessage());
      }

      Batch batch = Batch.createForReceipt(
          purchaseOrder.findItemById(line.purchaseOrderItemId()),
          receipt,
          line.batchNumber(),
          line.expiryDate(),
          line.purchaseCost(),
          line.sellingPrice(),
          line.receivedQuantity(),
          receivedAt
      );
      receipt.addBatch(batch);
    }

    GoodsReceipt savedReceipt = goodsReceiptRepository.saveAndFlush(receipt);
    for (Batch batch : savedReceipt.getBatches()) {
      movements.add(StockMovement.receipt(batch, receivedBy, savedReceipt.getId(), buildReason(command.notes())));
    }
    stockMovementRepository.saveAll(movements);
    hydrateForResponse(savedReceipt);
    return savedReceipt;
  }

  @Transactional(readOnly = true)
  public GoodsReceipt get(UUID id) {
    if (id == null) {
      throw new InvalidGoodsReceiptException("Goods receipt id is required");
    }
    GoodsReceipt receipt = goodsReceiptRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Goods receipt not found: " + id));
    hydrateForResponse(receipt);
    return receipt;
  }

  @Transactional(readOnly = true)
  public Page<GoodsReceipt> list(UUID purchaseOrderId, UUID receivedByUserId, Pageable pageable) {
    Specification<GoodsReceipt> specification = (root, query, cb) -> cb.conjunction();
    if (purchaseOrderId != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.join("purchaseOrder").get("id"), purchaseOrderId));
    }
    if (receivedByUserId != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.join("receivedBy").get("id"), receivedByUserId));
    }
    Page<GoodsReceipt> page = goodsReceiptRepository.findAll(specification, pageable);
    page.forEach(GoodsReceiptService::hydrateForResponse);
    return page;
  }

  private static void hydrateForResponse(GoodsReceipt receipt) {
    if (receipt == null) {
      return;
    }
    if (receipt.getPurchaseOrder() != null) {
      receipt.getPurchaseOrder().getPoNumber();
      if (receipt.getPurchaseOrder().getSupplier() != null) {
        receipt.getPurchaseOrder().getSupplier().getName();
      }
    }
    if (receipt.getReceivedBy() != null) {
      receipt.getReceivedBy().getFullName();
    }
    receipt.getBatches().forEach(batch -> {
      if (batch.getProduct() != null) {
        batch.getProduct().getName();
        batch.getProduct().getSkuBarcode();
      }
      if (batch.getPurchaseOrderItem() != null) {
        batch.getPurchaseOrderItem().getId();
      }
    });
  }

  private static void validateLine(GoodsReceiptLineCommand line) {
    if (line == null) {
      throw new InvalidGoodsReceiptException("Goods receipt line is required");
    }
    if (line.purchaseOrderItemId() == null) {
      throw new InvalidGoodsReceiptException("Goods receipt line must reference a purchase order item");
    }
    if (!StringUtils.hasText(line.batchNumber())) {
      throw new InvalidGoodsReceiptException("Batch number is required");
    }
    if (line.expiryDate() == null) {
      throw new InvalidGoodsReceiptException("Expiry date is required");
    }
    if (line.purchaseCost() == null) {
      throw new InvalidGoodsReceiptException("Purchase cost is required");
    }
    if (line.sellingPrice() == null) {
      throw new InvalidGoodsReceiptException("Selling price is required");
    }
  }

  private static String buildReason(String notes) {
    if (StringUtils.hasText(notes)) {
      return notes.trim();
    }
    return "Goods receipt";
  }
}
