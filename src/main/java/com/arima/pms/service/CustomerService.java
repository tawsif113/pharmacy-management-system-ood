package com.arima.pms.service;

import com.arima.pms.domain.entity.Customer;
import com.arima.pms.repository.CustomerRepository;
import com.arima.pms.service.command.CreateCustomerCommand;
import com.arima.pms.service.command.UpdateCustomerCommand;
import com.arima.pms.service.exception.InvalidCustomerException;
import com.arima.pms.service.exception.ResourceNotFoundException;
import com.arima.pms.web.dto.CustomerResponse;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

  private final CustomerRepository customerRepository;

  public CustomerResponse create(CreateCustomerCommand command) {
    validateCreate(command);
    Customer customer = new Customer();
    applyCommand(customer, command.name(), command.phone(), command.address(), command.dateOfBirth(), command.notes());
    return CustomerResponse.from(customerRepository.save(customer));
  }

  @Transactional(readOnly = true)
  public Page<CustomerResponse> list(String search, String phone, LocalDate dateOfBirth, Pageable pageable) {
    Specification<Customer> specification = (root, query, cb) -> cb.conjunction();
    if (StringUtils.hasText(search)) {
      String term = search.trim().toLowerCase();
      specification = specification.and((root, query, cb) -> cb.or(
          cb.like(cb.lower(root.get("name")), "%%" + term + "%%"),
          cb.like(cb.lower(root.get("phone")), "%%" + term + "%%")
      ));
    }
    if (StringUtils.hasText(phone)) {
      String value = phone.trim().toLowerCase();
      specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("phone")), "%%" + value + "%%"));
    }
    if (dateOfBirth != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.get("dateOfBirth"), dateOfBirth));
    }
    return customerRepository.findAll(specification, pageable).map(CustomerResponse::from);
  }

  @Transactional(readOnly = true)
  public CustomerResponse get(UUID id) {
    if (id == null) {
      throw new InvalidCustomerException("Customer id is required");
    }
    return CustomerResponse.from(customerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id)));
  }

  public CustomerResponse update(UUID id, UpdateCustomerCommand command) {
    if (id == null) {
      throw new InvalidCustomerException("Customer id is required");
    }
    if (command == null) {
      throw new InvalidCustomerException("Customer command is required");
    }

    Customer customer = customerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));

    String name = StringUtils.hasText(command.name()) ? command.name().trim() : customer.getName();
    if (!StringUtils.hasText(name)) {
      throw new InvalidCustomerException("Customer name is required");
    }

    applyCommand(
        customer,
        name,
        command.phone() != null ? command.phone().trim() : customer.getPhone(),
        command.address() != null ? command.address().trim() : customer.getAddress(),
        command.dateOfBirth() != null ? command.dateOfBirth() : customer.getDateOfBirth(),
        command.notes() != null ? command.notes().trim() : customer.getNotes()
    );

    return CustomerResponse.from(customerRepository.save(customer));
  }

  private static void validateCreate(CreateCustomerCommand command) {
    if (command == null) {
      throw new InvalidCustomerException("Customer command is required");
    }
    if (!StringUtils.hasText(command.name())) {
      throw new InvalidCustomerException("Customer name is required");
    }
  }

  private static void applyCommand(Customer customer, String name, String phone, String address, LocalDate dateOfBirth, String notes) {
    customer.setName(name.trim());
    customer.setPhone(blankToNull(phone));
    customer.setAddress(blankToNull(address));
    customer.setDateOfBirth(dateOfBirth);
    customer.setNotes(blankToNull(notes));
  }

  private static String blankToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
