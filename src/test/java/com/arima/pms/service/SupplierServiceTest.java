package com.arima.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arima.pms.domain.entity.Supplier;
import com.arima.pms.repository.SupplierRepository;
import com.arima.pms.service.command.CreateSupplierCommand;
import com.arima.pms.service.command.UpdateSupplierCommand;
import com.arima.pms.service.exception.InvalidSupplierException;
import com.arima.pms.web.dto.SupplierResponse;
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

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

  @Mock
  private SupplierRepository supplierRepository;

  @InjectMocks
  private SupplierService supplierService;

  @Test
  void createPersistsSupplier() {
    when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

    SupplierResponse response = supplierService.create(new CreateSupplierCommand(
        "Acme Supplies",
        "01700000000",
        "acme@example.com",
        "Dhaka",
        true
    ));

    assertThat(response.name()).isEqualTo("Acme Supplies");
    assertThat(response.active()).isTrue();

    ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
    verify(supplierRepository).save(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo("Acme Supplies");
  }

  @Test
  void createRejectsBlankName() {
    InvalidSupplierException exception = assertThrows(
        InvalidSupplierException.class,
        () -> supplierService.create(new CreateSupplierCommand("   ", null, null, null, null))
    );

    assertThat(exception.getMessage()).contains("name");
  }

  @Test
  void updateChangesSupplierFields() {
    UUID id = UUID.randomUUID();
    Supplier supplier = new Supplier();
    supplier.setId(id);
    supplier.setName("Old Name");
    supplier.setActive(true);

    when(supplierRepository.findById(id)).thenReturn(Optional.of(supplier));
    when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

    SupplierResponse response = supplierService.update(id, new UpdateSupplierCommand(
        "New Name",
        "01800000000",
        "new@example.com",
        "Chattogram",
        false
    ));

    assertThat(response.name()).isEqualTo("New Name");
    assertThat(response.active()).isFalse();
    assertThat(supplier.getPhone()).isEqualTo("01800000000");
  }

  @Test
  void listAppliesSearchFilter() {
    Supplier supplier = new Supplier();
    supplier.setName("Acme Supplies");
    supplier.setActive(true);
    when(supplierRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Supplier>>any(), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(java.util.List.of(supplier)));

    assertThat(supplierService.list("Acme", null, PageRequest.of(0, 20))).hasSize(1);
  }
}
