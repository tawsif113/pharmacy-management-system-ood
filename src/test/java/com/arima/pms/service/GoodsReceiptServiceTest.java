package com.arima.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arima.pms.domain.entity.Batch;
import com.arima.pms.domain.entity.GoodsReceipt;
import com.arima.pms.domain.entity.Product;
import com.arima.pms.domain.entity.PurchaseOrder;
import com.arima.pms.domain.entity.PurchaseOrderItem;
import com.arima.pms.domain.entity.StockMovement;
import com.arima.pms.domain.entity.Supplier;
import com.arima.pms.domain.entity.User;
import com.arima.pms.domain.enums.PurchaseOrderStatus;
import com.arima.pms.domain.enums.StockMovementType;
import com.arima.pms.repository.GoodsReceiptRepository;
import com.arima.pms.repository.PurchaseOrderRepository;
import com.arima.pms.repository.StockMovementRepository;
import com.arima.pms.repository.UserRepository;
import com.arima.pms.service.command.CreateGoodsReceiptCommand;
import com.arima.pms.service.command.GoodsReceiptLineCommand;
import com.arima.pms.service.exception.InvalidGoodsReceiptException;
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
class GoodsReceiptServiceTest {

  @Mock
  private GoodsReceiptRepository goodsReceiptRepository;

  @Mock
  private PurchaseOrderRepository purchaseOrderRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private StockMovementRepository stockMovementRepository;

  @InjectMocks
  private GoodsReceiptService goodsReceiptService;

  @Test
  void createGoodsReceipt_createsReceiptBatchesAndStockMovements() {
    UUID purchaseOrderId = UUID.randomUUID();
    UUID receivedById = UUID.randomUUID();
    UUID itemAId = UUID.randomUUID();
    UUID itemBId = UUID.randomUUID();
    LocalDateTime receivedAt = LocalDateTime.of(2026, 6, 16, 10, 30);

    PurchaseOrder purchaseOrder = purchaseOrder(PurchaseOrderStatus.APPROVED);
    purchaseOrder.setId(purchaseOrderId);

    User receivedBy = user(receivedById);

    PurchaseOrderItem itemA = item(purchaseOrder, itemAId, 5, 0, "SKU-A");
    PurchaseOrderItem itemB = item(purchaseOrder, itemBId, 3, 1, "SKU-B");
    purchaseOrder.getItems().add(itemA);
    purchaseOrder.getItems().add(itemB);

    when(purchaseOrderRepository.findById(purchaseOrderId)).thenReturn(Optional.of(purchaseOrder));
    when(userRepository.findById(receivedById)).thenReturn(Optional.of(receivedBy));
    when(goodsReceiptRepository.saveAndFlush(any(GoodsReceipt.class))).thenAnswer(invocation -> {
      GoodsReceipt receipt = invocation.getArgument(0);
      receipt.setId(UUID.randomUUID());
      return receipt;
    });
    when(stockMovementRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    CreateGoodsReceiptCommand command = new CreateGoodsReceiptCommand(
        purchaseOrderId,
        receivedById,
        receivedAt,
        "First delivery",
        List.of(
            new GoodsReceiptLineCommand(itemAId, 5, "BATCH-A", LocalDate.of(2027, 1, 1), new BigDecimal("7.50"), new BigDecimal("10.00")),
            new GoodsReceiptLineCommand(itemBId, 2, "BATCH-B", LocalDate.of(2027, 2, 1), new BigDecimal("6.00"), new BigDecimal("8.50"))
        )
    );

    GoodsReceipt receipt = goodsReceiptService.createGoodsReceipt(command);

    assertThat(receipt.getBatches()).hasSize(2);
    assertThat(receipt.getBatches().get(0).getBatchNumber()).isEqualTo("BATCH-A");
    assertThat(receipt.getBatches().get(0).getAvailableQuantity()).isEqualTo(5);
    assertThat(receipt.getBatches().get(0).getStatus()).isNotNull();
    assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
    assertThat(itemA.getReceivedQuantity()).isEqualTo(5);
    assertThat(itemB.getReceivedQuantity()).isEqualTo(3);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<StockMovement>> movementsCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(stockMovementRepository).saveAll(movementsCaptor.capture());
    assertThat(movementsCaptor.getValue()).hasSize(2);
    assertThat(movementsCaptor.getValue().get(0).getType()).isEqualTo(StockMovementType.RECEIPT);
  }

  @Test
  void get_returnsReceipt() {
    UUID id = UUID.randomUUID();
    GoodsReceipt receipt = receipt(PurchaseOrderStatus.APPROVED);
    receipt.setId(id);
    when(goodsReceiptRepository.findById(id)).thenReturn(Optional.of(receipt));

    GoodsReceipt actual = goodsReceiptService.get(id);

    assertThat(actual).isSameAs(receipt);
  }

  @Test
  void list_returnsFilteredReceipts() {
    GoodsReceipt receipt = receipt(PurchaseOrderStatus.APPROVED);
    when(goodsReceiptRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(
        new PageImpl<>(List.of(receipt))
    );

    assertThat(goodsReceiptService.list(UUID.randomUUID(), UUID.randomUUID(), PageRequest.of(0, 20))).hasSize(1);
    verify(goodsReceiptRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void createGoodsReceipt_rejectsQuantityBeyondRemainingOrder() {
    UUID purchaseOrderId = UUID.randomUUID();
    UUID receivedById = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();

    PurchaseOrder purchaseOrder = purchaseOrder(PurchaseOrderStatus.APPROVED);
    purchaseOrder.setId(purchaseOrderId);
    PurchaseOrderItem item = item(purchaseOrder, itemId, 5, 4, "SKU-A");
    purchaseOrder.getItems().add(item);

    when(purchaseOrderRepository.findById(purchaseOrderId)).thenReturn(Optional.of(purchaseOrder));
    when(userRepository.findById(receivedById)).thenReturn(Optional.of(user(receivedById)));

    CreateGoodsReceiptCommand command = new CreateGoodsReceiptCommand(
        purchaseOrderId,
        receivedById,
        null,
        null,
        List.of(new GoodsReceiptLineCommand(itemId, 2, "BATCH-X", LocalDate.of(2027, 3, 1), new BigDecimal("4.00"), new BigDecimal("6.00")))
    );

    InvalidGoodsReceiptException exception = assertThrows(
        InvalidGoodsReceiptException.class,
        () -> goodsReceiptService.createGoodsReceipt(command)
    );

    assertThat(exception.getMessage()).contains("exceeds remaining ordered quantity");
  }

  private static GoodsReceipt receipt(PurchaseOrderStatus status) {
    GoodsReceipt receipt = GoodsReceipt.create(purchaseOrder(status), user(UUID.randomUUID()), LocalDateTime.now(), "note");
    receipt.getBatches().add(Batch.createForReceipt(item(receipt.getPurchaseOrder(), UUID.randomUUID(), 1, 0, "SKU-Z"), receipt, "BATCH-Z", LocalDate.of(2027, 1, 1), new BigDecimal("1.00"), new BigDecimal("2.00"), 1, LocalDateTime.now()));
    return receipt;
  }

  private static PurchaseOrder purchaseOrder(PurchaseOrderStatus status) {
    PurchaseOrder purchaseOrder = new PurchaseOrder();
    purchaseOrder.setPoNumber("PO-20260527-ABC12345");
    purchaseOrder.setSupplier(supplier());
    purchaseOrder.setStatus(status);
    purchaseOrder.setCreatedBy(user(UUID.randomUUID()));
    return purchaseOrder;
  }

  private static PurchaseOrderItem item(PurchaseOrder purchaseOrder, UUID id, int orderedQuantity, int receivedQuantity, String sku) {
    PurchaseOrderItem item = new PurchaseOrderItem();
    item.setId(id);
    item.setPurchaseOrder(purchaseOrder);
    item.setProduct(product(sku));
    item.setOrderedQuantity(orderedQuantity);
    item.setReceivedQuantity(receivedQuantity);
    item.setUnitCost(new BigDecimal("7.50"));
    return item;
  }

  private static Supplier supplier() {
    Supplier supplier = new Supplier();
    supplier.setName("Acme Pharma");
    supplier.setActive(true);
    return supplier;
  }

  private static User user(UUID id) {
    User user = new User();
    user.setId(id);
    user.setUsername("arima");
    user.setFullName("Arima");
    user.setEmail("arima@example.com");
    user.setPasswordHash("hash");
    user.setActive(true);
    return user;
  }

  private static Product product(String sku) {
    Product product = new Product();
    product.setSkuBarcode(sku);
    product.setName("Paracetamol 500mg");
    product.setActive(true);
    return product;
  }
}
