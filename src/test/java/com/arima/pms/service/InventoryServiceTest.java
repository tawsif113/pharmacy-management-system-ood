package com.arima.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arima.pms.domain.entity.Batch;
import com.arima.pms.domain.entity.Product;
import com.arima.pms.domain.entity.StockMovement;
import com.arima.pms.domain.entity.Supplier;
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
class InventoryServiceTest {

  @Mock
  private BatchRepository batchRepository;

  @Mock
  private StockMovementRepository stockMovementRepository;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private InventoryService inventoryService;

  @Test
  void adjustBatchStock_increasesAndRecordsMovement() {
    UUID batchId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Batch batch = batch(batchId, 10, LocalDate.now().plusDays(30), BatchStatus.AVAILABLE);
    User user = user(userId, true);

    when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Batch updated = inventoryService.adjustBatchStock(batchId, new AdjustBatchStockCommand(userId, 5, "count correction"));

    assertThat(updated.getAvailableQuantity()).isEqualTo(15);
    ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
    verify(stockMovementRepository).save(movementCaptor.capture());
    assertThat(movementCaptor.getValue().getType()).isEqualTo(StockMovementType.ADJUSTMENT);
    assertThat(movementCaptor.getValue().getDeltaQuantity()).isEqualTo(5);
  }

  @Test
  void returnBatchStock_increasesStockAndRecordsReturnMovement() {
    UUID batchId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Batch batch = batch(batchId, 4, LocalDate.now().plusDays(30), BatchStatus.AVAILABLE);
    User user = user(userId, true);

    when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Batch updated = inventoryService.returnBatchStock(batchId, new ReturnBatchStockCommand(userId, 2, "customer return"));

    assertThat(updated.getAvailableQuantity()).isEqualTo(6);
    verify(stockMovementRepository).save(any(StockMovement.class));
  }

  @Test
  void writeOffBatchStock_decreasesStockAndRecordsWriteOff() {
    UUID batchId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Batch batch = batch(batchId, 7, LocalDate.now().plusDays(30), BatchStatus.AVAILABLE);
    User user = user(userId, true);

    when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Batch updated = inventoryService.writeOffBatchStock(batchId, new WriteOffBatchStockCommand(userId, 3, "damaged stock"));

    assertThat(updated.getAvailableQuantity()).isEqualTo(4);
    assertThat(updated.getStatus()).isEqualTo(BatchStatus.AVAILABLE);
  }

  @Test
  void listBatches_supportsFiltering() {
    Batch batch = batch(UUID.randomUUID(), 1, LocalDate.now().plusDays(5), BatchStatus.AVAILABLE);
    when(batchRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(batch)));

    assertThat(inventoryService.listBatches("BATCH", null, null, null, null, null, PageRequest.of(0, 10))).hasSize(1);
    verify(batchRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void listStockMovements_supportsFiltering() {
    StockMovement movement = stockMovement(UUID.randomUUID(), UUID.randomUUID(), 2);
    when(stockMovementRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(movement)));

    assertThat(inventoryService.listStockMovements(null, null, null, PageRequest.of(0, 10))).hasSize(1);
  }

  @Test
  void lowStockAlerts_returnsSummaries() {
    Product product = new Product();
    product.setId(UUID.randomUUID());
    product.setSkuBarcode("SKU-1");
    product.setName("Paracetamol");
    product.setReorderLevel(5);
    product.setActive(true);

    when(productRepository.findAll()).thenReturn(List.of(product));
    when(batchRepository.findAll()).thenReturn(List.of(
        batch(UUID.randomUUID(), 2, LocalDate.now().plusDays(15), BatchStatus.AVAILABLE)
    ));

    assertThat(inventoryService.lowStockAlerts()).isNotEmpty();
  }

  @Test
  void expiringAlerts_returnsSoonExpiringBatches() {
    when(batchRepository.findAll()).thenReturn(List.of(
        batch(UUID.randomUUID(), 2, LocalDate.now().plusDays(3), BatchStatus.AVAILABLE)
    ));

    assertThat(inventoryService.expiringAlerts(7)).hasSize(1);
  }

  @Test
  void writeOffBatchStock_rejectsTooMuchQuantity() {
    UUID batchId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Batch batch = batch(batchId, 1, LocalDate.now().plusDays(30), BatchStatus.AVAILABLE);
    when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, true)));

    InvalidInventoryException exception = assertThrows(
        InvalidInventoryException.class,
        () -> inventoryService.writeOffBatchStock(batchId, new WriteOffBatchStockCommand(userId, 3, "oops"))
    );

    assertThat(exception.getMessage()).contains("exceeds");
  }

  private static Batch batch(UUID id, int availableQuantity, LocalDate expiryDate, BatchStatus status) {
    Product product = new Product();
    product.setId(UUID.randomUUID());
    product.setSkuBarcode("SKU-1");
    product.setName("Paracetamol");
    product.setReorderLevel(5);
    product.setActive(true);

    Supplier supplier = new Supplier();
    supplier.setId(UUID.randomUUID());
    supplier.setName("Acme Pharma");

    Batch batch = new Batch();
    batch.setId(id);
    batch.setProduct(product);
    batch.setSupplier(supplier);
    batch.setBatchNumber("BATCH-1");
    batch.setPurchaseCost(new BigDecimal("10.00"));
    batch.setSellingPrice(new BigDecimal("15.00"));
    batch.setReceivedQuantity(10);
    batch.setAvailableQuantity(availableQuantity);
    batch.setExpiryDate(expiryDate);
    batch.setReceivedAt(LocalDateTime.now());
    batch.setStatus(status);
    return batch;
  }

  private static StockMovement stockMovement(UUID batchId, UUID userId, int deltaQuantity) {
    StockMovement movement = new StockMovement();
    movement.setId(UUID.randomUUID());
    movement.setBatch(batch(batchId, 10, LocalDate.now().plusDays(30), BatchStatus.AVAILABLE));
    movement.setProduct(movement.getBatch().getProduct());
    movement.setType(StockMovementType.ADJUSTMENT);
    movement.setQuantity(Math.abs(deltaQuantity));
    movement.setDeltaQuantity(deltaQuantity);
    movement.setReferenceType("inventory_adjustment");
    movement.setReferenceId(UUID.randomUUID());
    movement.setReason("adjustment");
    movement.setCreatedBy(user(userId, true));
    return movement;
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
}
