package com.arima.pms.web.controller;

import com.arima.pms.service.ProductService;
import com.arima.pms.service.command.CreateProductCommand;
import com.arima.pms.service.command.UpdateProductCommand;
import com.arima.pms.web.dto.ProductResponse;
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
@RequestMapping("/products")
public class ProductController {

  private final ProductService productService;

  @PostMapping
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductCommand command) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(command));
  }

  @GetMapping
  public Page<ProductResponse> list(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) String category,
      @RequestParam(required = false, name = "isPrescriptionRequired") Boolean prescriptionRequired,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    return productService.list(query, active, category, prescriptionRequired, pageable);
  }

  @GetMapping("/{id}")
  public ProductResponse get(@PathVariable UUID id) {
    return productService.get(id);
  }

  @PatchMapping("/{id}")
  public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductCommand command) {
    return productService.update(id, command);
  }
}
