package com.arima.pms.service;

import com.arima.pms.domain.entity.Batch;
import com.arima.pms.domain.entity.Customer;
import com.arima.pms.domain.entity.Prescription;
import com.arima.pms.domain.entity.Product;
import com.arima.pms.domain.entity.Sale;
import com.arima.pms.domain.entity.SaleItem;
import com.arima.pms.domain.entity.StockMovement;
import com.arima.pms.domain.entity.User;
import com.arima.pms.domain.enums.BatchStatus;
import com.arima.pms.domain.enums.PaymentStatus;
import com.arima.pms.domain.enums.SaleStatus;
import com.arima.pms.domain.enums.VerificationStatus;
import com.arima.pms.repository.BatchRepository;
import com.arima.pms.repository.CustomerRepository;
import com.arima.pms.repository.PrescriptionRepository;
import com.arima.pms.repository.ProductRepository;
import com.arima.pms.repository.SaleRepository;
import com.arima.pms.repository.StockMovementRepository;
import com.arima.pms.repository.UserRepository;
import com.arima.pms.service.command.CreateSaleCommand;
import com.arima.pms.service.command.CreateSaleLineCommand;
import com.arima.pms.service.command.SaleCancelCommand;
import com.arima.pms.service.command.SaleConfirmCommand;
import com.arima.pms.service.command.SaleVoidCommand;
import com.arima.pms.service.command.UpdateSaleCommand;
import com.arima.pms.service.exception.InvalidSaleException;
import com.arima.pms.service.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
public class SaleService {

  private final SaleRepository saleRepository;
  private final ProductRepository productRepository;
  private final BatchRepository batchRepository;
  private final CustomerRepository customerRepository;
  private final PrescriptionRepository prescriptionRepository;
  private final UserRepository userRepository;
  private final StockMovementRepository stockMovementRepository;

  public Sale createDraftSale(CreateSaleCommand command) {
    if (command == null) {
      throw new InvalidSaleException("Sale command is required");
    }
    if (CollectionUtils.isEmpty(command.items())) {
      throw new InvalidSaleException("Sale must contain at least one item");
    }

    User createdBy = loadActiveUser(command.createdByUserId());
    Sale sale = new Sale();
    sale.setInvoiceNumber(generateInvoiceNumber());
    sale.setCreatedBy(createdBy);
    sale.setStatus(SaleStatus.DRAFT);
    sale.setPaymentStatus(PaymentStatus.UNPAID);

    resolveCustomerAndPrescription(sale, command.customerId(), command.prescriptionId());
    validateFinancials(command.discount(), command.tax());
    rebuildDraftItems(sale, command.items());
    sale.applyFinancials(defaultZero(command.discount()), defaultZero(command.tax()));

    Sale saved = saleRepository.save(sale);
    hydrateForResponse(saved);
    return saved;
  }

  public Sale updateDraftSale(UUID id, UpdateSaleCommand command) {
    Sale sale = loadSale(id);
    ensureDraft(sale);
    if (command == null) {
      throw new InvalidSaleException("Sale command is required");
    }

    UUID resolvedCustomerId = command.customerId() != null ? command.customerId() : (sale.getCustomer() != null ? sale.getCustomer().getId() : null);
    UUID resolvedPrescriptionId = command.prescriptionId() != null ? command.prescriptionId() : (sale.getPrescription() != null ? sale.getPrescription().getId() : null);
    resolveCustomerAndPrescription(sale, resolvedCustomerId, resolvedPrescriptionId);

    if (command.items() != null) {
      if (CollectionUtils.isEmpty(command.items())) {
        throw new InvalidSaleException("Sale must contain at least one item");
      }
      validateFinancials(command.discount(), command.tax());
      sale.clearItems();
      rebuildDraftItems(sale, command.items());
    }

    sale.applyFinancials(
        command.discount() != null ? command.discount() : sale.getDiscount(),
        command.tax() != null ? command.tax() : sale.getTax()
    );

    Sale saved = saleRepository.save(sale);
    hydrateForResponse(saved);
    return saved;
  }

  @Transactional(readOnly = true)
  public Sale get(UUID id) {
    Sale sale = loadSale(id);
    hydrateForResponse(sale);
    return sale;
  }

  @Transactional(readOnly = true)
  public Page<Sale> list(String invoiceNumber, UUID customerId, UUID prescriptionId, SaleStatus status, PaymentStatus paymentStatus, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
    Specification<Sale> specification = (root, query, cb) -> cb.conjunction();
    if (StringUtils.hasText(invoiceNumber)) {
      String term = invoiceNumber.trim().toLowerCase(Locale.ROOT);
      specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("invoiceNumber")), "%" + term + "%"));
    }
    if (customerId != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.join("customer").get("id"), customerId));
    }
    if (prescriptionId != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.join("prescription").get("id"), prescriptionId));
    }
    if (status != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (paymentStatus != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.get("paymentStatus"), paymentStatus));
    }
    if (dateFrom != null) {
      specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay()));
    }
    if (dateTo != null) {
      specification = specification.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), dateTo.plusDays(1).atStartOfDay()));
    }
    Page<Sale> page = saleRepository.findAll(specification, pageable);
    page.forEach(SaleService::hydrateForResponse);
    return page;
  }

  public Sale confirmSale(UUID id, SaleConfirmCommand command) {
    Sale sale = loadSale(id);
    ensureDraft(sale);
    User performedBy = loadActiveUser(command.performedByUserId());

    validatePrescriptionRules(sale);
    validateAllocatedItems(sale);

    List<Batch> batchesToSave = new ArrayList<>();
    List<StockMovement> movements = new ArrayList<>();
    for (SaleItem item : sale.getItems()) {
      Batch batch = item.getBatch();
      int quantity = item.getQuantity();
      if (batch.getAvailableQuantity() < quantity) {
        throw new InvalidSaleException("Insufficient stock for batch " + batch.getBatchNumber());
      }
      batch.adjustAvailableQuantity(-quantity);
      batchesToSave.add(batch);
      movements.add(StockMovement.sale(batch, performedBy, sale.getId(), buildReason("Sale confirmation", sale.getInvoiceNumber()), -quantity));
    }

    sale.markConfirmed();
    sale.setPaymentStatus(command.paymentStatus() != null ? command.paymentStatus() : sale.getPaymentStatus());
    sale.applyFinancials(sale.getDiscount(), sale.getTax());

    batchRepository.saveAll(batchesToSave);
    stockMovementRepository.saveAll(movements);
    Sale saved = saleRepository.save(sale);
    hydrateForResponse(saved);
    return saved;
  }

  public Sale cancelSale(UUID id, SaleCancelCommand command) {
    Sale sale = loadSale(id);
    ensureDraft(sale);
    loadActiveUser(command.performedByUserId());
    sale.markCancelled();
    sale.setPaymentStatus(PaymentStatus.VOIDED);
    Sale saved = saleRepository.save(sale);
    hydrateForResponse(saved);
    return saved;
  }

  public Sale voidSale(UUID id, SaleVoidCommand command) {
    Sale sale = loadSale(id);
    ensureConfirmed(sale);
    User performedBy = loadActiveUser(command.performedByUserId());

    List<Batch> batchesToSave = new ArrayList<>();
    List<StockMovement> movements = new ArrayList<>();
    for (SaleItem item : sale.getItems()) {
      Batch batch = item.getBatch();
      int quantity = item.getQuantity();
      if (batch.getAvailableQuantity() + quantity > batch.getReceivedQuantity()) {
        throw new InvalidSaleException("Void would exceed received quantity for batch " + batch.getBatchNumber());
      }
      batch.adjustAvailableQuantity(quantity);
      batchesToSave.add(batch);
      movements.add(StockMovement.cancelled(batch, performedBy, sale.getId(), buildReason(command.reason(), "Sale void"), quantity));
    }

    sale.markVoided();
    sale.setPaymentStatus(PaymentStatus.VOIDED);
    sale.applyFinancials(sale.getDiscount(), sale.getTax());

    batchRepository.saveAll(batchesToSave);
    stockMovementRepository.saveAll(movements);
    Sale saved = saleRepository.save(sale);
    hydrateForResponse(saved);
    return saved;
  }

  private void rebuildDraftItems(Sale sale, List<CreateSaleLineCommand> lines) {
    Set<UUID> seenProductIds = new HashSet<>();
    for (CreateSaleLineCommand line : lines) {
      validateLine(line);
      Product product = productRepository.findById(line.productId())
          .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.productId()));
      if (!product.isActive()) {
        throw new InvalidSaleException("Product is inactive: " + product.getId());
      }
      if (!seenProductIds.add(product.getId())) {
        throw new InvalidSaleException("Duplicate product in sale: " + product.getId());
      }

      List<Batch> eligibleBatches = findEligibleBatches(product);
      int remaining = line.quantity();
      for (Batch batch : eligibleBatches) {
        if (remaining <= 0) {
          break;
        }
        int allocate = Math.min(remaining, batch.getAvailableQuantity());
        sale.addItem(product, batch, allocate, batch.getSellingPrice(), BigDecimal.ZERO);
        remaining -= allocate;
      }
      if (remaining > 0) {
        throw new InvalidSaleException("Insufficient stock for product: " + product.getSkuBarcode());
      }
    }
    sale.recalculateSubtotal();
  }

  private List<Batch> findEligibleBatches(Product product) {
    Specification<Batch> specification = (root, query, cb) -> cb.equal(root.join("product").get("id"), product.getId());
    List<Batch> batches = batchRepository.findAll(specification).stream()
        .filter(batch -> batch.getAvailableQuantity() > 0)
        .filter(batch -> batch.getExpiryDate() == null || !batch.getExpiryDate().isBefore(LocalDate.now()))
        .filter(batch -> batch.lifecycleStatus() != BatchStatus.VOIDED)
        .sorted(Comparator
            .comparing(Batch::getExpiryDate)
            .thenComparing(Batch::getReceivedAt)
            .thenComparing(Batch::getId))
        .toList();
    if (batches.isEmpty()) {
      throw new InvalidSaleException("No eligible batches available for product: " + product.getSkuBarcode());
    }
    return batches;
  }

  private void validatePrescriptionRules(Sale sale) {
    boolean requiresPrescription = sale.getItems().stream()
        .anyMatch(item -> item.getProduct() != null && item.getProduct().isPrescriptionRequired());
    if (!requiresPrescription) {
      return;
    }
    if (sale.getPrescription() == null) {
      throw new InvalidSaleException("Prescription is required for one or more sale items");
    }
    if (sale.getCustomer() == null) {
      throw new InvalidSaleException("Customer is required when prescription validation is needed");
    }
    Prescription prescription = sale.getPrescription();
    if (prescription.getCustomer() == null || !sale.getCustomer().getId().equals(prescription.getCustomer().getId())) {
      throw new InvalidSaleException("Prescription customer does not match the sale customer");
    }
    if (prescription.getVerificationStatus() != VerificationStatus.VERIFIED) {
      throw new InvalidSaleException("Prescription must be verified before confirmation");
    }
    if (prescription.getExpiryDate() != null && prescription.getExpiryDate().isBefore(LocalDate.now())) {
      throw new InvalidSaleException("Prescription is expired");
    }
  }

  private void validateAllocatedItems(Sale sale) {
    for (SaleItem item : sale.getItems()) {
      if (item.getProduct() == null) {
        throw new InvalidSaleException("Sale item product is required");
      }
      if (item.getBatch() == null) {
        throw new InvalidSaleException("Sale item batch is required");
      }
      if (!item.getProduct().getId().equals(item.getBatch().getProduct().getId())) {
        throw new InvalidSaleException("Sale item batch does not belong to the product");
      }
      if (item.getBatch().lifecycleStatus() == BatchStatus.EXPIRED) {
        throw new InvalidSaleException("Expired batch cannot be sold: " + item.getBatch().getBatchNumber());
      }
      if (item.getBatch().lifecycleStatus() == BatchStatus.VOIDED) {
        throw new InvalidSaleException("Voided batch cannot be sold: " + item.getBatch().getBatchNumber());
      }
    }
  }

  private Sale loadSale(UUID id) {
    if (id == null) {
      throw new InvalidSaleException("Sale id is required");
    }
    Sale sale = saleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + id));
    hydrateForResponse(sale);
    return sale;
  }

  private void ensureDraft(Sale sale) {
    if (!sale.canConfirm()) {
      throw new InvalidSaleException("Sale can only be modified while in draft");
    }
  }

  private void ensureConfirmed(Sale sale) {
    if (!sale.canVoid()) {
      throw new InvalidSaleException("Only confirmed sales can be voided");
    }
  }

  private void resolveCustomerAndPrescription(Sale sale, UUID customerId, UUID prescriptionId) {
    Customer customer = null;
    Prescription prescription = null;
    if (customerId != null) {
      customer = customerRepository.findById(customerId)
          .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
    }
    if (prescriptionId != null) {
      prescription = prescriptionRepository.findById(prescriptionId)
          .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + prescriptionId));
      if (customer == null) {
        customer = prescription.getCustomer();
      } else if (prescription.getCustomer() != null && !customer.getId().equals(prescription.getCustomer().getId())) {
        throw new InvalidSaleException("Prescription customer does not match the sale customer");
      }
    }
    sale.setCustomer(customer);
    sale.setPrescription(prescription);
  }

  private User loadActiveUser(UUID userId) {
    if (userId == null) {
      throw new InvalidSaleException("User id is required");
    }
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    if (!user.isActive()) {
      throw new InvalidSaleException("User is inactive: " + user.getId());
    }
    return user;
  }

  private static void hydrateForResponse(Sale sale) {
    if (sale == null) {
      return;
    }
    if (sale.getCustomer() != null) {
      sale.getCustomer().getName();
    }
    if (sale.getPrescription() != null) {
      sale.getPrescription().getDoctorName();
      if (sale.getPrescription().getCustomer() != null) {
        sale.getPrescription().getCustomer().getName();
      }
    }
    if (sale.getCreatedBy() != null) {
      sale.getCreatedBy().getFullName();
    }
    sale.getItems().forEach(item -> {
      if (item.getProduct() != null) {
        item.getProduct().getName();
        item.getProduct().getSkuBarcode();
        item.getProduct().isPrescriptionRequired();
      }
      if (item.getBatch() != null) {
        item.getBatch().getBatchNumber();
      }
    });
  }

  private static void validateLine(CreateSaleLineCommand line) {
    if (line == null) {
      throw new InvalidSaleException("Sale line is required");
    }
    if (line.productId() == null) {
      throw new InvalidSaleException("Sale line must reference a product");
    }
    if (line.quantity() <= 0) {
      throw new InvalidSaleException("Sale line quantity must be positive");
    }
  }

  private static String generateInvoiceNumber() {
    String datePart = LocalDate.now().toString().replace("-", "");
    String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    return "INV-" + datePart + "-" + suffix;
  }

  private static String buildReason(String reason, String fallback) {
    if (StringUtils.hasText(reason)) {
      return reason.trim();
    }
    return fallback;
  }

  private static void validateFinancials(BigDecimal discount, BigDecimal tax) {
    if (discount != null && discount.signum() < 0) {
      throw new InvalidSaleException("Discount cannot be negative");
    }
    if (tax != null && tax.signum() < 0) {
      throw new InvalidSaleException("Tax cannot be negative");
    }
  }

  private static BigDecimal defaultZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
  }
}
