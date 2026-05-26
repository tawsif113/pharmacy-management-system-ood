package com.arima.pms.domain.enums;

public enum PaymentStatus implements DatabaseValue {
    UNPAID("unpaid"),
    PARTIAL("partial"),
    PAID("paid"),
    REFUNDED("refunded"),
    VOIDED("void");

    private final String dbValue;

    PaymentStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }
}
