package com.arima.pms.domain.enums;

public enum StockMovementType implements DatabaseValue {
    RECEIPT("receipt"),
    SALE("sale"),
    RETURNED("return"),
    ADJUSTMENT("adjustment"),
    WRITE_OFF("write_off"),
    CANCELLED("cancel");

    private final String dbValue;

    StockMovementType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }
}
