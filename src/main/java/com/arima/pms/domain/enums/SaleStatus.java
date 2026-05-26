package com.arima.pms.domain.enums;

public enum SaleStatus implements DatabaseValue {
    DRAFT("draft"),
    CONFIRMED("confirmed"),
    CANCELLED("cancelled"),
    VOIDED("void");

    private final String dbValue;

    SaleStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }
}
