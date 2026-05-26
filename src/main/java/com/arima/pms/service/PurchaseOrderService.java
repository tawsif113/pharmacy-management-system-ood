package com.arima.pms.service;

import com.arima.pms.domain.entity.Product;
import com.arima.pms.domain.entity.PurchaseOrder;
import com.arima.pms.domain.entity.PurchaseOrderItem;
import com.arima.pms.domain.entity.Supplier;
import com.arima.pms.domain.entity.User;
import com.arima.pms.domain.enums.PurchaseOrderStatus;
import com.arima.pms.repository.ProductRepository;
import com.arima.pms.repository.PurchaseOrderItemRepository;
import com.arima.pms.repository.PurchaseOrderRepository;
import com.arima.pms.repository.SupplierRepository;
import com.arima.pms.repository.UserRepository;
import com.arima.pms.service.command.CreatePurchaseOrderCommand;
import com.arima.pms.service.command.PurchaseOrderLineCommand;
import com.arima.pms.service.exception.InvalidPurchaseOrderException;
import com.arima.pms.service.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {

  private static final DateTimeFormatter PO_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

  private final PurchaseOrderRepository purchaseOrderRepository;
  private final PurchaseOrderItemRepository purchaseOrderItemRepository;
  private final SupplierRepository supplierRepository;
  private final UserRepository userRepository;
  private final ProductRepository productRepository;

  public PurchaseOrder createDraftPurchaseOrder(CreatePurchaseOrderCommand command) {
    if (command == null) {
      throw new InvalidPurchaseOrderException("Purchase order command is required");
    }
    if (CollectionUtils.isEmpty(command.lines())) {
      throw new InvalidPurchaseOrderException("Purchase order must contain at least one line");
    }

    Supplier supplier = supplierRepository.findById(command.supplierId())
        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + command.supplierId()));
    if (!supplier.isActive()) {
      throw new InvalidPurchaseOrderException("Supplier is inactive: " + supplier.getId());
    }

    User createdBy = userRepository.findById(command.createdByUserId())
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + command.createdByUserId()));
    if (!createdBy.isActive()) {
      throw new InvalidPurchaseOrderException("User is inactive: " + createdBy.getId());
    }

    PurchaseOrder purchaseOrder = new PurchaseOrder();
    purchaseOrder.setPoNumber(generatePoNumber());
    purchaseOrder.setSupplier(supplier);
    purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT);
    purchaseOrder.setExpectedDeliveryDate(command.expectedDeliveryDate());
    purchaseOrder.setCreatedBy(createdBy);

    Set<UUID> seenProductIds = new HashSet<>();
    List<PurchaseOrderItem> items = new ArrayList<>();
    BigDecimal totalEstimatedCost = BigDecimal.ZERO;

    for (PurchaseOrderLineCommand line : command.lines()) {
      validateLine(line);

      Product product = productRepository.findById(line.productId())
          .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.productId()));
      if (!product.isActive()) {
        throw new InvalidPurchaseOrderException("Product is inactive: " + product.getId());
      }
      if (!seenProductIds.add(product.getId())) {
        throw new InvalidPurchaseOrderException("Duplicate product in purchase order: " + product.getId());
      }

      PurchaseOrderItem item = new PurchaseOrderItem();
      item.setPurchaseOrder(purchaseOrder);
      item.setProduct(product);
      item.setOrderedQuantity(line.orderedQuantity());
      item.setReceivedQuantity(0);
      item.setUnitCost(line.unitCost());
      items.add(item);

      totalEstimatedCost = totalEstimatedCost.add(
          line.unitCost().multiply(BigDecimal.valueOf(line.orderedQuantity()))
      );
    }

    purchaseOrder.setTotalEstimatedCost(totalEstimatedCost);
    PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);
    purchaseOrderItemRepository.saveAll(items);
    return savedPurchaseOrder;
  }

  private static void validateLine(PurchaseOrderLineCommand line) {
    if (line == null) {
      throw new InvalidPurchaseOrderException("Purchase order line is required");
    }
    if (line.productId() == null) {
      throw new InvalidPurchaseOrderException("Purchase order line must reference a product");
    }
    if (line.unitCost() == null) {
      throw new InvalidPurchaseOrderException("Unit cost is required");
    }
    if (line.unitCost().signum() < 0) {
      throw new InvalidPurchaseOrderException("Unit cost cannot be negative");
    }
  }

  private static String generatePoNumber() {
    String datePart = java.time.LocalDate.now().format(PO_DATE_FORMAT);
    String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    return "PO-" + datePart + "-" + suffix;
  }
}
