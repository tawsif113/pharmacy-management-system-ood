package com.arima.pms.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arima.pms.service.ProductService;
import com.arima.pms.service.command.CreateProductCommand;
import com.arima.pms.service.command.UpdateProductCommand;
import com.arima.pms.web.dto.ProductResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

  @Mock
  private ProductService productService;

  @InjectMocks
  private ProductController productController;

  @Test
  void createDelegatesToService() {
    ProductResponse response = new ProductResponse(
        UUID.randomUUID(),
        "SKU-1001",
        "Paracetamol 500mg",
        null,
        null,
        null,
        null,
        null,
        null,
        true,
        20,
        true
    );
    when(productService.create(any(CreateProductCommand.class))).thenReturn(response);

    ProductResponse actual = productController.create(new CreateProductCommand(
        "SKU-1001",
        "Paracetamol 500mg",
        null,
        null,
        null,
        null,
        null,
        null,
        true,
        20,
        true
    )).getBody();

    assertThat(actual).isEqualTo(response);
    verify(productService).create(any(CreateProductCommand.class));
  }

  @Test
  void listDelegatesToService() {
    when(productService.list(null, null, null, null, PageRequest.of(0, 20))).thenReturn(
        new PageImpl<>(List.of(new ProductResponse(UUID.randomUUID(), "SKU-1001", "Paracetamol 500mg", null, null, null, null, null, null, true, 20, true)))
    );

    assertThat(productController.list(null, null, null, null, PageRequest.of(0, 20))).hasSize(1);
    verify(productService).list(null, null, null, null, PageRequest.of(0, 20));
  }

  @Test
  void updateDelegatesToService() {
    UUID id = UUID.randomUUID();
    ProductResponse response = new ProductResponse(id, "SKU-2002", "Paracetamol Forte", null, null, null, null, null, null, false, 15, false);
    when(productService.update(any(UUID.class), any(UpdateProductCommand.class))).thenReturn(response);

    ProductResponse actual = productController.update(id, new UpdateProductCommand(
        "SKU-2002",
        "Paracetamol Forte",
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        15,
        false
    ));

    assertThat(actual).isEqualTo(response);
    verify(productService).update(any(UUID.class), any(UpdateProductCommand.class));
  }
}
