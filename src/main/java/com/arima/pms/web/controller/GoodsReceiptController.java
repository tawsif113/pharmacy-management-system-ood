package com.arima.pms.web.controller;

import com.arima.pms.service.GoodsReceiptService;
import com.arima.pms.service.command.CreateGoodsReceiptCommand;
import com.arima.pms.web.dto.GoodsReceiptResponse;
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
@RequestMapping("/goods-receipts")
public class GoodsReceiptController {

  private final GoodsReceiptService goodsReceiptService;

  @PostMapping
  public ResponseEntity<GoodsReceiptResponse> create(@Valid @RequestBody CreateGoodsReceiptCommand command) {
    return ResponseEntity.status(HttpStatus.CREATED).body(GoodsReceiptResponse.from(goodsReceiptService.createGoodsReceipt(command)));
  }

  @GetMapping
  public Page<GoodsReceiptResponse> list(
      @RequestParam(required = false) UUID purchaseOrderId,
      @RequestParam(required = false) UUID receivedByUserId,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    return goodsReceiptService.list(purchaseOrderId, receivedByUserId, pageable).map(GoodsReceiptResponse::from);
  }

  @GetMapping("/{id}")
  public GoodsReceiptResponse get(@PathVariable UUID id) {
    return GoodsReceiptResponse.from(goodsReceiptService.get(id));
  }
}
