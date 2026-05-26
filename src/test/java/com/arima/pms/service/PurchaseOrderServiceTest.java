package com.arima.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

  @Mock
  private PurchaseOrderRepository purchaseOrderRepository;

  @Mock
  private PurchaseOrderItemRepository purchaseOrderItemRepository;

  @Mock
  private SupplierRepository supplierRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProductRepository productRepository;

  @InjectMocks
  private PurchaseOrderService purchaseOrderService;

  @Test
  void createDraftPurchaseOrder_persistsHeaderAndLines() {
    UUID supplierId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID productAId = UUID.randomUUID();
    UUID productBId = UUID.randomUUID();

    Supplier supplier = supplier(supplierId, true);
    User creator = user(userId, true);
    Product productA = product(productAId, true);
    Product productB = product(productBId, true);

    when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(supplier));
    when(userRepository.findById(userId)).thenReturn(Optional.of(creator));
    when(productRepository.findById(productAId)).thenReturn(Optional.of(productA));
    when(productRepository.findById(productBId)).thenReturn(Optional.of(productB));
    when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(purchaseOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    CreatePurchaseOrderCommand command = new CreatePurchaseOrderCommand(
        supplierId,
        userId,
        LocalDate.of(2026, 6, 15),
        List.of(
            new PurchaseOrderLineCommand(productAId, 2, new BigDecimal("10.50")),
            new PurchaseOrderLineCommand(productBId, 3, new BigDecimal("11.25"))
        )
    );

    PurchaseOrder purchaseOrder = purchaseOrderService.createDraftPurchaseOrder(command);

    assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
    assertThat(purchaseOrder.getPoNumber()).startsWith("PO-");
    assertThat(purchaseOrder.getSupplier()).isSameAs(supplier);
    assertThat(purchaseOrder.getCreatedBy()).isSameAs(creator);
    assertThat(purchaseOrder.getExpectedDeliveryDate()).isEqualTo(LocalDate.of(2026, 6, 15));
    assertThat(purchaseOrder.getTotalEstimatedCost()).isEqualByComparingTo("54.75");

    ArgumentCaptor<PurchaseOrder> orderCaptor = ArgumentCaptor.forClass(PurchaseOrder.class);
    verify(purchaseOrderRepository).save(orderCaptor.capture());
    assertThat(orderCaptor.getValue().getPoNumber()).startsWith("PO-");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PurchaseOrderItem>> itemsCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(purchaseOrderItemRepository).saveAll(itemsCaptor.capture());
    List<PurchaseOrderItem> savedItems = itemsCaptor.getValue();
    assertThat(savedItems).hasSize(2);
    assertThat(savedItems.get(0).getPurchaseOrder()).isSameAs(purchaseOrder);
    assertThat(savedItems.get(0).getProduct()).isSameAs(productA);
    assertThat(savedItems.get(0).getOrderedQuantity()).isEqualTo(2);
    assertThat(savedItems.get(0).getReceivedQuantity()).isZero();
    assertThat(savedItems.get(0).getUnitCost()).isEqualByComparingTo("10.50");
  }

  @Test
  void createDraftPurchaseOrder_rejectsDuplicateProducts() {
    UUID supplierId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();

    when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(supplier(supplierId, true)));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, true)));
    when(productRepository.findById(productId)).thenReturn(Optional.of(product(productId, true)));

    CreatePurchaseOrderCommand command = new CreatePurchaseOrderCommand(
        supplierId,
        userId,
        null,
        List.of(
            new PurchaseOrderLineCommand(productId, 1, new BigDecimal("2.00")),
            new PurchaseOrderLineCommand(productId, 2, new BigDecimal("3.00"))
        )
    );

    InvalidPurchaseOrderException exception = assertThrows(
        InvalidPurchaseOrderException.class,
        () -> purchaseOrderService.createDraftPurchaseOrder(command)
    );

    assertThat(exception.getMessage()).contains("Duplicate product");
  }

  @Test
  void createDraftPurchaseOrder_rejectsEmptyLineList() {
    UUID supplierId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    CreatePurchaseOrderCommand command = new CreatePurchaseOrderCommand(
        supplierId,
        userId,
        null,
        List.of()
    );

    InvalidPurchaseOrderException exception = assertThrows(
        InvalidPurchaseOrderException.class,
        () -> purchaseOrderService.createDraftPurchaseOrder(command)
    );

    assertThat(exception.getMessage()).contains("at least one line");
  }

  private static Supplier supplier(UUID id, boolean active) {
    Supplier supplier = new Supplier();
    supplier.setId(id);
    supplier.setName("Acme Pharma");
    supplier.setActive(active);
    return supplier;
  }

  private static User user(UUID id, boolean active) {
    User user = new User();
    user.setId(id);
    user.setUsername("arima");
    user.setFullName("Arima");
    user.setEmail("arima@example.com");
    user.setPasswordHash("hash");
    user.setActive(active);
    return user;
  }

  private static Product product(UUID id, boolean active) {
    Product product = new Product();
    product.setId(id);
    product.setSkuBarcode("SKU-001");
    product.setName("Paracetamol 500mg");
    product.setActive(active);
    return product;
  }
}
