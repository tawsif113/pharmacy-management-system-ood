package com.arima.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arima.pms.domain.entity.Product;
import com.arima.pms.repository.ProductRepository;
import com.arima.pms.service.command.CreateProductCommand;
import com.arima.pms.service.command.UpdateProductCommand;
import com.arima.pms.service.exception.DuplicateProductException;
import com.arima.pms.service.exception.InvalidProductException;
import com.arima.pms.web.dto.ProductResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock
  private ProductRepository productRepository;

  @InjectMocks
  private ProductService productService;

  @Test
  void createPersistsNewProduct() {
    CreateProductCommand command = new CreateProductCommand(
        "SKU-1001",
        "Paracetamol 500mg",
        "Acme",
        "Paracetamol",
        "Analgesic",
        "Tablet",
        "500mg",
        "10 tablets",
        true,
        20,
        true
    );

    when(productRepository.existsBySkuBarcodeIgnoreCase("SKU-1001")).thenReturn(false);
    when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ProductResponse response = productService.create(command);

    assertThat(response.skuBarcode()).isEqualTo("SKU-1001");
    assertThat(response.name()).isEqualTo("Paracetamol 500mg");
    assertThat(response.prescriptionRequired()).isTrue();
    assertThat(response.reorderLevel()).isEqualTo(20);

    ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).save(productCaptor.capture());
    assertThat(productCaptor.getValue().getSkuBarcode()).isEqualTo("SKU-1001");
  }

  @Test
  void createRejectsDuplicateSku() {
    when(productRepository.existsBySkuBarcodeIgnoreCase("SKU-1001")).thenReturn(true);

    DuplicateProductException exception = assertThrows(
        DuplicateProductException.class,
        () -> productService.create(new CreateProductCommand(
            "SKU-1001",
            "Paracetamol 500mg",
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            0,
            true
        ))
    );

    assertThat(exception.getMessage()).contains("already exists");
  }

  @Test
  void updateChangesMutableFields() {
    UUID id = UUID.randomUUID();
    Product product = product();
    product.setId(id);

    when(productRepository.findById(id)).thenReturn(Optional.of(product));
    when(productRepository.existsBySkuBarcodeIgnoreCaseAndIdNot("SKU-2002", id)).thenReturn(false);
    when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ProductResponse response = productService.update(id, new UpdateProductCommand(
        "SKU-2002",
        "Paracetamol Forte",
        "Acme",
        null,
        "Analgesic",
        null,
        null,
        null,
        false,
        15,
        false
    ));

    assertThat(response.skuBarcode()).isEqualTo("SKU-2002");
    assertThat(response.name()).isEqualTo("Paracetamol Forte");
    assertThat(response.active()).isFalse();
    assertThat(product.getReorderLevel()).isEqualTo(15);
  }

  @Test
  void updateRejectsNegativeReorderLevel() {
    UUID id = UUID.randomUUID();
    when(productRepository.findById(id)).thenReturn(Optional.of(product()));

    InvalidProductException exception = assertThrows(
        InvalidProductException.class,
        () -> productService.update(id, new UpdateProductCommand(null, null, null, null, null, null, null, null, null, -1, null))
    );

    assertThat(exception.getMessage()).contains("negative");
  }

  private static Product product() {
    Product product = new Product();
    product.setSkuBarcode("SKU-1001");
    product.setName("Paracetamol 500mg");
    product.setActive(true);
    product.setReorderLevel(10);
    return product;
  }
}
