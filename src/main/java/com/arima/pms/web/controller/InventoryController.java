package com.arima.pms.web.controller;

import com.arima.pms.domain.enums.BatchStatus;
import com.arima.pms.domain.enums.StockMovementType;
import com.arima.pms.service.InventoryService;
import com.arima.pms.service.command.AdjustBatchStockCommand;
import com.arima.pms.service.command.ReturnBatchStockCommand;
import com.arima.pms.service.command.WriteOffBatchStockCommand;
import com.arima.pms.web.dto.BatchResponse;
import com.arima.pms.web.dto.ExpiryAlertResponse;
import com.arima.pms.web.dto.LowStockAlertResponse;
import com.arima.pms.web.dto.StockMovementResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inventory")
public class InventoryController {

  private final InventoryService inventoryService;

  @GetMapping("/batches")
  public Page<BatchResponse> listBatches(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) UUID productId,
      @RequestParam(required = false) UUID supplierId,
      @RequestParam(required = false) BatchStatus status,
      @RequestParam(required = false) LocalDate expiringBefore,
      @RequestParam(required = false) Boolean lowStockOnly,
      Pageable pageable
  ) {
    return inventoryService.listBatches(search, productId, supplierId, status, expiringBefore, lowStockOnly, pageable).map(BatchResponse::from);
  }

  @GetMapping("/batches/{id}")
  public BatchResponse getBatch(@PathVariable UUID id) {
    return BatchResponse.from(inventoryService.getBatch(id));
  }

  @PostMapping("/batches/{id}/adjust")
  public BatchResponse adjust(@PathVariable UUID id, @RequestBody AdjustBatchStockCommand command) {
    return BatchResponse.from(inventoryService.adjustBatchStock(id, command));
  }

  @PostMapping("/batches/{id}/return")
  public BatchResponse returnStock(@PathVariable UUID id, @RequestBody ReturnBatchStockCommand command) {
    return BatchResponse.from(inventoryService.returnBatchStock(id, command));
  }

  @PostMapping("/batches/{id}/write-off")
  public BatchResponse writeOff(@PathVariable UUID id, @RequestBody WriteOffBatchStockCommand command) {
    return BatchResponse.from(inventoryService.writeOffBatchStock(id, command));
  }

  @GetMapping("/stock-movements")
  public Page<StockMovementResponse> listMovements(
      @RequestParam(required = false) UUID productId,
      @RequestParam(required = false) UUID batchId,
      @RequestParam(required = false) StockMovementType type,
      Pageable pageable
  ) {
    return inventoryService.listStockMovements(productId, batchId, type, pageable).map(StockMovementResponse::from);
  }

  @GetMapping("/stock-movements/{id}")
  public StockMovementResponse getMovement(@PathVariable UUID id) {
    return StockMovementResponse.from(inventoryService.getStockMovement(id));
  }

  @GetMapping("/alerts/low-stock")
  public List<LowStockAlertResponse> lowStockAlerts() {
    return inventoryService.lowStockAlerts();
  }

  @GetMapping("/alerts/expiring")
  public List<ExpiryAlertResponse> expiringAlerts(@RequestParam(defaultValue = "30") int days) {
    return inventoryService.expiringAlerts(days);
  }
}
