package com.arima.pms.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arima.pms.domain.entity.Product;
import com.arima.pms.domain.entity.PurchaseOrder;
import com.arima.pms.domain.entity.PurchaseOrderItem;
import com.arima.pms.domain.entity.Supplier;
import com.arima.pms.domain.entity.User;
import com.arima.pms.domain.enums.PurchaseOrderStatus;
import com.arima.pms.service.PurchaseOrderService;
import com.arima.pms.service.command.CreatePurchaseOrderCommand;
import com.arima.pms.web.dto.PurchaseOrderResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class PurchaseOrderControllerTest {

  @Mock
  private PurchaseOrderService purchaseOrderService;

  @InjectMocks
  private PurchaseOrderController purchaseOrderController;

  @Test
  void createDelegatesToService() {
    PurchaseOrder purchaseOrder = purchaseOrder(PurchaseOrderStatus.DRAFT);
    when(purchaseOrderService.createDraftPurchaseOrder(any(CreatePurchaseOrderCommand.class))).thenReturn(purchaseOrder);

    PurchaseOrderResponse actual = purchaseOrderController.create(new CreatePurchaseOrderCommand(
        UUID.randomUUID(),
        UUID.randomUUID(),
        LocalDate.of(2026, 6, 15),
        List.of()
    )).getBody();

    assertThat(actual.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
    verify(purchaseOrderService).createDraftPurchaseOrder(any(CreatePurchaseOrderCommand.class));
  }

  @Test
  void listDelegatesToService() {
    PurchaseOrder purchaseOrder = purchaseOrder(PurchaseOrderStatus.APPROVED);
    when(purchaseOrderService.list(null, null, null, PageRequest.of(0, 20))).thenReturn(
        new PageImpl<>(List.of(purchaseOrder))
    );

    assertThat(purchaseOrderController.list(null, null, null, PageRequest.of(0, 20))).hasSize(1);
    verify(purchaseOrderService).list(null, null, null, PageRequest.of(0, 20));
  }

  @Test
  void getDelegatesToService() {
    PurchaseOrder purchaseOrder = purchaseOrder(PurchaseOrderStatus.APPROVED);
    UUID id = UUID.randomUUID();
    purchaseOrder.setId(id);
    when(purchaseOrderService.get(id)).thenReturn(purchaseOrder);

    PurchaseOrderResponse actual = purchaseOrderController.get(id);

    assertThat(actual.id()).isEqualTo(id);
    verify(purchaseOrderService).get(id);
  }

  @Test
  void approveDelegatesToService() {
    UUID id = UUID.randomUUID();
    PurchaseOrder purchaseOrder = purchaseOrder(PurchaseOrderStatus.APPROVED);
    when(purchaseOrderService.approve(id)).thenReturn(purchaseOrder);

    PurchaseOrderResponse actual = purchaseOrderController.approve(id);

    assertThat(actual.status()).isEqualTo(PurchaseOrderStatus.APPROVED);
    verify(purchaseOrderService).approve(id);
  }

  @Test
  void cancelDelegatesToService() {
    UUID id = UUID.randomUUID();
    PurchaseOrder purchaseOrder = purchaseOrder(PurchaseOrderStatus.CANCELLED);
    when(purchaseOrderService.cancel(id)).thenReturn(purchaseOrder);

    PurchaseOrderResponse actual = purchaseOrderController.cancel(id);

    assertThat(actual.status()).isEqualTo(PurchaseOrderStatus.CANCELLED);
    verify(purchaseOrderService).cancel(id);
  }

  private static PurchaseOrder purchaseOrder(PurchaseOrderStatus status) {
    PurchaseOrder purchaseOrder = new PurchaseOrder();
    purchaseOrder.setPoNumber("PO-20260527-ABC12345");
    purchaseOrder.setSupplier(supplier());
    purchaseOrder.setStatus(status);
    purchaseOrder.setCreatedBy(user(UUID.randomUUID()));
    PurchaseOrderItem item = new PurchaseOrderItem();
    item.setId(UUID.randomUUID());
    item.setPurchaseOrder(purchaseOrder);
    item.setProduct(product());
    item.setOrderedQuantity(2);
    item.setReceivedQuantity(1);
    item.setUnitCost(new BigDecimal("10.00"));
    purchaseOrder.getItems().add(item);
    return purchaseOrder;
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
