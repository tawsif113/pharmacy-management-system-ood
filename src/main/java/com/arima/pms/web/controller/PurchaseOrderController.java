package com.arima.pms.web.controller;

import com.arima.pms.domain.enums.PurchaseOrderStatus;
import com.arima.pms.service.PurchaseOrderService;
import com.arima.pms.service.command.CreatePurchaseOrderCommand;
import com.arima.pms.web.dto.PurchaseOrderResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {

  private final PurchaseOrderService purchaseOrderService;

  @PostMapping
  public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody CreatePurchaseOrderCommand command) {
    return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseOrderResponse.from(purchaseOrderService.createDraftPurchaseOrder(command)));
  }

  @GetMapping
  public Page<PurchaseOrderResponse> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) PurchaseOrderStatus status,
      @RequestParam(required = false) UUID supplierId,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    return purchaseOrderService.list(search, status, supplierId, pageable).map(PurchaseOrderResponse::from);
  }

  @GetMapping("/{id}")
  public PurchaseOrderResponse get(@PathVariable UUID id) {
    return PurchaseOrderResponse.from(purchaseOrderService.get(id));
  }

  @PostMapping("/{id}/approve")
  public PurchaseOrderResponse approve(@PathVariable UUID id) {
    return PurchaseOrderResponse.from(purchaseOrderService.approve(id));
  }

  @PostMapping("/{id}/cancel")
  public PurchaseOrderResponse cancel(@PathVariable UUID id) {
    return PurchaseOrderResponse.from(purchaseOrderService.cancel(id));
  }
}
