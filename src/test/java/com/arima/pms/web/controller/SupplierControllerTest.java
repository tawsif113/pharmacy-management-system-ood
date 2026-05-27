package com.arima.pms.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arima.pms.service.SupplierService;
import com.arima.pms.service.command.CreateSupplierCommand;
import com.arima.pms.service.command.UpdateSupplierCommand;
import com.arima.pms.web.dto.SupplierResponse;
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
class SupplierControllerTest {

  @Mock
  private SupplierService supplierService;

  @InjectMocks
  private SupplierController supplierController;

  @Test
  void createDelegatesToService() {
    SupplierResponse response = new SupplierResponse(UUID.randomUUID(), "Acme Supplies", "01700000000", "acme@example.com", "Dhaka", true, null, null);
    when(supplierService.create(any(CreateSupplierCommand.class))).thenReturn(response);

    SupplierResponse actual = supplierController.create(new CreateSupplierCommand(
        "Acme Supplies",
        "01700000000",
        "acme@example.com",
        "Dhaka",
        true
    )).getBody();

    assertThat(actual).isEqualTo(response);
    verify(supplierService).create(any(CreateSupplierCommand.class));
  }

  @Test
  void listDelegatesToService() {
    when(supplierService.list(null, null, PageRequest.of(0, 20))).thenReturn(
        new PageImpl<>(List.of(new SupplierResponse(UUID.randomUUID(), "Acme Supplies", null, null, null, true, null, null)))
    );

    assertThat(supplierController.list(null, null, PageRequest.of(0, 20))).hasSize(1);
    verify(supplierService).list(null, null, PageRequest.of(0, 20));
  }

  @Test
  void updateDelegatesToService() {
    UUID id = UUID.randomUUID();
    SupplierResponse response = new SupplierResponse(id, "New Name", null, null, null, false, null, null);
    when(supplierService.update(any(UUID.class), any(UpdateSupplierCommand.class))).thenReturn(response);

    SupplierResponse actual = supplierController.update(id, new UpdateSupplierCommand(
        "New Name",
        null,
        null,
        null,
        false
    ));

    assertThat(actual).isEqualTo(response);
    verify(supplierService).update(any(UUID.class), any(UpdateSupplierCommand.class));
  }
}
