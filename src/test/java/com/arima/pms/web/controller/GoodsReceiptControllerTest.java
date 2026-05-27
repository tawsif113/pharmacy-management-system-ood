package com.arima.pms.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arima.pms.domain.entity.Batch;
import com.arima.pms.domain.entity.GoodsReceipt;
import com.arima.pms.domain.entity.Product;
import com.arima.pms.domain.entity.PurchaseOrder;
import com.arima.pms.domain.entity.PurchaseOrderItem;
import com.arima.pms.domain.entity.Supplier;
import com.arima.pms.domain.entity.User;
import com.arima.pms.domain.enums.PurchaseOrderStatus;
import com.arima.pms.domain.enums.BatchStatus;
import com.arima.pms.service.GoodsReceiptService;
import com.arima.pms.service.command.CreateGoodsReceiptCommand;
import com.arima.pms.web.dto.GoodsReceiptResponse;
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
class GoodsReceiptControllerTest {

  @Mock
  private GoodsReceiptService goodsReceiptService;

  @InjectMocks
  private GoodsReceiptController goodsReceiptController;

  @Test
  void createDelegatesToService() {
    GoodsReceipt receipt = receipt();
    when(goodsReceiptService.createGoodsReceipt(any(CreateGoodsReceiptCommand.class))).thenReturn(receipt);

    GoodsReceiptResponse actual = goodsReceiptController.create(new CreateGoodsReceiptCommand(
        UUID.randomUUID(),
        UUID.randomUUID(),
        LocalDateTime.now(),
        "note",
        List.of()
    )).getBody();

    assertThat(actual.totalQuantity()).isEqualTo(5);
    verify(goodsReceiptService).createGoodsReceipt(any(CreateGoodsReceiptCommand.class));
  }

  @Test
  void listDelegatesToService() {
    GoodsReceipt receipt = receipt();
    when(goodsReceiptService.list(null, null, PageRequest.of(0, 20))).thenReturn(
        new PageImpl<>(List.of(receipt))
    );

    assertThat(goodsReceiptController.list(null, null, PageRequest.of(0, 20))).hasSize(1);
    verify(goodsReceiptService).list(null, null, PageRequest.of(0, 20));
  }

  @Test
  void getDelegatesToService() {
    GoodsReceipt receipt = receipt();
    UUID id = UUID.randomUUID();
    receipt.setId(id);
    when(goodsReceiptService.get(id)).thenReturn(receipt);

    GoodsReceiptResponse actual = goodsReceiptController.get(id);

    assertThat(actual.id()).isEqualTo(id);
    verify(goodsReceiptService).get(id);
  }

  private static GoodsReceipt receipt() {
    PurchaseOrder purchaseOrder = new PurchaseOrder();
    purchaseOrder.setId(UUID.randomUUID());
    purchaseOrder.setPoNumber("PO-20260527-ABC12345");
    purchaseOrder.setSupplier(supplier());
    purchaseOrder.setStatus(PurchaseOrderStatus.APPROVED);
    purchaseOrder.setCreatedBy(user(UUID.randomUUID()));

    PurchaseOrderItem item = new PurchaseOrderItem();
    item.setId(UUID.randomUUID());
    item.setPurchaseOrder(purchaseOrder);
    item.setProduct(product());
    item.setOrderedQuantity(5);
    item.setReceivedQuantity(5);
    item.setUnitCost(new BigDecimal("7.50"));
    purchaseOrder.getItems().add(item);

    GoodsReceipt receipt = GoodsReceipt.create(purchaseOrder, user(UUID.randomUUID()), LocalDateTime.now(), "First delivery");
    Batch batch = Batch.createForReceipt(item, receipt, "BATCH-1", LocalDate.of(2027, 1, 1), new BigDecimal("7.50"), new BigDecimal("10.00"), 5, LocalDateTime.now());
    batch.setId(UUID.randomUUID());
    batch.setStatus(BatchStatus.AVAILABLE);
    receipt.addBatch(batch);
    return receipt;
  }

  private static Supplier supplier() {
    Supplier supplier = new Supplier();
    supplier.setId(UUID.randomUUID());
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

  private static Product product() {
    Product product = new Product();
    product.setId(UUID.randomUUID());
    product.setSkuBarcode("SKU-1001");
    product.setName("Paracetamol 500mg");
    product.setActive(true);
    return product;
  }
}
