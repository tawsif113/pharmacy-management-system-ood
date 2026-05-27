package com.arima.pms.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arima.pms.domain.entity.Batch;
import com.arima.pms.domain.entity.Customer;
import com.arima.pms.domain.entity.Product;
import com.arima.pms.domain.entity.Sale;
import com.arima.pms.domain.entity.SaleItem;
import com.arima.pms.domain.entity.Supplier;
import com.arima.pms.domain.entity.User;
import com.arima.pms.domain.enums.BatchStatus;
import com.arima.pms.domain.enums.PaymentStatus;
import com.arima.pms.domain.enums.SaleStatus;
import com.arima.pms.service.SaleService;
import com.arima.pms.service.command.CreateSaleCommand;
import com.arima.pms.service.command.CreateSaleLineCommand;
import com.arima.pms.service.command.SaleCancelCommand;
import com.arima.pms.service.command.SaleConfirmCommand;
import com.arima.pms.service.command.SaleVoidCommand;
import com.arima.pms.service.command.UpdateSaleCommand;
import com.arima.pms.web.dto.SaleResponse;
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
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class SaleControllerTest {

  @Mock private SaleService saleService;
  @InjectMocks private SaleController saleController;

  @Test
  void create_returnsCreated() {
    when(saleService.createDraftSale(any())).thenReturn(saleEntity());
    ResponseEntity<SaleResponse> response = saleController.create(new CreateSaleCommand(UUID.randomUUID(), UUID.randomUUID(), null, BigDecimal.ZERO, BigDecimal.ZERO, List.of(new CreateSaleLineCommand(UUID.randomUUID(), 1))));
    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isNotNull();
  }

  @Test
  void list_mapsPage() {
    when(saleService.list(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(saleEntity())));
    assertThat(saleController.list(null, null, null, null, null, null, null, PageRequest.of(0, 20)).getContent()).hasSize(1);
  }

  @Test
  void get_returnsSale() {
    UUID id = UUID.randomUUID();
    when(saleService.get(id)).thenReturn(saleEntity());
    assertThat(saleController.get(id).invoiceNumber()).isNotNull();
  }

  @Test
  void update_callsService() {
    UUID id = UUID.randomUUID();
    when(saleService.updateDraftSale(any(), any())).thenReturn(saleEntity());
    saleController.update(id, new UpdateSaleCommand(null, null, BigDecimal.ONE, BigDecimal.ONE, null));
    verify(saleService).updateDraftSale(any(), any());
  }

  @Test
  void confirm_callsService() {
    UUID id = UUID.randomUUID();
    when(saleService.confirmSale(any(), any())).thenReturn(saleEntity());
    saleController.confirm(id, new SaleConfirmCommand(UUID.randomUUID(), PaymentStatus.PAID));
    verify(saleService).confirmSale(any(), any());
  }

  @Test
  void cancel_callsService() {
    UUID id = UUID.randomUUID();
    when(saleService.cancelSale(any(), any())).thenReturn(saleEntity());
    saleController.cancel(id, new SaleCancelCommand(UUID.randomUUID(), "nope"));
    verify(saleService).cancelSale(any(), any());
  }

  @Test
  void voidSale_callsService() {
    UUID id = UUID.randomUUID();
    when(saleService.voidSale(any(), any())).thenReturn(saleEntity());
    saleController.voidSale(id, new SaleVoidCommand(UUID.randomUUID(), "voiding"));
    verify(saleService).voidSale(any(), any());
  }

  private static Sale saleEntity() {
    Sale sale = new Sale();
    sale.setId(UUID.randomUUID());
    sale.setInvoiceNumber("INV-20260527-AAAA1111");
    sale.setCustomer(customer());
    sale.setStatus(SaleStatus.DRAFT);
    sale.setSubtotal(new BigDecimal("10.00"));
    sale.setDiscount(BigDecimal.ZERO);
    sale.setTax(BigDecimal.ZERO);
    sale.setTotal(new BigDecimal("10.00"));
    sale.setPaymentStatus(PaymentStatus.UNPAID);
    sale.setCreatedBy(user());
    sale.setCreatedAt(LocalDateTime.now());
    sale.setUpdatedAt(LocalDateTime.now());
    sale.addItem(product(), batch(), 1, new BigDecimal("10.00"), BigDecimal.ZERO);
    return sale;
  }

  private static Product product() {
    Product product = new Product();
    product.setId(UUID.randomUUID());
    product.setSkuBarcode("SKU-A");
    product.setName("Medicine A");
    product.setActive(true);
    product.setPrescriptionRequired(false);
    return product;
  }

  private static Batch batch() {
    Batch batch = new Batch();
    batch.setId(UUID.randomUUID());
    batch.setProduct(product());
    batch.setSupplier(supplier());
    batch.setBatchNumber("BATCH-A");
    batch.setReceivedQuantity(1);
    batch.setAvailableQuantity(1);
    batch.setExpiryDate(LocalDate.now().plusDays(10));
    batch.setReceivedAt(LocalDateTime.now().minusDays(1));
    batch.setStatus(BatchStatus.AVAILABLE);
    batch.setPurchaseCost(new BigDecimal("5.00"));
    batch.setSellingPrice(new BigDecimal("10.00"));
    return batch;
  }

  private static Customer customer() {
    Customer customer = new Customer();
    customer.setId(UUID.randomUUID());
    customer.setName("Customer A");
    return customer;
  }

  private static Supplier supplier() {
    Supplier supplier = new Supplier();
    supplier.setId(UUID.randomUUID());
    supplier.setName("Acme Pharma");
    return supplier;
  }

  private static User user() {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername("smoke.user");
    user.setFullName("Smoke User");
    user.setEmail("smoke@example.com");
    user.setPasswordHash("hash");
    user.setActive(true);
    return user;
  }
}
