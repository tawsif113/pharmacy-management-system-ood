package com.arima.pms.domain.enums;

public enum PurchaseOrderStatus implements DatabaseValue {
    DRAFT("draft"),
    APPROVED("approved"),
    PARTIALLY_RECEIVED("partially_received"),
    RECEIVED("received"),
    CANCELLED("cancelled");

    private final String dbValue;

    PurchaseOrderStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }
}
