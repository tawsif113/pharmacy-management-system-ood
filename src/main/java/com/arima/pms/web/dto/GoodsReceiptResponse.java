package com.arima.pms.web.dto;

import com.arima.pms.domain.entity.Batch;
import com.arima.pms.domain.entity.GoodsReceipt;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GoodsReceiptResponse(
    UUID id,
    UUID purchaseOrderId,
    String purchaseOrderNumber,
    UUID receivedById,
    String receivedByName,
    LocalDateTime receivedAt,
    String notes,
    LocalDateTime createdAt,
    int totalQuantity,
    List<GoodsReceiptBatchResponse> batches
) {
  public static GoodsReceiptResponse from(GoodsReceipt receipt) {
    List<GoodsReceiptBatchResponse> batches = receipt.getBatches().stream()
        .map(GoodsReceiptBatchResponse::from)
        .toList();
    int totalQuantity = receipt.getBatches().stream().mapToInt(Batch::getReceivedQuantity).sum();
    return new GoodsReceiptResponse(
        receipt.getId(),
        receipt.getPurchaseOrder() != null ? receipt.getPurchaseOrder().getId() : null,
        receipt.getPurchaseOrder() != null ? receipt.getPurchaseOrder().getPoNumber() : null,
        receipt.getReceivedBy() != null ? receipt.getReceivedBy().getId() : null,
        receipt.getReceivedBy() != null ? receipt.getReceivedBy().getFullName() : null,
        receipt.getReceivedAt(),
        receipt.getNotes(),
        receipt.getCreatedAt(),
        totalQuantity,
        batches
    );
  }
}
