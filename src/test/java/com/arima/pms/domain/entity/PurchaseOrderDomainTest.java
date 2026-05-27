package com.arima.pms.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.arima.pms.domain.enums.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PurchaseOrderDomainTest {

  @Test
  void receiveItem_marksOrderPartiallyReceivedWhenSomeQuantityRemains() {
    PurchaseOrder purchaseOrder = purchaseOrder(PurchaseOrderStatus.APPROVED);
    PurchaseOrderItem item = purchaseOrderItem(UUID.randomUUID(), purchaseOrder, 5, 0);
    purchaseOrder.getItems().add(item);

    purchaseOrder.receiveItem(item.getId(), 3);

    assertThat(item.getReceivedQuantity()).isEqualTo(3);
    assertThat(item.remainingQuantity()).isEqualTo(2);
    assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
  }

  @Test
  void receiveItem_marksOrderReceivedWhenAllItemsAreComplete() {
    PurchaseOrder purchaseOrder = purchaseOrder(PurchaseOrderStatus.APPROVED);
    PurchaseOrderItem item = purchaseOrderItem(UUID.randomUUID(), purchaseOrder, 5, 0);
    purchaseOrder.getItems().add(item);

    purchaseOrder.receiveItem(item.getId(), 5);

    assertThat(item.getReceivedQuantity()).isEqualTo(5);
    assertThat(item.isFullyReceived()).isTrue();
    assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
  }

  @Test
  void findItemById_throwsWhenItemDoesNotExist() {
    PurchaseOrder purchaseOrder = purchaseOrder(PurchaseOrderStatus.APPROVED);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> purchaseOrder.findItemById(UUID.randomUUID())
    );

    assertThat(exception.getMessage()).contains("not found");
  }

  private static PurchaseOrder purchaseOrder(PurchaseOrderStatus status) {
    PurchaseOrder purchaseOrder = new PurchaseOrder();
    purchaseOrder.setId(UUID.randomUUID());
    purchaseOrder.setPoNumber("PO-20260527-ABC12345");
    purchaseOrder.setSupplier(supplier());
    purchaseOrder.setStatus(status);
    purchaseOrder.setCreatedBy(user());
    purchaseOrder.setTotalEstimatedCost(BigDecimal.ZERO);
    return purchaseOrder;
  }

  private static PurchaseOrderItem purchaseOrderItem(UUID id, PurchaseOrder purchaseOrder, int orderedQuantity, int receivedQuantity) {
    PurchaseOrderItem item = new PurchaseOrderItem();
    item.setId(id);
    item.setPurchaseOrder(purchaseOrder);
    item.setProduct(product());
    item.setOrderedQuantity(orderedQuantity);
    item.setReceivedQuantity(receivedQuantity);
    item.setUnitCost(new BigDecimal("10.00"));
    return item;
  }

  private static Supplier supplier() {
    Supplier supplier = new Supplier();
    supplier.setId(UUID.randomUUID());
    supplier.setName("Acme Pharma");
    supplier.setActive(true);
    return supplier;
  }

  private static Product product() {
    Product product = new Product();
    product.setId(UUID.randomUUID());
    product.setSkuBarcode("SKU-1001");
    product.setName("Paracetamol 500mg");
    product.setActive(true);
    return product;
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
