package com.arima.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arima.pms.domain.entity.Batch;
import com.arima.pms.domain.entity.Customer;
import com.arima.pms.domain.entity.Prescription;
import com.arima.pms.domain.entity.Product;
import com.arima.pms.domain.entity.Sale;
import com.arima.pms.domain.entity.SaleItem;
import com.arima.pms.domain.entity.StockMovement;
import com.arima.pms.domain.entity.Supplier;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

  @Mock private SaleRepository saleRepository;
  @Mock private ProductRepository productRepository;
  @Mock private BatchRepository batchRepository;
  @Mock private CustomerRepository customerRepository;
  @Mock private PrescriptionRepository prescriptionRepository;
  @Mock private UserRepository userRepository;
  @Mock private StockMovementRepository stockMovementRepository;

  @InjectMocks private SaleService saleService;

  @Test
  void createDraftSale_allocatesFefoBatchesAndCreatesDraft() {
    UUID createdById = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();
    UUID batch1Id = UUID.randomUUID();
    UUID batch2Id = UUID.randomUUID();

    User createdBy = user(createdById);
    Customer customer = customer(customerId);
    Product product = product(productId, "SKU-A", false, true);
    Batch batch1 = batch(batch1Id, product, 5, 5, LocalDate.now().plusDays(10), LocalDateTime.now().minusDays(10), BatchStatus.AVAILABLE, new BigDecimal("12.00"));
    Batch batch2 = batch(batch2Id, product, 4, 4, LocalDate.now().plusDays(30), LocalDateTime.now().minusDays(2), BatchStatus.AVAILABLE, new BigDecimal("13.00"));

    when(userRepository.findById(createdById)).thenReturn(Optional.of(createdBy));
    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
    when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    when(batchRepository.findAll(any(Specification.class))).thenReturn(List.of(batch2, batch1));
    when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
      Sale sale = invocation.getArgument(0);
      sale.setId(UUID.randomUUID());
      sale.getItems().forEach(item -> item.setId(UUID.randomUUID()));
      return sale;
    });

    Sale sale = saleService.createDraftSale(new CreateSaleCommand(
        createdById,
        customerId,
        null,
        new BigDecimal("1.50"),
        new BigDecimal("0.50"),
        List.of(new CreateSaleLineCommand(productId, 7))
    ));

    assertThat(sale.getStatus()).isEqualTo(SaleStatus.DRAFT);
    assertThat(sale.getItems()).hasSize(2);
    assertThat(sale.getItems().get(0).getBatch().getId()).isEqualTo(batch1Id);
    assertThat(sale.getItems().get(0).getQuantity()).isEqualTo(5);
    assertThat(sale.getItems().get(1).getBatch().getId()).isEqualTo(batch2Id);
    assertThat(sale.getItems().get(1).getQuantity()).isEqualTo(2);
    assertThat(sale.getSubtotal()).isEqualByComparingTo("86.00");
    assertThat(sale.getTotal()).isEqualByComparingTo("85.00");
  }

  @Test
  void updateDraftSale_rebuildsItems() {
    Sale sale = draftSale();
    UUID productId = sale.getItems().get(0).getProduct().getId();
    UUID batchId = sale.getItems().get(0).getBatch().getId();

    when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
    when(customerRepository.findById(sale.getCustomer().getId())).thenReturn(Optional.of(sale.getCustomer()));
    when(productRepository.findById(productId)).thenReturn(Optional.of(sale.getItems().get(0).getProduct()));
    when(batchRepository.findAll(any(Specification.class))).thenReturn(List.of(sale.getItems().get(0).getBatch()));
    when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Sale updated = saleService.updateDraftSale(sale.getId(), new UpdateSaleCommand(null, null, new BigDecimal("2.00"), new BigDecimal("1.00"), List.of(new CreateSaleLineCommand(productId, 2))));

    assertThat(updated.getItems()).hasSize(1);
    assertThat(updated.getItems().get(0).getBatch().getId()).isEqualTo(batchId);
    assertThat(updated.getTotal()).isEqualByComparingTo("19.00");
  }

  @Test
  void confirmSale_deductsStockAndCreatesStockMovement() {
    Sale sale = confirmedDraft(false);
    Batch batch = sale.getItems().get(0).getBatch();
    User actor = user(UUID.randomUUID());

    when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
    when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
    when(batchRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    when(stockMovementRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Sale confirmed = saleService.confirmSale(sale.getId(), new SaleConfirmCommand(actor.getId(), PaymentStatus.PAID));

    assertThat(confirmed.getStatus()).isEqualTo(SaleStatus.CONFIRMED);
    assertThat(confirmed.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(batch.getAvailableQuantity()).isEqualTo(1);
    ArgumentCaptor<List<StockMovement>> movementsCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(stockMovementRepository).saveAll(movementsCaptor.capture());
    assertThat(movementsCaptor.getValue()).hasSize(1);
    assertThat(movementsCaptor.getValue().get(0).getType()).isEqualTo(com.arima.pms.domain.enums.StockMovementType.SALE);
  }

  @Test
  void cancelSale_marksDraftAsCancelled() {
    Sale sale = draftSale();
    when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
    when(userRepository.findById(any())).thenReturn(Optional.of(user(UUID.randomUUID())));
    when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Sale cancelled = saleService.cancelSale(sale.getId(), new SaleCancelCommand(UUID.randomUUID(), "changed mind"));

    assertThat(cancelled.getStatus()).isEqualTo(SaleStatus.CANCELLED);
    assertThat(cancelled.getPaymentStatus()).isEqualTo(PaymentStatus.VOIDED);
  }

  @Test
  void voidSale_restoresStockAndCreatesReversalMovement() {
    Sale sale = confirmedDraft(true);
    sale.setStatus(SaleStatus.CONFIRMED);
    Batch batch = sale.getItems().get(0).getBatch();
    batch.adjustAvailableQuantity(-2);
    User actor = user(UUID.randomUUID());

    when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
    when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
    when(batchRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    when(stockMovementRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Sale voided = saleService.voidSale(sale.getId(), new SaleVoidCommand(actor.getId(), "customer request"));

    assertThat(voided.getStatus()).isEqualTo(SaleStatus.VOIDED);
    assertThat(voided.getPaymentStatus()).isEqualTo(PaymentStatus.VOIDED);
    assertThat(batch.getAvailableQuantity()).isEqualTo(3);
  }

  @Test
  void confirmSale_rejectsMissingPrescriptionForRequiredProduct() {
    Sale sale = draftSaleRequiredPrescription();
    when(saleRepository.findById(sale.getId())).thenReturn(Optional.of(sale));
    when(userRepository.findById(any())).thenReturn(Optional.of(user(UUID.randomUUID())));

    InvalidSaleException exception = assertThrows(InvalidSaleException.class,
        () -> saleService.confirmSale(sale.getId(), new SaleConfirmCommand(UUID.randomUUID(), PaymentStatus.UNPAID)));

    assertThat(exception.getMessage()).contains("Prescription is required");
  }

  @Test
  void list_supportsFiltering() {
    Sale sale = draftSale();
    when(saleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(sale)));

    assertThat(saleService.list(sale.getInvoiceNumber(), sale.getCustomer().getId(), null, SaleStatus.DRAFT, PaymentStatus.UNPAID, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), PageRequest.of(0, 20))).hasSize(1);
  }

  private Sale draftSale() {
    Sale sale = new Sale();
    sale.setId(UUID.randomUUID());
    sale.setInvoiceNumber("INV-20260527-AAAA1111");
    sale.setCreatedBy(user(UUID.randomUUID()));
    sale.setStatus(SaleStatus.DRAFT);
    sale.setPaymentStatus(PaymentStatus.UNPAID);
    sale.setCustomer(customer(UUID.randomUUID()));
    Product product = product(UUID.randomUUID(), "SKU-A", false, true);
    Batch batch = batch(UUID.randomUUID(), product, 3, 3, LocalDate.now().plusDays(10), LocalDateTime.now().minusDays(1), BatchStatus.AVAILABLE, new BigDecimal("10.00"));
    sale.addItem(product, batch, 2, new BigDecimal("10.00"), BigDecimal.ZERO);
    sale.applyFinancials(new BigDecimal("1.00"), new BigDecimal("0.50"));
    return sale;
  }

  private Sale confirmedDraft(boolean withCustomerAndPrescription) {
    Sale sale = draftSale();
    sale.setStatus(SaleStatus.DRAFT);
    if (withCustomerAndPrescription) {
      Customer customer = sale.getCustomer();
      Prescription prescription = new Prescription();
      prescription.setId(UUID.randomUUID());
      prescription.setCustomer(customer);
      prescription.setDoctorName("Dr. A");
      prescription.setIssueDate(LocalDate.now());
      prescription.setExpiryDate(LocalDate.now().plusDays(10));
      prescription.setVerificationStatus(VerificationStatus.VERIFIED);
      sale.setPrescription(prescription);
    }
    return sale;
  }

  private Sale draftSaleRequiredPrescription() {
    Sale sale = new Sale();
    sale.setId(UUID.randomUUID());
    sale.setInvoiceNumber("INV-20260527-REQPRSC");
    sale.setCreatedBy(user(UUID.randomUUID()));
    sale.setStatus(SaleStatus.DRAFT);
    sale.setPaymentStatus(PaymentStatus.UNPAID);
    sale.setCustomer(customer(UUID.randomUUID()));
    Product product = product(UUID.randomUUID(), "SKU-RX", true, true);
    Batch batch = batch(UUID.randomUUID(), product, 5, 5, LocalDate.now().plusDays(10), LocalDateTime.now().minusDays(1), BatchStatus.AVAILABLE, new BigDecimal("20.00"));
    sale.addItem(product, batch, 1, new BigDecimal("20.00"), BigDecimal.ZERO);
    sale.applyFinancials(BigDecimal.ZERO, BigDecimal.ZERO);
    return sale;
  }

  private static Batch batch(UUID id, Product product, int receivedQuantity, int availableQuantity, LocalDate expiryDate, LocalDateTime receivedAt, BatchStatus status, BigDecimal sellingPrice) {
    Supplier supplier = new Supplier();
    supplier.setId(UUID.randomUUID());
    supplier.setName("Acme Pharma");

    Batch batch = new Batch();
    batch.setId(id);
    batch.setProduct(product);
    batch.setSupplier(supplier);
    batch.setBatchNumber("BATCH-" + id.toString().substring(0, 6));
    batch.setReceivedQuantity(receivedQuantity);
    batch.setAvailableQuantity(availableQuantity);
    batch.setExpiryDate(expiryDate);
    batch.setReceivedAt(receivedAt);
    batch.setStatus(status);
    batch.setPurchaseCost(new BigDecimal("7.50"));
    batch.setSellingPrice(sellingPrice);
    return batch;
  }

  private static Product product(UUID id, String sku, boolean prescriptionRequired, boolean active) {
    Product product = new Product();
    product.setId(id);
    product.setSkuBarcode(sku);
    product.setName("Medicine " + sku);
    product.setPrescriptionRequired(prescriptionRequired);
    product.setActive(active);
    product.setReorderLevel(5);
    return product;
  }

  private static Customer customer(UUID id) {
    Customer customer = new Customer();
    customer.setId(id);
    customer.setName("Customer " + id.toString().substring(0, 6));
    return customer;
  }

  private static User user(UUID id) {
    User user = new User();
    user.setId(id);
    user.setUsername("smoke.user");
    user.setFullName("Smoke User");
    user.setEmail("smoke@example.com");
    user.setPasswordHash("hash");
    user.setActive(true);
    return user;
  }
}
