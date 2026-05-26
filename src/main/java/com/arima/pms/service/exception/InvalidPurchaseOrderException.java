package com.arima.pms.service.exception;

public class InvalidPurchaseOrderException extends RuntimeException {

  public InvalidPurchaseOrderException(String message) {
    super(message);
  }
}
