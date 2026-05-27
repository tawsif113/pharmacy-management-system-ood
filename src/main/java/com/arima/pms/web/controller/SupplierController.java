package com.arima.pms.web.controller;

import com.arima.pms.service.SupplierService;
import com.arima.pms.service.command.CreateSupplierCommand;
import com.arima.pms.service.command.UpdateSupplierCommand;
import com.arima.pms.web.dto.SupplierResponse;
import jakarta.validation.Valid;
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
@RequestMapping("/suppliers")
public class SupplierController {

  private final SupplierService supplierService;

  @PostMapping
  public ResponseEntity<SupplierResponse> create(@Valid @RequestBody CreateSupplierCommand command) {
    return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(command));
  }

  @GetMapping
  public Page<SupplierResponse> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Boolean active,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    return supplierService.list(search, active, pageable);
  }

  @GetMapping("/{id}")
  public SupplierResponse get(@PathVariable UUID id) {
    return supplierService.get(id);
  }

  @PatchMapping("/{id}")
  public SupplierResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSupplierCommand command) {
    return supplierService.update(id, command);
  }
}
