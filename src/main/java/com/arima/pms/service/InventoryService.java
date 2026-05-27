package com.arima.pms.service;

import com.arima.pms.domain.entity.Batch;
import com.arima.pms.domain.entity.Product;
import com.arima.pms.domain.entity.StockMovement;
import com.arima.pms.domain.entity.User;
import com.arima.pms.domain.enums.BatchStatus;
import com.arima.pms.domain.enums.StockMovementType;
import com.arima.pms.repository.BatchRepository;
import com.arima.pms.repository.ProductRepository;
import com.arima.pms.repository.StockMovementRepository;
import com.arima.pms.repository.UserRepository;
import com.arima.pms.service.command.AdjustBatchStockCommand;
import com.arima.pms.service.command.ReturnBatchStockCommand;
import com.arima.pms.service.command.WriteOffBatchStockCommand;
import com.arima.pms.service.exception.InvalidInventoryException;
import com.arima.pms.service.exception.ResourceNotFoundException;
import com.arima.pms.web.dto.ExpiryAlertResponse;
import com.arima.pms.web.dto.LowStockAlertResponse;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

  private final BatchRepository batchRepository;
  private final StockMovementRepository stockMovementRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public Page<Batch> listBatches(String search, UUID productId, UUID supplierId, BatchStatus status, LocalDate expiringBefore, Boolean lowStockOnly, Pageable pageable) {
    Specification<Batch> specification = (root, query, cb) -> cb.conjunction();
    if (StringUtils.hasText(search)) {
      String term = search.trim().toLowerCase(Locale.ROOT);
      specification = specification.and((root, query, cb) -> cb.or(
          cb.like(cb.lower(root.get("batchNumber")), "%" + term + "%"),
          cb.like(cb.lower(root.join("product").get("name")), "%" + term + "%"),
          cb.like(cb.lower(root.join("product").get("skuBarcode")), "%" + term + "%"),
          cb.like(cb.lower(root.join("supplier").get("name")), "%" + term + "%")
      ));
    }
    if (productId != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.join("product").get("id"), productId));
    }
    if (supplierId != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.join("supplier").get("id"), supplierId));
    }
    if (status != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (expiringBefore != null) {
      specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("expiryDate"), expiringBefore));
    }
    if (Boolean.TRUE.equals(lowStockOnly)) {
      specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("availableQuantity"), root.join("product").get("reorderLevel")));
    }
    Page<Batch> page = batchRepository.findAll(specification, pageable);
    page.forEach(InventoryService::hydrateBatch);
    return page;
  }

  @Transactional(readOnly = true)
  public Batch getBatch(UUID id) {
    Batch batch = loadBatch(id);
    hydrateBatch(batch);
    return batch;
  }

  @Transactional(readOnly = true)
  public Page<StockMovement> listStockMovements(UUID productId, UUID batchId, StockMovementType type, Pageable pageable) {
    Specification<StockMovement> specification = (root, query, cb) -> cb.conjunction();
    if (productId != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.join("product").get("id"), productId));
    }
    if (batchId != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.join("batch").get("id"), batchId));
    }
    if (type != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.get("type"), type));
    }
    Page<StockMovement> page = stockMovementRepository.findAll(specification, pageable);
    page.forEach(InventoryService::hydrateMovement);
    return page;
  }

  @Transactional(readOnly = true)
  public StockMovement getStockMovement(UUID id) {
    if (id == null) {
      throw new InvalidInventoryException("Stock movement id is required");
    }
    StockMovement movement = stockMovementRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Stock movement not found: " + id));
    hydrateMovement(movement);
    return movement;
  }

  public Batch adjustBatchStock(UUID batchId, AdjustBatchStockCommand command) {
    Batch batch = loadBatch(batchId);
    User user = loadActiveUser(command.performedByUserId());
    int delta = command.quantityChange();
    if (delta == 0) {
      throw new InvalidInventoryException("Quantity change must not be zero");
    }
    ensureBatchMutable(batch);
    applyDelta(batch, delta);
    StockMovement movement = StockMovement.adjustment(batch, user, UUID.randomUUID(), buildReason(command.reason(), "Inventory adjustment"), delta);
    batchRepository.save(batch);
    stockMovementRepository.save(movement);
    hydrateBatch(batch);
    return batch;
  }

  public Batch returnBatchStock(UUID batchId, ReturnBatchStockCommand command) {
    Batch batch = loadBatch(batchId);
    User user = loadActiveUser(command.performedByUserId());
    ensureBatchMutable(batch);
    int delta = command.quantity();
    applyDelta(batch, delta);
    StockMovement movement = StockMovement.returned(batch, user, UUID.randomUUID(), buildReason(command.reason(), "Inventory return"), delta);
    batchRepository.save(batch);
    stockMovementRepository.save(movement);
    hydrateBatch(batch);
    return batch;
  }

  public Batch writeOffBatchStock(UUID batchId, WriteOffBatchStockCommand command) {
    Batch batch = loadBatch(batchId);
    User user = loadActiveUser(command.performedByUserId());
    ensureBatchMutable(batch);
    int delta = -command.quantity();
    if (batch.getAvailableQuantity() < command.quantity()) {
      throw new InvalidInventoryException("Write-off quantity exceeds available stock");
    }
    applyDelta(batch, delta);
    StockMovement movement = StockMovement.writeOff(batch, user, UUID.randomUUID(), buildReason(command.reason(), "Inventory write-off"), delta);
    batchRepository.save(batch);
    stockMovementRepository.save(movement);
    hydrateBatch(batch);
    return batch;
  }

  @Transactional(readOnly = true)
  public List<LowStockAlertResponse> lowStockAlerts() {
    Map<UUID, List<Batch>> batchesByProduct = batchRepository.findAll().stream()
        .filter(batch -> batch.getProduct() != null)
        .filter(batch -> batch.lifecycleStatus() != BatchStatus.VOIDED)
        .collect(Collectors.groupingBy(batch -> batch.getProduct().getId()));

    return productRepository.findAll().stream()
        .filter(Product::isActive)
        .map(product -> lowStockAlertFor(product, batchesByProduct.getOrDefault(product.getId(), List.of())))
        .filter(alert -> alert.totalAvailableQuantity() <= alert.reorderLevel())
        .sorted(Comparator.comparingInt(LowStockAlertResponse::shortageQuantity).reversed())
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ExpiryAlertResponse> expiringAlerts(int days) {
    if (days < 0) {
      throw new InvalidInventoryException("Days must be zero or positive");
    }
    LocalDate cutoff = LocalDate.now().plusDays(days);
    return batchRepository.findAll().stream()
        .filter(batch -> batch.getExpiryDate() != null)
        .filter(batch -> !batch.getExpiryDate().isAfter(cutoff))
        .filter(batch -> batch.getAvailableQuantity() > 0)
        .map(batch -> new ExpiryAlertResponse(
            batch.getId(),
            batch.getProduct() != null ? batch.getProduct().getId() : null,
            batch.getProduct() != null ? batch.getProduct().getName() : null,
            batch.getProduct() != null ? batch.getProduct().getSkuBarcode() : null,
            batch.getBatchNumber(),
            batch.getExpiryDate(),
            ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate()),
            batch.getAvailableQuantity(),
            batch.lifecycleStatus()
        ))
        .sorted(Comparator.comparing(ExpiryAlertResponse::expiryDate))
        .toList();
  }

  private Batch loadBatch(UUID id) {
    if (id == null) {
      throw new InvalidInventoryException("Batch id is required");
    }
    return batchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + id));
  }

  private User loadActiveUser(UUID userId) {
    if (userId == null) {
      throw new InvalidInventoryException("User id is required");
    }
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    if (!user.isActive()) {
      throw new InvalidInventoryException("User is inactive: " + user.getId());
    }
    return user;
  }

  private static void ensureBatchMutable(Batch batch) {
    if (batch.lifecycleStatus() == BatchStatus.VOIDED) {
      throw new InvalidInventoryException("Voided batches cannot be adjusted");
    }
  }

  private static void applyDelta(Batch batch, int delta) {
    try {
      batch.adjustAvailableQuantity(delta);
    } catch (IllegalArgumentException ex) {
      throw new InvalidInventoryException(ex.getMessage());
    }
  }

  private static String buildReason(String reason, String fallback) {
    if (StringUtils.hasText(reason)) {
      return reason.trim();
    }
    return fallback;
  }

  private static void hydrateBatch(Batch batch) {
    if (batch == null) {
      return;
    }
    if (batch.getProduct() != null) {
      batch.getProduct().getName();
      batch.getProduct().getSkuBarcode();
      batch.getProduct().getReorderLevel();
    }
    if (batch.getSupplier() != null) {
      batch.getSupplier().getName();
    }
    if (batch.getPurchaseOrderItem() != null) {
      batch.getPurchaseOrderItem().getId();
    }
    if (batch.getGoodsReceipt() != null) {
      batch.getGoodsReceipt().getId();
    }
  }

  private static void hydrateMovement(StockMovement movement) {
    if (movement == null) {
      return;
    }
    if (movement.getProduct() != null) {
      movement.getProduct().getName();
      movement.getProduct().getSkuBarcode();
    }
    if (movement.getBatch() != null) {
      movement.getBatch().getBatchNumber();
    }
    if (movement.getCreatedBy() != null) {
      movement.getCreatedBy().getFullName();
    }
  }

  private static LowStockAlertResponse lowStockAlertFor(Product product, List<Batch> batches) {
    int totalAvailable = batches.stream().mapToInt(Batch::getAvailableQuantity).sum();
    int shortage = Math.max(0, product.getReorderLevel() - totalAvailable);
    int batchCount = batches.size();
    LocalDate earliestExpiry = batches.stream()
        .map(Batch::getExpiryDate)
        .filter(date -> date != null)
        .min(LocalDate::compareTo)
        .orElse(null);
    return new LowStockAlertResponse(
        product.getId(),
        product.getName(),
        product.getSkuBarcode(),
        product.getReorderLevel(),
        totalAvailable,
        shortage,
        batchCount,
        earliestExpiry
    );
  }
}
