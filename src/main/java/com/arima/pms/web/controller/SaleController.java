package com.arima.pms.web.controller;

import com.arima.pms.domain.enums.PaymentStatus;
import com.arima.pms.domain.enums.SaleStatus;
import com.arima.pms.service.SaleService;
import com.arima.pms.service.command.CreateSaleCommand;
import com.arima.pms.service.command.SaleCancelCommand;
import com.arima.pms.service.command.SaleConfirmCommand;
import com.arima.pms.service.command.SaleVoidCommand;
import com.arima.pms.service.command.UpdateSaleCommand;
import com.arima.pms.web.dto.SaleResponse;
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
@RequestMapping("/sales")
public class SaleController {

  private final SaleService saleService;

  @PostMapping
  public ResponseEntity<SaleResponse> create(@Valid @RequestBody CreateSaleCommand command) {
    return ResponseEntity.status(HttpStatus.CREATED).body(SaleResponse.from(saleService.createDraftSale(command)));
  }

  @GetMapping
  public Page<SaleResponse> list(
      @RequestParam(required = false) String invoiceNumber,
      @RequestParam(required = false) UUID customerId,
      @RequestParam(required = false) UUID prescriptionId,
      @RequestParam(required = false) SaleStatus status,
      @RequestParam(required = false) PaymentStatus paymentStatus,
      @RequestParam(required = false) LocalDate dateFrom,
      @RequestParam(required = false) LocalDate dateTo,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    return saleService.list(invoiceNumber, customerId, prescriptionId, status, paymentStatus, dateFrom, dateTo, pageable).map(SaleResponse::from);
  }

  @GetMapping("/{id}")
  public SaleResponse get(@PathVariable UUID id) {
    return SaleResponse.from(saleService.get(id));
  }

  @PatchMapping("/{id}")
  public SaleResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSaleCommand command) {
    return SaleResponse.from(saleService.updateDraftSale(id, command));
  }

  @PostMapping("/{id}/confirm")
  public SaleResponse confirm(@PathVariable UUID id, @Valid @RequestBody SaleConfirmCommand command) {
    return SaleResponse.from(saleService.confirmSale(id, command));
  }

  @PostMapping("/{id}/cancel")
  public SaleResponse cancel(@PathVariable UUID id, @Valid @RequestBody SaleCancelCommand command) {
    return SaleResponse.from(saleService.cancelSale(id, command));
  }

  @PostMapping("/{id}/void")
  public SaleResponse voidSale(@PathVariable UUID id, @Valid @RequestBody SaleVoidCommand command) {
    return SaleResponse.from(saleService.voidSale(id, command));
  }
}
