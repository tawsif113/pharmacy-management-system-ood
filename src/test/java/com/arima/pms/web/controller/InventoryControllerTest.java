package com.arima.pms.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arima.pms.domain.entity.Batch;
import com.arima.pms.domain.entity.Product;
import com.arima.pms.domain.entity.StockMovement;
import com.arima.pms.domain.entity.Supplier;
import com.arima.pms.domain.entity.User;
import com.arima.pms.domain.enums.BatchStatus;
import com.arima.pms.domain.enums.StockMovementType;
import com.arima.pms.service.InventoryService;
import com.arima.pms.service.command.AdjustBatchStockCommand;
import com.arima.pms.service.command.ReturnBatchStockCommand;
import com.arima.pms.service.command.WriteOffBatchStockCommand;
import com.arima.pms.web.dto.BatchResponse;
import com.arima.pms.web.dto.StockMovementResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

  @Mock
  private InventoryService inventoryService;

  @InjectMocks
  private InventoryController inventoryController;

  @Test
  void listBatchesDelegatesToService() {
    Batch batch = batch(BatchStatus.AVAILABLE);
    when(inventoryService.listBatches(null, null, null, null, null, null, PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(batch)));

    assertThat(inventoryController.listBatches(null, null, null, null, null, null, PageRequest.of(0, 10))).hasSize(1);
    verify(inventoryService).listBatches(null, null, null, null, null, null, PageRequest.of(0, 10));
  }

  @Test
  void getBatchDelegatesToService() {
    Batch batch = batch(BatchStatus.AVAILABLE);
    UUID id = UUID.randomUUID();
    batch.setId(id);
    when(inventoryService.getBatch(id)).thenReturn(batch);

    BatchResponse response = inventoryController.getBatch(id);

    assertThat(response.id()).isEqualTo(id);
    verify(inventoryService).getBatch(id);
  }

  @Test
  void adjustDelegatesToService() {
    UUID id = UUID.randomUUID();
    Batch batch = batch(BatchStatus.AVAILABLE);
    when(inventoryService.adjustBatchStock(any(UUID.class), any(AdjustBatchStockCommand.class))).thenReturn(batch);

    BatchResponse response = inventoryController.adjust(id, new AdjustBatchStockCommand(UUID.randomUUID(), 2, "count"));

    assertThat(response.status()).isEqualTo(BatchStatus.AVAILABLE);
  }

  @Test
  void returnStockDelegatesToService() {
    UUID id = UUID.randomUUID();
    Batch batch = batch(BatchStatus.AVAILABLE);
    when(inventoryService.returnBatchStock(any(UUID.class), any(ReturnBatchStockCommand.class))).thenReturn(batch);

    BatchResponse response = inventoryController.returnStock(id, new ReturnBatchStockCommand(UUID.randomUUID(), 2, "return"));

    assertThat(response.availableQuantity()).isEqualTo(10);
  }

  @Test
  void writeOffDelegatesToService() {
    UUID id = UUID.randomUUID();
    Batch batch = batch(BatchStatus.AVAILABLE);
    when(inventoryService.writeOffBatchStock(any(UUID.class), any(WriteOffBatchStockCommand.class))).thenReturn(batch);

    BatchResponse response = inventoryController.writeOff(id, new WriteOffBatchStockCommand(UUID.randomUUID(), 2, "damage"));

    assertThat(response.availableQuantity()).isEqualTo(10);
  }

  @Test
  void listMovementsDelegatesToService() {
    StockMovement movement = stockMovement();
    when(inventoryService.listStockMovements(null, null, null, PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(movement)));

    assertThat(inventoryController.listMovements(null, null, null, PageRequest.of(0, 10))).hasSize(1);
  }

  @Test
  void lowStockAlertsDelegatesToService() {
    when(inventoryService.lowStockAlerts()).thenReturn(List.of());

    assertThat(inventoryController.lowStockAlerts()).isEmpty();
  }

  private static Batch batch(BatchStatus status) {
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
    batch.setId(UUID.randomUUID());
    batch.setProduct(product);
    batch.setSupplier(supplier);
    batch.setBatchNumber("BATCH-1");
    batch.setPurchaseCost(new BigDecimal("10.00"));
    batch.setSellingPrice(new BigDecimal("15.00"));
    batch.setReceivedQuantity(10);
    batch.setAvailableQuantity(10);
    batch.setExpiryDate(LocalDate.now().plusDays(30));
    batch.setReceivedAt(LocalDateTime.now());
    batch.setStatus(status);
    return batch;
  }

  private static StockMovement stockMovement() {
    StockMovement movement = new StockMovement();
    movement.setId(UUID.randomUUID());
    movement.setBatch(batch(BatchStatus.AVAILABLE));
    movement.setProduct(movement.getBatch().getProduct());
    movement.setType(StockMovementType.ADJUSTMENT);
    movement.setQuantity(2);
    movement.setDeltaQuantity(2);
    movement.setReferenceType("inventory_adjustment");
    movement.setReferenceId(UUID.randomUUID());
    movement.setReason("adjustment");
    movement.setCreatedBy(user());
    return movement;
  }

  private static User user() {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername("arima");
    user.setFullName("Arima");
    user.setEmail("arima@example.com");
    user.setPasswordHash("hash");
    user.setActive(true);
    return user;
  }
}
