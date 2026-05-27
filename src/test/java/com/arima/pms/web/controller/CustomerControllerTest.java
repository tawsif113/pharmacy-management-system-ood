package com.arima.pms.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arima.pms.service.CustomerService;
import com.arima.pms.service.command.CreateCustomerCommand;
import com.arima.pms.service.command.UpdateCustomerCommand;
import com.arima.pms.web.dto.CustomerResponse;
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
class CustomerControllerTest {

  @Mock
  private CustomerService customerService;

  @InjectMocks
  private CustomerController customerController;

  @Test
  void createDelegatesToService() {
    CustomerResponse response = new CustomerResponse(UUID.randomUUID(), "Rakib Hasan", "01711111111", "Mirpur", LocalDate.of(1995, 5, 17), "Regular customer", null, null);
    when(customerService.create(any(CreateCustomerCommand.class))).thenReturn(response);

    CustomerResponse actual = customerController.create(new CreateCustomerCommand(
        "Rakib Hasan",
        "01711111111",
        "Mirpur",
        LocalDate.of(1995, 5, 17),
        "Regular customer"
    )).getBody();

    assertThat(actual).isEqualTo(response);
    verify(customerService).create(any(CreateCustomerCommand.class));
  }

  @Test
  void listDelegatesToService() {
    when(customerService.list(null, null, null, PageRequest.of(0, 20))).thenReturn(
        new PageImpl<>(List.of(new CustomerResponse(UUID.randomUUID(), "Rakib Hasan", null, null, null, null, null, null)))
    );

    assertThat(customerController.list(null, null, null, PageRequest.of(0, 20))).hasSize(1);
    verify(customerService).list(null, null, null, PageRequest.of(0, 20));
  }

  @Test
  void updateDelegatesToService() {
    UUID id = UUID.randomUUID();
    CustomerResponse response = new CustomerResponse(id, "New Name", null, null, null, null, null, null);
    when(customerService.update(any(UUID.class), any(UpdateCustomerCommand.class))).thenReturn(response);

    CustomerResponse actual = customerController.update(id, new UpdateCustomerCommand(
        "New Name",
        null,
        null,
        null,
        null
    ));

    assertThat(actual).isEqualTo(response);
    verify(customerService).update(any(UUID.class), any(UpdateCustomerCommand.class));
  }
}
