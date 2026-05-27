package com.arima.pms.web.controller;

import com.arima.pms.domain.enums.VerificationStatus;
import com.arima.pms.service.PrescriptionService;
import com.arima.pms.service.command.CreatePrescriptionCommand;
import com.arima.pms.service.command.PrescriptionRejectionCommand;
import com.arima.pms.service.command.PrescriptionVerificationCommand;
import com.arima.pms.service.command.UpdatePrescriptionCommand;
import com.arima.pms.web.dto.PrescriptionResponse;
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
@RequestMapping("/prescriptions")
public class PrescriptionController {

  private final PrescriptionService prescriptionService;

  @PostMapping
  public ResponseEntity<PrescriptionResponse> create(@Valid @RequestBody CreatePrescriptionCommand command) {
    return ResponseEntity.status(HttpStatus.CREATED).body(prescriptionService.create(command));
  }

  @GetMapping
  public Page<PrescriptionResponse> list(
      @RequestParam(required = false) UUID customerId,
      @RequestParam(required = false) VerificationStatus verificationStatus,
      @RequestParam(required = false) LocalDate expiryBefore,
      @RequestParam(required = false) LocalDate expiryAfter,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    return prescriptionService.list(customerId, verificationStatus, expiryBefore, expiryAfter, pageable);
  }

  @GetMapping("/{id}")
  public PrescriptionResponse get(@PathVariable UUID id) {
    return prescriptionService.get(id);
  }

  @PatchMapping("/{id}")
  public PrescriptionResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePrescriptionCommand command) {
    return prescriptionService.update(id, command);
  }

  @PostMapping("/{id}/verify")
  public PrescriptionResponse verify(@PathVariable UUID id, @RequestBody(required = false) PrescriptionVerificationCommand command) {
    return prescriptionService.verify(id, command);
  }

  @PostMapping("/{id}/reject")
  public PrescriptionResponse reject(@PathVariable UUID id, @Valid @RequestBody PrescriptionRejectionCommand command) {
    return prescriptionService.reject(id, command);
  }
}
