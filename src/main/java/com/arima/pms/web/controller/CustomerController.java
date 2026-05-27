package com.arima.pms.web.controller;

import com.arima.pms.service.CustomerService;
import com.arima.pms.service.command.CreateCustomerCommand;
import com.arima.pms.service.command.UpdateCustomerCommand;
import com.arima.pms.web.dto.CustomerResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/customers")
public class CustomerController {

  private final CustomerService customerService;

  @PostMapping
  public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerCommand command) {
    return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(command));
  }

  @GetMapping
  public Page<CustomerResponse> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String phone,
      @RequestParam(required = false) LocalDate dateOfBirth,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    return customerService.list(search, phone, dateOfBirth, pageable);
  }

  @GetMapping("/{id}")
  public CustomerResponse get(@PathVariable UUID id) {
    return customerService.get(id);
  }

  @PatchMapping("/{id}")
  public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCustomerCommand command) {
    return customerService.update(id, command);
  }
}
