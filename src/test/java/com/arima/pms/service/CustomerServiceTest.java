package com.arima.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arima.pms.domain.entity.Customer;
import com.arima.pms.repository.CustomerRepository;
import com.arima.pms.service.command.CreateCustomerCommand;
import com.arima.pms.service.command.UpdateCustomerCommand;
import com.arima.pms.service.exception.InvalidCustomerException;
import com.arima.pms.web.dto.CustomerResponse;
import java.time.LocalDate;
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
class CustomerServiceTest {

  @Mock
  private CustomerRepository customerRepository;

  @InjectMocks
  private CustomerService customerService;

  @Test
  void createPersistsCustomer() {
    when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CustomerResponse response = customerService.create(new CreateCustomerCommand(
        "Rakib Hasan",
        "01711111111",
        "Mirpur",
        LocalDate.of(1995, 5, 17),
        "Regular customer"
    ));

    assertThat(response.name()).isEqualTo("Rakib Hasan");
    assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1995, 5, 17));

    ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
    verify(customerRepository).save(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo("Rakib Hasan");
  }

  @Test
  void createRejectsBlankName() {
    InvalidCustomerException exception = assertThrows(
        InvalidCustomerException.class,
        () -> customerService.create(new CreateCustomerCommand("  ", null, null, null, null))
    );

    assertThat(exception.getMessage()).contains("name");
  }

  @Test
  void updateChangesCustomerFields() {
    UUID id = UUID.randomUUID();
    Customer customer = new Customer();
    customer.setId(id);
    customer.setName("Old Name");

    when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
    when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CustomerResponse response = customerService.update(id, new UpdateCustomerCommand(
        "New Name",
        "01822222222",
        "New Address",
        LocalDate.of(1991, 1, 2),
        "Updated note"
    ));

    assertThat(response.name()).isEqualTo("New Name");
    assertThat(customer.getPhone()).isEqualTo("01822222222");
    assertThat(customer.getNotes()).isEqualTo("Updated note");
  }

  @Test
  void listAppliesSearchFilter() {
    Customer customer = new Customer();
    customer.setName("Rakib Hasan");
    when(customerRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Customer>>any(), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(java.util.List.of(customer)));

    assertThat(customerService.list("Rakib", null, null, PageRequest.of(0, 20))).hasSize(1);
  }
}
